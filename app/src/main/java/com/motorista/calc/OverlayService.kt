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
        val percentualLucro = intent.getDoubleOrNull(EXTRA_PERCENTUAL_LUCRO)
        val nivel = NivelCorrida.values().getOrElse(intent.getIntExtra(EXTRA_NIVEL, 0)) { NivelCorrida.RUIM }

        mostrarOverlay(registroId, valorKmCalc, valorHoraEfetivo, valorMinutoEfetivo, lucro, percentualLucro, nivel)
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
        percentualLucro: Double?,
        nivel: NivelCorrida
    ) {
        limparViewsSemCallback()
        registroIdAtual = registroId
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        data class Paleta(val fundoTopo: Int, val fundoBase: Int, val borda: Int, val titulo: Int, val rotulo: Int, val valor: Int, val pillFundo: Int, val texto: String)

        val paleta = when (nivel) {
            NivelCorrida.RUIM -> Paleta(Color.parseColor("#4A1414"), Color.parseColor("#2A0A0A"), Color.parseColor("#C24A4A"), Color.parseColor("#F7C1C1"), Color.parseColor("#D98787"), Color.parseColor("#FCEBEB"), Color.parseColor("#5A1E1E"), "NÃO COMPENSA")
            NivelCorrida.MEDIO -> Paleta(Color.parseColor("#4A340A"), Color.parseColor("#2A1D03"), Color.parseColor("#D69A3F"), Color.parseColor("#FAC775"), Color.parseColor("#C99A55"), Color.parseColor("#FAEEDA"), Color.parseColor("#5A3E12"), "VOCÊ DECIDE")
            NivelCorrida.BOM -> Paleta(Color.parseColor("#1E4A0A"), Color.parseColor("#0F2A03"), Color.parseColor("#6FAF3D"), Color.parseColor("#C0DD97"), Color.parseColor("#8FB868"), Color.parseColor("#EAF3DE"), Color.parseColor("#2E5A16"), "VALE A PENA")
        }

        val fundoCard = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(paleta.fundoTopo, paleta.fundoBase)
        ).apply {
            cornerRadius = dp(20).toFloat()
            setStroke(dp(2), paleta.borda)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = fundoCard
            setPadding(dp(20), dp(10), dp(20), dp(10))
        }

        val linhaTopo = TextView(this).apply {
            text = paleta.texto
            setTextColor(paleta.titulo)
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
        linhaMetricas.addView(criarColuna("R$/KM", valorKmCalc?.let { "%.2f".format(it) } ?: "--", paleta.rotulo, paleta.valor))
        linhaMetricas.addView(criarColuna("R$/HORA", valorHoraEfetivo?.let { "%.0f".format(it) } ?: "--", paleta.rotulo, paleta.valor))
        linhaMetricas.addView(criarColuna("R$/MIN", valorMinutoEfetivo?.let { "%.2f".format(it) } ?: "--", paleta.rotulo, paleta.valor))
        container.addView(linhaMetricas)

        val linhaSeparadora = android.view.View(this).apply {
            setBackgroundColor(Color.parseColor("#26FFFFFF"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                topMargin = dp(8); bottomMargin = dp(8)
            }
        }
        container.addView(linhaSeparadora)

        val linhaLucro = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val txtLucro = TextView(this).apply {
            text = lucro?.let { "R$ %.2f".format(it) } ?: "--"
            setTextColor(paleta.valor)
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
        }
        linhaLucro.addView(txtLucro)

        if (percentualLucro != null) {
            val espacoPill = android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(8), 1)
            }
            val pillFundo = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(paleta.pillFundo)
            }
            val txtPercentual = TextView(this).apply {
                text = "%.0f%% lucro".format(percentualLucro)
                setTextColor(paleta.titulo)
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                background = pillFundo
                setPadding(dp(8), dp(2), dp(8), dp(2))
            }
            linhaLucro.addView(espacoPill)
            linhaLucro.addView(txtPercentual)
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
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor("#CC1B5E20"))
            }
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
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor("#CC000000"))
            }
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
        const val EXTRA_PERCENTUAL_LUCRO = "extra_percentual_lucro"
        const val EXTRA_NIVEL = "extra_nivel"

        private const val DURACAO_EXIBICAO_MS = 10000L

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
