package com.motorista.calc

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: android.view.View? = null
    private var botaoFechar: android.view.View? = null

    private val handler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        instanciaAtual = this
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY

        val valorKmCalc = intent.getDoubleOrNull(EXTRA_VALOR_KM_CALC)
        val valorKmExibido = intent.getDoubleOrNull(EXTRA_VALOR_KM_EXIBIDO)
        val valorHoraEfetivo = intent.getDoubleOrNull(EXTRA_VALOR_HORA_EFETIVO)
        val lucro = intent.getDoubleOrNull(EXTRA_LUCRO)
        val surge = intent.getDoubleOrNull(EXTRA_SURGE)
        val avaliacao = intent.getDoubleOrNull(EXTRA_AVALIACAO)
        val valeAPena = intent.getBooleanExtra(EXTRA_VALE_A_PENA, true)
        val motivo = intent.getStringExtra(EXTRA_MOTIVO) ?: ""

        mostrarOverlay(valorKmCalc, valorKmExibido, valorHoraEfetivo, lucro, surge, avaliacao, valeAPena, motivo)
        return START_NOT_STICKY
    }

    // Só lê o extra se ele de fato existir com um valor real — evita o bug de
    // "0.0 fantasma" quando o valor original era nulo.
    private fun Intent.getDoubleOrNull(key: String): Double? =
        if (hasExtra(key)) getDoubleExtra(key, Double.NaN).takeIf { !it.isNaN() } else null

    private fun mostrarOverlay(
        valorKmCalc: Double?,
        valorKmExibido: Double?,
        valorHoraEfetivo: Double?,
        lucro: Double?,
        surge: Double?,
        avaliacao: Double?,
        valeAPena: Boolean,
        motivo: String
    ) {
        removerOverlayExistente()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val corFundo = if (valeAPena) Color.parseColor("#E62E7D32") else Color.parseColor("#E6C62828")

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(corFundo)
            setPadding(28, 20, 28, 20)
        }

        val linhaTopo = TextView(this).apply {
            text = buildString {
                append(if (valeAPena) "✅ VALE A PENA" else "⚠️ NÃO COMPENSA")
                // Só mostra o raio quando realmente existe tarifa dinâmica (surge > 1.0).
                if (surge != null && surge > 1.0) append("  ⚡%.1fx".format(surge))
            }
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(0, 0, 0, 12)
        }
        container.addView(linhaTopo)

        val linhaMetricas = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        linhaMetricas.addView(criarColuna("R$/KM", valorKmCalc?.let { "%.2f".format(it) } ?: "--", minimoKm = MIN_KM_REF, valorReal = valorKmCalc))
        linhaMetricas.addView(criarColuna("R$/H", valorHoraEfetivo?.let { "%.0f".format(it) } ?: "--", minimoKm = MIN_HORA_REF, valorReal = valorHoraEfetivo))
        avaliacao?.let {
            linhaMetricas.addView(criarColuna("NOTA", "%.2f".format(it), minimoKm = 4.5, valorReal = it))
        }
        container.addView(linhaMetricas)

        val linhaDetalhe = TextView(this).apply {
            text = buildString {
                lucro?.let { append("Lucro líq. est.: R$ %.2f  ".format(it)) }
                append(motivo)
            }
            setTextColor(Color.parseColor("#DDFFFFFF"))
            textSize = 12f
            setPadding(0, 12, 0, 0)
        }
        container.addView(linhaDetalhe)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        // Card principal: continua "furado" pra toque (não bloqueia o Aceitar por baixo).
        val paramsCard = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100
        }

        overlayView = container
        windowManager?.addView(overlayView, paramsCard)

        // Botão de fechar: janela SEPARADA, pequena, e essa SIM recebe toque
        // (sem FLAG_NOT_TOUCHABLE), no canto superior direito da tela.
        val fechar = TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(20, 10, 20, 10)
            setOnClickListener { removerOverlayExistente() }
        }

        val paramsFechar = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 90
        }

        botaoFechar = fechar
        windowManager?.addView(botaoFechar, paramsFechar)

        dismissRunnable = Runnable { removerOverlayExistente() }
        handler.postDelayed(dismissRunnable!!, 9000)
    }

    private fun criarColuna(rotulo: String, valorTexto: String, minimoKm: Double, valorReal: Double?): LinearLayout {
        val coluna = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 0, 24, 0)
        }

        val bolinha = android.view.View(this).apply {
            val cor = when {
                valorReal == null -> Color.GRAY
                valorReal >= minimoKm -> Color.parseColor("#4CAF50")
                else -> Color.parseColor("#F44336")
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(cor)
            }
            layoutParams = LinearLayout.LayoutParams(36, 36).apply { gravity = Gravity.CENTER_HORIZONTAL }
        }

        val txtValor = TextView(this).apply {
            text = valorTexto
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val txtRotulo = TextView(this).apply {
            text = rotulo
            setTextColor(Color.parseColor("#CCFFFFFF"))
            textSize = 10f
            gravity = Gravity.CENTER_HORIZONTAL
            setSingleLine(true)
        }

        coluna.addView(txtRotulo)
        coluna.addView(bolinha)
        coluna.addView(txtValor)
        return coluna
    }

    private fun removerOverlayExistente() {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        dismissRunnable = null

        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // já removido, ignora
            }
        }
        overlayView = null

        botaoFechar?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // já removido, ignora
            }
        }
        botaoFechar = null
    }

    override fun onDestroy() {
        super.onDestroy()
        removerOverlayExistente()
        if (instanciaAtual === this) instanciaAtual = null
    }

    companion object {
        const val EXTRA_VALOR_KM_CALC = "extra_valor_km_calc"
        const val EXTRA_VALOR_KM_EXIBIDO = "extra_valor_km_exibido"
        const val EXTRA_VALOR_HORA_EFETIVO = "extra_valor_hora_efetivo"
        const val EXTRA_LUCRO = "extra_lucro"
        const val EXTRA_SURGE = "extra_surge"
        const val EXTRA_AVALIACAO = "extra_avaliacao"
        const val EXTRA_VALE_A_PENA = "extra_vale_a_pena"
        const val EXTRA_MOTIVO = "extra_motivo"

        private const val MIN_KM_REF = 1.50
        private const val MIN_HORA_REF = 25.0

        @Volatile
        private var instanciaAtual: OverlayService? = null

        fun ocultarTemporariamente() {
            try {
                instanciaAtual?.overlayView?.visibility = android.view.View.INVISIBLE
                instanciaAtual?.botaoFechar?.visibility = android.view.View.INVISIBLE
            } catch (e: Exception) {
                // ignora
            }
        }

        fun restaurarVisibilidade() {
            try {
                instanciaAtual?.overlayView?.visibility = android.view.View.VISIBLE
                instanciaAtual?.botaoFechar?.visibility = android.view.View.VISIBLE
            } catch (e: Exception) {
                // ignora
            }
        }
    }
}
