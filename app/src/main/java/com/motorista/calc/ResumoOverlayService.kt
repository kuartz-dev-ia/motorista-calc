package com.motorista.calc

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class ResumoOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: android.view.View? = null
    private var edtKmRef: EditText? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val valor = intent?.getDoubleExtra(EXTRA_VALOR, -1.0)?.takeIf { it > 0 } ?: return START_NOT_STICKY
        val plataforma = intent.getStringExtra(EXTRA_PLATAFORMA) ?: "Outro"
        val categoria = intent.getStringExtra(EXTRA_CATEGORIA)
        val horario = intent.getStringExtra(EXTRA_HORARIO)

        mostrarCard(valor, plataforma, categoria, horario)
        return START_NOT_STICKY
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun mostrarCard(valor: Double, plataforma: String, categoria: String?, horario: String?) {
        limpar()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val fundo = GradientDrawable().apply {
            orientation = GradientDrawable.Orientation.TL_BR
            colors = intArrayOf(Color.parseColor("#132030"), Color.parseColor("#0B141F"))
            cornerRadius = dp(22).toFloat()
            setStroke(dp(1), Color.parseColor("#801FE7C4"))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = fundo
            setPadding(dp(22), dp(18), dp(22), dp(18))
        }

        val emoji = when (plataforma) { "Uber" -> "⬛"; "99" -> "🟡"; else -> "🚗" }

        // Cabeçalho
        val cabecalho = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        cabecalho.addView(TextView(this).apply {
            text = "$emoji  $plataforma"
            setTextColor(Color.parseColor("#E4E7EC"))
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
        })
        container.addView(cabecalho)

        container.addView(TextView(this).apply {
            text = "ÚLTIMA VIAGEM DETECTADA"
            setTextColor(Color.parseColor("#1FE7C4"))
            textSize = 9.5f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(2), 0, dp(10))
        })

        // Valor em destaque
        container.addView(TextView(this).apply {
            text = "R$ %.2f".format(valor)
            setTextColor(Color.parseColor("#1FE7A0"))
            textSize = 30f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
        })

        // Pills de horário/categoria
        if (categoria != null || horario != null) {
            val linhaPills = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, dp(4))
            }
            horario?.let {
                linhaPills.addView(criarPill("🕐 $it", "#3DB8F5"))
            }
            if (categoria != null && horario != null) {
                linhaPills.addView(android.view.View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(8), 1) })
            }
            categoria?.let {
                linhaPills.addView(criarPill(it, "#9B6BF5"))
            }
            container.addView(linhaPills)
        }

        // Separador
        container.addView(android.view.View(this).apply {
            setBackgroundColor(Color.parseColor("#221FE7C4"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                topMargin = dp(14); bottomMargin = dp(14)
            }
        })

        container.addView(TextView(this).apply {
            text = "KM PERCORRIDO (OPCIONAL)"
            setTextColor(Color.parseColor("#8B96AC"))
            textSize = 9.5f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dp(6))
        })

        val edtKm = EditText(this).apply {
            hint = "Toque pra digitar (ex: 8,5)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#5C6B80"))
            gravity = Gravity.CENTER
            isFocusable = true
            isFocusableInTouchMode = true
            isCursorVisible = true
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A2236"))
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(10), dp(12), dp(10), dp(12))
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(14)
            }
            setOnClickListener {
                requestFocus()
                mostrarTeclado(this)
            }
        }
        edtKmRef = edtKm
        container.addView(edtKm)

        val linhaBotoes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        val btnSalvar = TextView(this).apply {
            text = "✔  Salvar corrida"
            setTextColor(Color.parseColor("#052018"))
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
                colors = intArrayOf(Color.parseColor("#14C98B"), Color.parseColor("#0FBFA0"))
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) }
            setOnClickListener {
                esconderTeclado()
                val km = edtKm.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
                salvar(valor, plataforma, km)
            }
        }

        val btnIgnorar = TextView(this).apply {
            text = "✕  Ignorar"
            setTextColor(Color.parseColor("#8B96AC"))
            textSize = 13f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A2236"))
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                esconderTeclado()
                limpar()
                stopSelf()
            }
        }

        linhaBotoes.addView(btnSalvar)
        linhaBotoes.addView(btnIgnorar)
        container.addView(linhaBotoes)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Centralizado na tela (não mais colado no topo).
            gravity = Gravity.CENTER
            width = dp(280)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        overlayView = container
        try { windowManager?.addView(container, params) } catch (e: Exception) { }

        handler.postDelayed({ esconderTeclado(); limpar(); stopSelf() }, 45_000L)
    }

    private fun criarPill(texto: String, cor: String): TextView {
        return TextView(this).apply {
            text = texto
            setTextColor(Color.parseColor(cor))
            textSize = 10.5f
            setTypeface(typeface, Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A2236"))
                cornerRadius = dp(20).toFloat()
            }
            setPadding(dp(10), dp(5), dp(10), dp(5))
        }
    }

    private fun mostrarTeclado(campo: EditText) {
        handler.postDelayed({
            try {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(campo, InputMethodManager.SHOW_FORCED)
            } catch (e: Exception) { }
        }, 100L)
    }

    private fun esconderTeclado() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            edtKmRef?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
        } catch (e: Exception) { }
    }

    private fun salvar(valor: Double, plataforma: String, km: Double) {
        try {
            val valorPorKm = if (km > 0) valor / km else null
            val (id, novo) = HistoricoStorage.adicionarRegistro(
                context = this,
                valorTotal = valor,
                distanciaTotalKm = km,
                tempoTotalMin = 0,
                valorPorKm = valorPorKm,
                valorPorHora = null,
                lucroLiquido = valor,
                valeAPena = true,
                plataforma = plataforma
            )
            if (novo) HistoricoStorage.marcarAceita(this, id)
            Toast.makeText(this, "Corrida salva no histórico de hoje", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
        }
        limpar()
        stopSelf()
    }

    private fun limpar() {
        overlayView?.let { try { windowManager?.removeView(it) } catch (e: Exception) { } }
        overlayView = null
        edtKmRef = null
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        esconderTeclado()
        limpar()
    }

    companion object {
        const val EXTRA_VALOR = "extra_valor_resumo"
        const val EXTRA_PLATAFORMA = "extra_plataforma_resumo"
        const val EXTRA_CATEGORIA = "extra_categoria_resumo"
        const val EXTRA_HORARIO = "extra_horario_resumo"
    }
}
