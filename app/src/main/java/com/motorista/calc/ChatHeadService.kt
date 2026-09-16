package com.motorista.calc

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatHeadService : Service() {

    private var windowManager: WindowManager? = null
    private var bolha: View? = null
    private var badgeBolha: TextView? = null
    private var painel: View? = null
    private var painelAberto = false

    private var limiteHistorico = 10

    private val handler = Handler(Looper.getMainLooper())
    private val tickerRunnable = object : Runnable {
        override fun run() {
            atualizarBadge()
            if (painelAberto) atualizarPainel()
            val jornadaAtiva = JornadaStorage.jornadaAtiva(this@ChatHeadService)
            if (jornadaAtiva == null) {
                stopSelf()
                return
            }
            handler.postDelayed(this, 15_000L)
        }
    }

    private var paramsBolha: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        criarCanal()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (bolha == null) {
            startForeground(NOTIFICACAO_ID, montarNotificacao())
            criarBolha()
            handler.post(tickerRunnable)
        }
        return START_STICKY
    }

    private fun criarCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CANAL_ID, "Bolha flutuante", NotificationManager.IMPORTANCE_MIN)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(canal)
        }
    }

    private fun montarNotificacao(): android.app.Notification {
        return NotificationCompat.Builder(this, CANAL_ID)
            .setContentTitle("Motorista Calc ativo")
            .setContentText("Bolha flutuante disponível durante a jornada")
            .setSmallIcon(android.R.drawable.presence_online)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun tipoJanela() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

    private fun criarBolha() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val circulo = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(this@ChatHeadService, R.drawable.bg_bubble_circle)
        }
        val emoji = TextView(this).apply {
            text = "🚗"
            textSize = 20f
        }
        circulo.addView(emoji, LinearLayout.LayoutParams(dp(52), dp(52)))

        val badge = TextView(this).apply {
            visibility = View.GONE
            text = "●"
            setTextColor(Color.parseColor("#F5576B"))
            textSize = 18f
        }
        badgeBolha = badge

        val framePai = android.widget.FrameLayout(this)
        framePai.addView(circulo, android.widget.FrameLayout.LayoutParams(dp(52), dp(52)))
        framePai.addView(badge, android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.TOP or Gravity.END })

        container.addView(framePai)
        bolha = container

        paramsBolha = WindowManager.LayoutParams(
            dp(52), dp(52), tipoJanela(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(300)
            y = dp(200)
        }

        var xInicial = 0
        var yInicial = 0
        var toqueXInicial = 0f
        var toqueYInicial = 0f
        var moveu = false

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    xInicial = paramsBolha!!.x
                    yInicial = paramsBolha!!.y
                    toqueXInicial = event.rawX
                    toqueYInicial = event.rawY
                    moveu = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - toqueXInicial).toInt()
                    val dy = (event.rawY - toqueYInicial).toInt()
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) moveu = true
                    paramsBolha!!.x = xInicial + dx
                    paramsBolha!!.y = yInicial + dy
                    try { windowManager?.updateViewLayout(container, paramsBolha) } catch (e: Exception) { }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moveu) alternarPainel()
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(container, paramsBolha)
        } catch (e: Exception) { }
    }

    private fun alternarPainel() {
        if (painelAberto) {
            fecharPainel()
        } else {
            abrirPainel()
        }
    }

    private fun abrirPainel() {
        if (painel != null) return
        val jornada = JornadaStorage.jornadaAtiva(this) ?: return

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@ChatHeadService, R.drawable.bg_card_dark)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val cabecalho = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        cabecalho.addView(TextView(this).apply {
            text = "🚗 Motorista Calc"
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        cabecalho.addView(TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#8B96AC"))
            textSize = 16f
            setPadding(dp(8), 0, 0, 0)
            setOnClickListener { fecharPainel() }
        })
        raiz.addView(cabecalho)

        val txtTempo = TextView(this).apply {
            id = ID_TXT_TEMPO
            setTextColor(Color.WHITE)
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(8))
        }
        raiz.addView(txtTempo)

        val grid1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        grid1.addView(criarMiniCard("💲 Faturamento", ID_TXT_GANHO, R.drawable.bg_tint_green, "#1FE7A0"))
        grid1.addView(criarMiniCard("💧 Lucro líquido", ID_TXT_LUCRO, R.drawable.bg_tint_green, "#1FE7A0"))
        raiz.addView(grid1)

        val grid2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(6), 0, 0) }
        grid2.addView(criarMiniCard("🕐 R$/hora", ID_TXT_RHORA, R.drawable.bg_tint_blue, "#3DB8F5"))
        grid2.addView(criarMiniCard("📍 R$/km", ID_TXT_RKM, R.drawable.bg_tint_purple, "#9B6BF5"))
        raiz.addView(grid2)

        val grid3 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(6), 0, dp(10)) }
        grid3.addView(criarMiniCard("🚗 Corridas / Km", ID_TXT_CORRIDAS_KM, R.drawable.bg_card_dark, "#FFFFFF"))
        grid3.addView(criarMiniCard("⛽ Gastos do dia", ID_TXT_GASTOS, R.drawable.bg_tint_red, "#F5576B"))
        raiz.addView(grid3)

        val txtConsumo = TextView(this).apply {
            id = ID_TXT_CONSUMO
            setTextColor(Color.parseColor("#8B96AC"))
            textSize = 10.5f
            setPadding(0, 0, 0, dp(10))
        }
        raiz.addView(txtConsumo)

        val linhaBotoes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 0, 0, dp(10)) }
        val btnPausar = TextView(this).apply {
            id = ID_BTN_PAUSAR
            text = "⏸ Pausar"
            setTextColor(Color.parseColor("#F5A623"))
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(this@ChatHeadService, R.drawable.bg_input_verde)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) }
            setOnClickListener { alternarPausa() }
        }
        val btnFinalizar = TextView(this).apply {
            text = "⏹ Finalizar"
            setTextColor(Color.parseColor("#2E1500"))
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(this@ChatHeadService, R.drawable.bg_cta_stop)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val i = Intent(this@ChatHeadService, EncerrarJornadaActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(i)
                fecharPainel()
            }
        }
        linhaBotoes.addView(btnPausar)
        linhaBotoes.addView(btnFinalizar)
        raiz.addView(linhaBotoes)

        val btnAbrirApp = TextView(this).apply {
            text = "Abrir Motorista Calc →"
            setTextColor(Color.parseColor("#1FE7C4"))
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(10))
            setOnClickListener {
                val i = Intent(this@ChatHeadService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(i)
                fecharPainel()
            }
        }
        raiz.addView(btnAbrirApp)

        raiz.addView(TextView(this).apply {
            text = "Histórico de hoje"
            setTextColor(Color.WHITE)
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(6))
        })

        val linhaFiltro = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 0, 0, dp(8)) }
        for (n in listOf(10, 25, 50, 100)) {
            linhaFiltro.addView(TextView(this).apply {
                text = "$n"
                textSize = 10.5f
                gravity = Gravity.CENTER
                setTextColor(if (n == limiteHistorico) Color.parseColor("#08131A") else Color.parseColor("#8B96AC"))
                background = ContextCompat.getDrawable(this@ChatHeadService, if (n == limiteHistorico) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
                setPadding(dp(10), dp(6), dp(10), dp(6))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) }
                setOnClickListener { limiteHistorico = n; atualizarPainel() }
            })
        }
        raiz.addView(linhaFiltro)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(160))
        }
        val listaContainer = LinearLayout(this).apply {
            id = ID_LISTA_CORRIDAS
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(listaContainer)
        raiz.addView(scroll)

        val params = WindowManager.LayoutParams(
            dp(300), WindowManager.LayoutParams.WRAP_CONTENT, tipoJanela(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (paramsBolha?.x ?: dp(20)).coerceAtMost(resources.displayMetrics.widthPixels - dp(310))
            y = (paramsBolha?.y ?: dp(200)) + dp(60)
        }

        painel = raiz
        try {
            windowManager?.addView(raiz, params)
            painelAberto = true
            atualizarPainel()
            marcarBadgeVisto()
        } catch (e: Exception) { }
    }

    private fun criarMiniCard(rotulo: String, id: Int, fundoRes: Int, corValor: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@ChatHeadService, fundoRes)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) }
            addView(TextView(this@ChatHeadService).apply {
                text = rotulo
                setTextColor(Color.parseColor("#8B96AC"))
                textSize = 8.5f
            })
            addView(TextView(this@ChatHeadService).apply {
                this.id = id
                text = "--"
                setTextColor(Color.parseColor(corValor))
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
            })
        }
    }

    private fun fecharPainel() {
        painel?.let {
            try { windowManager?.removeView(it) } catch (e: Exception) { }
        }
        painel = null
        painelAberto = false
    }

    private fun alternarPausa() {
        val prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)
        val ativoAgora = prefs.getBoolean(RideAccessibilityService.PREF_MONITORAMENTO_ATIVO, true)
        prefs.edit().putBoolean(RideAccessibilityService.PREF_MONITORAMENTO_ATIVO, !ativoAgora).apply()
        atualizarPainel()
    }

    private fun atualizarBadge() {
        val jornada = JornadaStorage.jornadaAtiva(this) ?: return
        val corridas = HistoricoStorage.listarEntre(this, jornada.dataInicioMillis, System.currentTimeMillis()).count { it.aceita && !it.cancelada }
        val prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)
        val vistoAte = prefs.getInt(PREF_CORRIDAS_VISTAS, 0)
        badgeBolha?.visibility = if (corridas > vistoAte) View.VISIBLE else View.GONE
    }

    private fun marcarBadgeVisto() {
        val jornada = JornadaStorage.jornadaAtiva(this) ?: return
        val corridas = HistoricoStorage.listarEntre(this, jornada.dataInicioMillis, System.currentTimeMillis()).count { it.aceita && !it.cancelada }
        getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE).edit().putInt(PREF_CORRIDAS_VISTAS, corridas).apply()
        badgeBolha?.visibility = View.GONE
    }

    private fun atualizarPainel() {
        val raiz = painel ?: return
        val jornada = JornadaStorage.jornadaAtiva(this) ?: run { fecharPainel(); return }
        val stats = JornadaStorage.calcularStats(this, jornada)

        val horas = stats.tempoTrabalhadoMin / 60
        val minutos = stats.tempoTrabalhadoMin % 60
        raiz.findViewById<TextView>(ID_TXT_TEMPO)?.text = "%02d:%02d".format(horas, minutos)
        raiz.findViewById<TextView>(ID_TXT_GANHO)?.text = "R$ %.2f".format(stats.ganhoBruto)
        raiz.findViewById<TextView>(ID_TXT_LUCRO)?.text = "R$ %.2f".format(stats.lucroLiquido)
        raiz.findViewById<TextView>(ID_TXT_RHORA)?.text = "R$ %.2f".format(stats.valorPorHora)
        raiz.findViewById<TextView>(ID_TXT_RKM)?.text = "R$ %.2f".format(stats.valorPorKm)
        raiz.findViewById<TextView>(ID_TXT_GASTOS)?.text = "R$ %.2f".format(stats.custoCombustivel + stats.custoFixo)

        val corridas = HistoricoStorage.listarEntre(this, jornada.dataInicioMillis, System.currentTimeMillis()).filter { it.aceita && !it.cancelada }
        raiz.findViewById<TextView>(ID_TXT_CORRIDAS_KM)?.text = "${corridas.size} / %.1f km".format(stats.kmRodados)

        val prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)
        val (preco, consumo) = when (prefs.getString(RideAccessibilityService.PREF_COMBUSTIVEL_ATIVO, "etanol")) {
            "gasolina" -> Pair(prefs.getFloat(RideAccessibilityService.PREF_PRECO_GASOLINA, 6.10f), prefs.getFloat(RideAccessibilityService.PREF_CONSUMO_GASOLINA, 10.0f))
            "gnv" -> Pair(prefs.getFloat(RideAccessibilityService.PREF_PRECO_GNV, 4.50f), prefs.getFloat(RideAccessibilityService.PREF_CONSUMO_GNV, 12.0f))
            else -> Pair(prefs.getFloat(RideAccessibilityService.PREF_PRECO_ETANOL, 4.20f), prefs.getFloat(RideAccessibilityService.PREF_CONSUMO_ETANOL, 7.0f))
        }
        raiz.findViewById<TextView>(ID_TXT_CONSUMO)?.text = "Consumo: R$ %.2f/L  •  %.1f km/L".format(preco, consumo)

        val pausado = !prefs.getBoolean(RideAccessibilityService.PREF_MONITORAMENTO_ATIVO, true)
        raiz.findViewById<TextView>(ID_BTN_PAUSAR)?.text = if (pausado) "▶ Retomar" else "⏸ Pausar"

        val listaContainer = raiz.findViewById<LinearLayout>(ID_LISTA_CORRIDAS) ?: return
        listaContainer.removeAllViews()
        val minKm = prefs.getFloat(RideAccessibilityService.PREF_MIN_KM, 1.50f)
        val minHora = prefs.getFloat(RideAccessibilityService.PREF_MIN_HORA, 25.0f)
        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

        if (corridas.isEmpty()) {
            listaContainer.addView(TextView(this).apply {
                text = "Nenhuma corrida ainda."
                setTextColor(Color.parseColor("#8B96AC"))
                textSize = 11f
            })
        } else {
            for (c in corridas.sortedByDescending { it.dataHora }.take(limiteHistorico)) {
                val cor = when {
                    !c.valeAPena -> "#F5576B"
                    (c.valorPorKm ?: 0.0) >= minKm * 1.3 || (c.valorPorHora ?: 0.0) >= minHora * 1.3 -> "#1FE7A0"
                    else -> "#F5A623"
                }
                val emoji = when (c.plataforma) { "Uber" -> "⬛"; "99" -> "🟡"; else -> "🚗" }
                val linha = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(4), 0, dp(4))
                }
                linha.addView(View(this).apply {
                    setBackgroundColor(Color.parseColor(cor))
                    layoutParams = LinearLayout.LayoutParams(dp(4), dp(28)).apply { marginEnd = dp(8) }
                })
                linha.addView(TextView(this).apply {
                    text = "$emoji ${formatoHora.format(Date(c.dataHora))} — R$ %.2f (%.1f km)".format(c.valorTotal, c.distanciaTotalKm)
                    setTextColor(Color.parseColor("#E4E7EC"))
                    textSize = 10.5f
                })
                listaContainer.addView(linha)
            }
        }
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickerRunnable)
        fecharPainel()
        bolha?.let { try { windowManager?.removeView(it) } catch (e: Exception) { } }
        bolha = null
    }

    companion object {
        private const val CANAL_ID = "bolha_flutuante"
        private const val NOTIFICACAO_ID = 881
        private const val PREF_CORRIDAS_VISTAS = "corridas_vistas_bolha"
        private const val ID_TXT_TEMPO = 90001
        private const val ID_TXT_GANHO = 90002
        private const val ID_TXT_LUCRO = 90003
        private const val ID_TXT_RHORA = 90004
        private const val ID_TXT_RKM = 90005
        private const val ID_TXT_CORRIDAS_KM = 90006
        private const val ID_TXT_GASTOS = 90007
        private const val ID_TXT_CONSUMO = 90008
        private const val ID_BTN_PAUSAR = 90009
        private const val ID_LISTA_CORRIDAS = 90010
    }
}
