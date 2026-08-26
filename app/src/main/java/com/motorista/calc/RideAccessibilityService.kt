package com.motorista.calc

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

class RideAccessibilityService : AccessibilityService() {

    private val executor = Executors.newSingleThreadExecutor()

    // Inicialização "preguiçosa" e protegida: só cria o cliente de OCR quando for
    // usado de fato, e nunca deixa uma falha aqui derrubar o serviço inteiro.
    private val recognizer by lazy {
        try {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao criar cliente de OCR: ${e.message}")
            null
        }
    }

    private var ultimoTextoProcessado: String = ""
    private var capturandoNoMomento = false

    private val handler = Handler(Looper.getMainLooper())
    private val pollingIntervalMs = 1500L
    private val pollRunnable = object : Runnable {
        override fun run() {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    tentarCapturarEOcr()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro no ciclo de captura: ${e.message}")
                capturandoNoMomento = false
            } finally {
                handler.postDelayed(this, pollingIntervalMs)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Serviço de acessibilidade conectado (modo print + OCR)")
        try {
            handler.post(pollRunnable)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao iniciar polling: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // O reconhecimento roda via print + OCR em polling (função abaixo), não
        // depende deste evento. Método mantido só porque a classe exige.
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun tentarCapturarEOcr() {
        if (capturandoNoMomento) return
        val ocr = recognizer ?: return
        capturandoNoMomento = true

        try {
            takeScreenshot(
                android.view.Display.DEFAULT_DISPLAY,
                executor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(result: ScreenshotResult) {
                        try {
                            val bitmapHardware = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                            val bitmap = bitmapHardware?.copy(Bitmap.Config.ARGB_8888, false)
                            result.hardwareBuffer.close()

                            if (bitmap == null) {
                                capturandoNoMomento = false
                                return
                            }

                            val inputImage = InputImage.fromBitmap(bitmap, 0)
                            ocr.process(inputImage)
                                .addOnSuccessListener { visionText ->
                                    try {
                                        processarTextoOcr(visionText.text)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Erro processando texto do OCR: ${e.message}")
                                    } finally {
                                        capturandoNoMomento = false
                                    }
                                }
                                .addOnFailureListener { erro ->
                                    Log.e(TAG, "Falha no OCR: ${erro.message}")
                                    capturandoNoMomento = false
                                }
                        } catch (e: Exception) {
                            Log.e(TAG, "Erro processando print: ${e.message}")
                            capturandoNoMomento = false
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        // Código comum: pedimos print rápido demais (limite do sistema).
                        // Não é grave, só espera o próximo ciclo.
                        capturandoNoMomento = false
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao pedir print: ${e.message}")
            capturandoNoMomento = false
        }
    }

    private fun processarTextoOcr(texto: String) {
        if (texto.isBlank() || texto == ultimoTextoProcessado) return
        ultimoTextoProcessado = texto

        if (texto.contains("R$")) {
            registrarDebug(texto)
        }

        if (!TriggerPatterns.pareceTelaDeCorrida(texto)) return

        Log.d(TAG, "Tela de corrida detectada via OCR. Processando...")
        processarTelaDeCorrida(texto)
    }

    private fun registrarDebug(texto: String) {
        try {
            val prefsDebug = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val hora = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            val entradaNova = "=== $hora (OCR) ===\n$texto\n\n"
            val logAntigo = prefsDebug.getString(PREF_ULTIMO_TEXTO, "") ?: ""
            val novoLog = (entradaNova + logAntigo).take(8000)
            prefsDebug.edit().putString(PREF_ULTIMO_TEXTO, novoLog).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Erro salvando debug: ${e.message}")
        }
    }

    private fun processarTelaDeCorrida(texto: String) {
        val pernas = TriggerPatterns.extrairPernas(texto)
        val pernaPickup = pernas.firstOrNull()
        val pernaCorrida = pernas.lastOrNull()

        val ride = RideInfo(
            valorTotal = TriggerPatterns.extrairValorTotal(texto),
            valorPorKmExibido = TriggerPatterns.extrairValorPorKmExibido(texto),
            surgeMultiplicador = TriggerPatterns.extrairSurge(texto),
            avaliacaoPassageiro = TriggerPatterns.extrairAvaliacao(texto),
            viagemLonga = TriggerPatterns.ehViagemLonga(texto),
            verificado = TriggerPatterns.ehVerificado(texto),
            tempoPickupMin = pernaPickup?.tempoMin,
            distanciaPickupKm = pernaPickup?.distanciaKm,
            tempoCorridaMin = if (pernas.size >= 2) pernaCorrida?.tempoMin else pernas.firstOrNull()?.tempoMin,
            distanciaCorridaKm = if (pernas.size >= 2) pernaCorrida?.distanciaKm else pernas.firstOrNull()?.distanciaKm
        )

        if (ride.valorTotal == null) {
            Log.d(TAG, "Tela parecia corrida mas não achei valor em R$. Texto: $texto")
            return
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val financiamento = prefs.getFloat(PREF_FINANCIAMENTO, 0f).toDouble()
        val seguro = prefs.getFloat(PREF_SEGURO, 0f).toDouble()
        val ipvaAnual = prefs.getFloat(PREF_IPVA, 0f).toDouble()
        val licenciamentoAnual = prefs.getFloat(PREF_LICENCIAMENTO, 0f).toDouble()
        val manutencao = prefs.getFloat(PREF_MANUTENCAO, 0f).toDouble()
        val contasPessoais = prefs.getFloat(PREF_CONTAS_PESSOAIS, 0f).toDouble()
        val kmMes = prefs.getFloat(PREF_KM_MES, 3000f).toDouble()

        val custoFixoMensal = financiamento + seguro + (ipvaAnual / 12.0) +
            (licenciamentoAnual / 12.0) + manutencao + contasPessoais
        val custoFixoPorKm = if (kmMes > 0) custoFixoMensal / kmMes else 0.0

        val engine = CalculationEngine(
            precoCombustivelPorLitro = prefs.getFloat(PREF_PRECO_COMBUSTIVEL, 6.10f).toDouble(),
            consumoKmPorLitro = prefs.getFloat(PREF_CONSUMO, 12.0f).toDouble(),
            minimoValorPorKm = prefs.getFloat(PREF_MIN_KM, 1.50f).toDouble(),
            minimoValorPorHora = prefs.getFloat(PREF_MIN_HORA, 25.0f).toDouble(),
            custoFixoPorKm = custoFixoPorKm
        )
        val resultado = engine.calcular(ride)

        Log.d(TAG, "Ride: $ride")
        Log.d(TAG, "Resultado: $resultado")

        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_VALOR_KM_CALC, resultado.valorPorKmCalculado)
            putExtra(OverlayService.EXTRA_VALOR_KM_EXIBIDO, resultado.valorPorKmExibido)
            putExtra(OverlayService.EXTRA_VALOR_HORA_EFETIVO, resultado.valorPorHoraEfetivo)
            putExtra(OverlayService.EXTRA_LUCRO, resultado.lucroLiquidoEstimado)
            putExtra(OverlayService.EXTRA_SURGE, ride.surgeMultiplicador)
            putExtra(OverlayService.EXTRA_AVALIACAO, ride.avaliacaoPassageiro)
            putExtra(OverlayService.EXTRA_VALE_A_PENA, resultado.valeAPena)
            putExtra(OverlayService.EXTRA_MOTIVO, resultado.motivo)
        }
        startService(intent)
    }

    override fun onInterrupt() {
        Log.w(TAG, "Serviço de acessibilidade interrompido")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
    }

    companion object {
        private const val TAG = "RideAccessibility"
        const val PREFS_NAME = "motorista_calc_prefs"
        const val PREF_PRECO_COMBUSTIVEL = "preco_combustivel"
        const val PREF_CONSUMO = "consumo_km_litro"
        const val PREF_MIN_KM = "minimo_valor_km"
        const val PREF_MIN_HORA = "minimo_valor_hora"
        const val PREF_SALVAR_PRINT = "salvar_print"
        const val PREF_ULTIMO_TEXTO = "ultimo_texto_capturado"
        const val PREF_FINANCIAMENTO = "financiamento_mensal"
        const val PREF_SEGURO = "seguro_mensal"
        const val PREF_IPVA = "ipva_anual"
        const val PREF_LICENCIAMENTO = "licenciamento_anual"
        const val PREF_MANUTENCAO = "manutencao_mensal"
        const val PREF_CONTAS_PESSOAIS = "contas_pessoais_mensal"
        const val PREF_KM_MES = "km_rodados_mes"
    }
}
