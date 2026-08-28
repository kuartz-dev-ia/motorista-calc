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
    private var barraAcoes: android.view.View? = null
    private var registroIdAtual: Long? = null

    private val handler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        instanciaAtual = this
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY

        val registroId = if (intent.hasExtra(EXTRA_REGISTRO_ID)) intent.getLongExtra(EXTRA_REGISTRO_ID, -1L) else null
        val valorKmCalc = intent.getDoubleOrNull(EXTRA_VALOR_KM_CALC)
        val valorHoraEfetivo = intent.getDoubleOrNull(EXTRA_VALOR_HORA_EFETIVO)
        val valorMinutoEfetivo = intent.getDoubleOrNull(EXTRA_VALOR_MINUTO_EFETIVO)
        val lucro = intent.getDoubleOrNull(EXTRA_LUCRO)
        val surge = intent.getDoubleOrNull(EXTRA_SURGE)
        val avaliacao = intent.getDoubleOrNull(EXTRA_AVALIACAO)
        val valeAPena = intent.getBooleanExtra(EXTRA_VALE_A_PENA, true)
        val motivo = intent.getStringExtra(EXTRA_MOTIVO) ?: ""

        mostrarOverlay(registroId, valorKmCalc, valorHoraEfetivo, valorMinutoEfetivo, lucro, surge, avaliacao, valeAPena, motivo)
        return START_NOT_STICKY
    }

    private fun Intent.getDoubleOrNull(key: String): Double? =
        if (hasExtra(key)) getDoubleExtra(key, Double.NaN).takeIf { !it.isNaN() } else null

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()

    private fun mostrarOverlay(
        registroId: Long?,
        valorKmCalc: Double?,
        valorHoraEfetivo: Double?,
        valorMinutoEfetivo: Double?,
        lucro: Double?,
        surge: Double?,
        avaliacao: Double?,
        valeAPena: Boolean,
        motivo: String
    ) {
        limparViewsSemCallback()
        registroIdAtual = registroId
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val corFundo = if (valeAPena) Color.parseColor("#FF1B5E20") else Color.parseColor("#FF8B1A1A")

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(corFundo)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        val linhaTopo = TextView(this).apply {
            text = buildString {
                append(if (valeAPena) "✅ VALE A PENA" else "⚠️ NÃO COMPENSA")
                if (surge != null && surge > 1.0) append("  ⚡%.1fx".format(surge))
            }
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(0, 0, 0, dp(8))
        }
        container.addView(linhaTopo)

        val linhaMetricas = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        linhaMetricas.addView(criarColuna("R$/KM", valorKmCalc?.let { "%.2f".format(it) } ?: "--", minimoRef = MIN_KM_REF, valorReal = valorKmCalc))
        linhaMetricas.addView(criarColuna("R$/HORA", valorHoraEfetivo?.let { "%.0f".format(it) } ?: "--", minimoRef = MIN_HORA_REF, valorReal = valorHoraEfetivo))
        linhaMetricas.addView(criarColuna("R$/MIN", valorMinutoEfetivo?.let { "%.2f".format(it) } ?: "--", minimoRef = MIN_MINUTO_REF, valorReal = valorMinutoEfetivo))
        container.addView(linhaMetricas)

        val linhaDetalhe = TextView(this).apply {
            text = buildString {
                lucro?.let { append("Lucro líq. est.: R$ %.2f".format(it)) }
                avaliacao?.let { append("  •  Nota passageiro: %.2f".format(it)) }
                if (motivo.isNotBlank()) {
                    if (isNotEmpty()) append("\n")
                    append(motivo)
                }
            }
            setTextColor(Color.parseColor("#DDFFFFFF"))
            textSize = 12f
            setPadding(0, dp(8), 0, 0)
        }
        container.addView(linhaDetalhe)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

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
            y = dp(60)
        }

        overlayView = container
        windowManager?.addView(overlayView, paramsCard)

        // Barra com dois botões: "✔ Aceitei" (marca a corrida no histórico) e
        // "✕" (só fecha o card). Janela separada, recebe toque normalmente.
        val barra = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val btnAceitei = TextView(this).apply {
            text = "✔ Aceitei"
            setTextColor(Color.WHITE)
            textSize = 12f
            setBackgroundColor(Color.parseColor("#CC1B5E20"))
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener {
                registroIdAtual?.let { id -> HistoricoStorage.marcarAceita(this@OverlayService, id) }
                fecharCard()
            }
        }

        val espaco = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(6), 1)
        }

        val btnFechar = TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener { fecharCard() }
        }

        barra.addView(btnAceitei)
        barra.addView(espaco)
        barra.addView(btnFechar)

        val paramsBarra = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(8)
            y = dp(54)
        }

        barraAcoes = barra
        windowManager?.addView(barraAcoes, paramsBarra)

        dismissRunnable = Runnable { fecharCard() }
        handler.postDelayed(dismissRunnable!!, DURACAO_EXIBICAO_MS)
    }

    private fun criarColuna(rotulo: String, valorTexto: String, minimoRef: Double, valorReal: Double?): LinearLayout {
        val coluna = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, dp(10), 0)
            minimumWidth = dp(78)
        }

        val bolinha = android.view.View(this).apply {
            val cor = when {
                valorReal == null -> Color.GRAY
                valorReal >= minimoRef -> Color.parseColor("#4CAF50")
                else -> Color.parseColor("#F44336")
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(cor)
            }
            layoutParams = LinearLayout.LayoutParams(dp(12), dp(12)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(2)
                bottomMargin = dp(2)
            }
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
            textSize = 11f
            gravity = Gravity.CENTER_HORIZONTAL
            maxLines = 1
        }

        coluna.addView(txtRotulo)
        coluna.addView(bolinha)
        coluna.addView(txtValor)
        return coluna
    }

    private fun limparViewsSemCallback() {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        dismissRunnable = null

        overlayView?.let {
            try { windowManager?.removeView(it) } catch (e: Exception) { }
        }
        overlayView = null

        barraAcoes?.let {
            try { windowManager?.removeView(it) } catch (e: Exception) { }
        }
        barraAcoes = null
    }

    private fun fecharCard() {
        limparViewsSemCallback()
        registroIdAtual = null
        aoFechar?.invoke()
        aoFechar = null
    }

    override fun onDestroy() {
        super.onDestroy()
        limparViewsSemCallback()
        if (instanciaAtual === this) instanciaAtual = null
    }

    companion object {
        const val EXTRA_REGISTRO_ID = "extra_registro_id"
        const val EXTRA_VALOR_KM_CALC = "extra_valor_km_calc"
        const val EXTRA_VALOR_KM_EXIBIDO = "extra_valor_km_exibido"
        const val EXTRA_VALOR_HORA_EFETIVO = "extra_valor_hora_efetivo"
        const val EXTRA_VALOR_MINUTO_EFETIVO = "extra_valor_minuto_efetivo"
        const val EXTRA_LUCRO = "extra_lucro"
        const val EXTRA_SURGE = "extra_surge"
        const val EXTRA_AVALIACAO = "extra_avaliacao"
        const val EXTRA_VALE_A_PENA = "extra_vale_a_pena"
        const val EXTRA_MOTIVO = "extra_motivo"

        private const val MIN_KM_REF = 1.50
        private const val MIN_HORA_REF = 25.0
        private const val MIN_MINUTO_REF = 0.42

        private const val DURACAO_EXIBICAO_MS = 5000L

        @Volatile
        private var instanciaAtual: OverlayService? = null

        @Volatile
        var aoFechar: (() -> Unit)? = null

        fun ocultarTemporariamente() {
            try {
                instanciaAtual?.overlayView?.visibility = android.view.View.INVISIBLE
                instanciaAtual?.barraAcoes?.visibility = android.view.View.INVISIBLE
            } catch (e: Exception) { }
        }

        fun restaurarVisibilidade() {
            try {
                instanciaAtual?.overlayView?.visibility = android.view.View.VISIBLE
                instanciaAtual?.barraAcoes?.visibility = android.view.View.VISIBLE
            } catch (e: Exception) { }
        }
    }
}
