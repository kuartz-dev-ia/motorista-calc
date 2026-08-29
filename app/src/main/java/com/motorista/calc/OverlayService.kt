package com.motorista.calc

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
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
        val nivel = NivelCorrida.values().getOrElse(intent.getIntExtra(EXTRA_NIVEL, 0)) { NivelCorrida.RUIM }

        mostrarOverlay(registroId, valorKmCalc, valorHoraEfetivo, valorMinutoEfetivo, lucro, nivel)
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
        nivel: NivelCorrida
    ) {
        limparViewsSemCallback()
        registroIdAtual = registroId
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val (corFundo, corTitulo, corValor, textoTitulo) = when (nivel) {
            NivelCorrida.RUIM -> arrayOf(Color.parseColor("#FF791F1F"), Color.parseColor("#FFF7C1C1"), Color.parseColor("#FFFCEBEB"), "NÃO COMPENSA")
            NivelCorrida.MEDIO -> arrayOf(Color.parseColor("#FF854F0B"), Color.parseColor("#FFFAC775"), Color.parseColor("#FFFAEEDA"), "VOCÊ DECIDE")
            NivelCorrida.BOM -> arrayOf(Color.parseColor("#FF173404"), Color.parseColor("#FFC0DD97"), Color.parseColor("#FFEAF3DE"), "VALE A PENA")
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(corFundo as Int)
            setPadding(dp(20), dp(10), dp(20), dp(10))
        }

        val linhaTopo = TextView(this).apply {
            text = textoTitulo as String
            setTextColor(corTitulo as Int)
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dp(6))
        }
        container.addView(linhaTopo)

        val linhaMetricas = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        linhaMetricas.addView(criarColuna("R$/KM", valorKmCalc?.let { "%.2f".format(it) } ?: "--", corTitulo as Int, corValor as Int))
        linhaMetricas.addView(criarColuna("R$/HORA", valorHoraEfetivo?.let { "%.0f".format(it) } ?: "--", corTitulo, corValor))
        linhaMetricas.addView(criarColuna("R$/MIN", valorMinutoEfetivo?.let { "%.2f".format(it) } ?: "--", corTitulo, corValor))
        container.addView(linhaMetricas)

        val linhaLucro = TextView(this).apply {
            text = lucro?.let { "Lucro líquido: R$ %.2f".format(it) } ?: ""
            setTextColor(corTitulo)
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(6), 0, 0)
        }
        container.addView(linhaLucro)

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

        val barra = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

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
            y = dp(50)
        }

        barraAcoes = barra
        windowManager?.addView(barraAcoes, paramsBarra)

        dismissRunnable = Runnable { fecharCard() }
        handler.postDelayed(dismissRunnable!!, DURACAO_EXIBICAO_MS)
    }

    private fun criarColuna(rotulo: String, valorTexto: String, corRotulo: Int, corValor: Int): LinearLayout {
        val coluna = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(12), 0, dp(12), 0)
        }

        val txtValor = TextView(this).apply {
            text = valorTexto
            setTextColor(corValor)
            textSize = 21f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val txtRotulo = TextView(this).apply {
            text = rotulo
            setTextColor(corRotulo)
            textSize = 11f
            gravity = Gravity.CENTER_HORIZONTAL
            maxLines = 1
        }

        coluna.addView(txtRotulo)
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
        const val EXTRA_VALOR_HORA_EFETIVO = "extra_valor_hora_efetivo"
        const val EXTRA_VALOR_MINUTO_EFETIVO = "extra_valor_minuto_efetivo"
        const val EXTRA_LUCRO = "extra_lucro"
        const val EXTRA_NIVEL = "extra_nivel"

        private const val DURACAO_EXIBICAO_MS = 30000L

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
