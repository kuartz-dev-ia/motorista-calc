package com.motorista.calc

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
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

    private val pacotesPermitidos = setOf("com.ubercab.driver", "com.app99.driver")

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Garante que o serviço também consiga ler janelas flutuantes (overlays),
        // não só a janela que está em foco. É isso que faltava pra pegar a tela
        // de "nova corrida" do Uber, que aparece como um popup flutuante por cima.
        serviceInfo = serviceInfo?.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        Log.d(TAG, "Serviço de acessibilidade conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // Varre TODAS as janelas visíveis na tela (não só a que está em foco),
        // porque a tela de nova corrida do Uber/99 aparece como popup flutuante
        // por cima de outros apps.
        val janelasVisiveis = windows ?: return

        for (janela in janelasVisiveis) {
            val root = janela.root ?: continue
            val pacoteDaJanela = root.packageName?.toString()
            if (pacoteDaJanela !in pacotesPermitidos) continue

            val textoTela = StringBuilder()
            coletarTexto(root, textoTela)
            val texto = textoTela.toString()

            if (texto.isBlank() || texto == ultimoTextoProcessado) continue
            ultimoTextoProcessado = texto

            registrarDebug(texto)

            if (TriggerPatterns.pareceTelaDeCorrida(texto)) {
                Log.d(TAG, "Tela de corrida detectada. Processando...")
                processarTelaDeCorrida(texto)
            }
        }
    }

    /** Mantém um histórico das últimas capturas (mais recente primeiro), pra debug. */
    private fun registrarDebug(texto: String) {
        val prefsDebug = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val hora = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val entradaNova = "=== $hora ===\n$texto\n\n"
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
