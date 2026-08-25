package com.motorista.calc

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors

class RideAccessibilityService : AccessibilityService() {

    private val executor = Executors.newSingleThreadExecutor()
    private var ultimoTextoProcessado: String = ""
    private var ultimoResumo: String = ""

    private val pacotesDoApp = setOf("com.ubercab.driver", "com.app99.driver")

    private val handler = Handler(Looper.getMainLooper())
    private val pollingIntervalMs = 500L
    private val pollRunnable = object : Runnable {
        override fun run() {
            varrerJanelas()
            handler.postDelayed(this, pollingIntervalMs)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo?.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        Log.d(TAG, "Serviço de acessibilidade conectado (modo diagnóstico + polling)")
        handler.post(pollRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        varrerJanelas()
    }

    private fun tipoDeJanela(tipo: Int): String = when (tipo) {
        AccessibilityWindowInfo.TYPE_APPLICATION -> "APP"
        AccessibilityWindowInfo.TYPE_INPUT_METHOD -> "TECLADO"
        AccessibilityWindowInfo.TYPE_SYSTEM -> "SISTEMA"
        AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> "OVERLAY_ACESSIBILIDADE"
        AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER -> "DIVISOR"
        AccessibilityWindowInfo.TYPE_MAGNIFICATION_OVERLAY -> "MAGNIFICADOR"
        else -> "TIPO_$tipo"
    }

    private fun varrerJanelas() {
        val janelasVisiveis = windows ?: return

        // RESUMO: lista TODAS as janelas visíveis com tipo, pacote e quantidade de
        // caracteres de texto (mesmo zero), pra diagnosticar se o popup aparece
        // como janela vazia ou nem aparece na lista.
        val resumoPartes = mutableListOf<String>()

        for (janela in janelasVisiveis) {
            val root = janela.root
            val pacoteDaJanela = root?.packageName?.toString() ?: "sem-root"
            val tipo = tipoDeJanela(janela.type)

            val textoTela = StringBuilder()
            if (root != null) coletarTexto(root, textoTela)
            val texto = textoTela.toString()

            resumoPartes.add("$tipo/$pacoteDaJanela(${texto.length}c)")

            if (texto.isBlank()) continue

            val chave = "$pacoteDaJanela::$texto"
            if (chave != ultimoTextoProcessado) {
                ultimoTextoProcessado = chave
                if (texto.contains("R$")) {
                    registrarDebug(pacoteDaJanela, texto)
                }
                if (pacoteDaJanela in pacotesDoApp && TriggerPatterns.pareceTelaDeCorrida(texto)) {
                    Log.d(TAG, "Tela de corrida detectada. Processando...")
                    processarTelaDeCorrida(texto)
                }
            }
        }

        val resumo = resumoPartes.joinToString(" | ")
        if (resumo != ultimoResumo) {
            ultimoResumo = resumo
            registrarResumo(resumo)
        }
    }

    private fun registrarResumo(resumo: String) {
        val prefsDebug = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val hora = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val entradaNova = "--- $hora [RESUMO JANELAS] ---\n$resumo\n\n"
        val logAntigo = prefsDebug.getString(PREF_RESUMO_JANELAS, "") ?: ""
        val novoLog = (entradaNova + logAntigo).take(6000)
        prefsDebug.edit().putString(PREF_RESUMO_JANELAS, novoLog).apply()
    }

    /** Mantém um histórico das últimas capturas com texto (mais recente primeiro), com o pacote de origem, pra debug. */
    private fun registrarDebug(pacote: String, texto: String) {
        val prefsDebug = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val hora = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val entradaNova = "=== $hora [$pacote] ===\n$texto\n\n"
        val logAntigo = prefsDebug.getString(PREF_ULTIMO_TEXTO, "") ?: ""
        val novoLog = (entradaNova + logAntigo).take(8000)
        prefsDebug.edit().putString(PREF_ULTIMO_TEXTO, novoLog).apply()
    }

    private fun coletarTexto(node: AccessibilityNodeInfo?, out: StringBuilder) {
        node ?: return
        node.text?.let { if (it.isNotBlank()) out.append(it).append(" | ") }
        node.contentDescription?.let { if (it.isNotBlank()) out.append(it).append(" | ") }
        for (i in 0 until node.childCount) {
            coletarTexto(node.getChild(i), out)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && prefs.getBoolean(PREF_SALVAR_PRINT, false)) {
            capturarPrint()
        }

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

    @RequiresApi(Build.VERSION_CODES.R)
    private fun capturarPrint() {
        takeScreenshot(
            android.view.Display.DEFAULT_DISPLAY,
            executor,
            object : TakeScreenshotCallback {
                override fun onSuccess(result: ScreenshotResult) {
                    Log.d(TAG, "Print capturado com sucesso")
                }

                override fun onFailure(errorCode: Int) {
                    Log.e(TAG, "Falha ao capturar print: $errorCode")
                }
            }
        )
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
        const val PREF_RESUMO_JANELAS = "resumo_janelas"
        const val PREF_FINANCIAMENTO = "financiamento_mensal"
        const val PREF_SEGURO = "seguro_mensal"
        const val PREF_IPVA = "ipva_anual"
        const val PREF_LICENCIAMENTO = "licenciamento_anual"
        const val PREF_MANUTENCAO = "manutencao_mensal"
        const val PREF_CONTAS_PESSOAIS = "contas_pessoais_mensal"
        const val PREF_KM_MES = "km_rodados_mes"
    }
}
