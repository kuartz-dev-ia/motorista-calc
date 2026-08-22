package com.motorista.calc

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors

class RideAccessibilityService : AccessibilityService() {

    private val executor = Executors.newSingleThreadExecutor()
    private var ultimoTextoProcessado: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Serviço de acessibilidade conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val rootNode = rootInActiveWindow ?: return

        val textoTela = StringBuilder()
        coletarTexto(rootNode, textoTela)
        val texto = textoTela.toString()

        if (texto == ultimoTextoProcessado || texto.isBlank()) return
        ultimoTextoProcessado = texto

        if (!TriggerPatterns.pareceTelaDeCorrida(texto)) return

        Log.d(TAG, "Tela de corrida detectada. Processando...")
        processarTelaDeCorrida(texto)
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
        val engine = CalculationEngine(
            precoCombustivelPorLitro = prefs.getFloat(PREF_PRECO_COMBUSTIVEL, 6.10f).toDouble(),
            consumoKmPorLitro = prefs.getFloat(PREF_CONSUMO, 12.0f).toDouble(),
            minimoValorPorKm = prefs.getFloat(PREF_MIN_KM, 1.50f).toDouble(),
            minimoValorPorHora = prefs.getFloat(PREF_MIN_HORA, 25.0f).toDouble()
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

    companion object {
        private const val TAG = "RideAccessibility"
        const val PREFS_NAME = "motorista_calc_prefs"
        const val PREF_PRECO_COMBUSTIVEL = "preco_combustivel"
        const val PREF_CONSUMO = "consumo_km_litro"
        const val PREF_MIN_KM = "minimo_valor_km"
        const val PREF_MIN_HORA = "minimo_valor_hora"
        const val PREF_SALVAR_PRINT = "salvar_print"
    }
}
