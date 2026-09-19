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
            setColor(Color.parseColor("#0F1B2D"))
            cornerRadius = dp(18).toFloat()
            setStroke(dp(1), Color.parseColor("#661FE7C4"))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = fundo
            setPadding(dp(18), dp(14), dp(18), dp(14))
        }

        val emoji = when (plataforma) { "Uber" -> "⬛"; "99" -> "🟡"; else -> "🚗" }

        container.addView(TextView(this).apply {
            text = "$emoji $plataforma · ÚLTIMA VIAGEM DETECTADA"
            setTextColor(Color.parseColor("#8B96AC"))
            textSize = 10.5f
            setTypeface(typeface, Typeface.BOLD)
        })

        container.addView(TextView(this).apply {
            text = "R$ %.2f".format(valor)
            setTextColor(Color.parseColor("#1FE7A0"))
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(2))
        })

        val detalhes = listOfNotNull(categoria, horario).joinToString("  •  ")
        if (detalhes.isNotBlank()) {
            container.addView(TextView(this).apply {
                text = detalhes
                setTextColor(Color.parseColor("#8B96AC"))
                textSize = 11f
                setPadding(0, 0, 0, dp(8))
            })
        } else {
            container.addView(android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(8))
            })
        }

        container.addView(TextView(this).apply {
            text = "KM PERCORRIDO NESSA CORRIDA (OPCIONAL)"
            setTextColor(Color.parseColor("#1FE7C4"))
            textSize = 9.5f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(4))
        })

        val edtKm = EditText(this).apply {
            hint = "Toque aqui pra digitar (ex: 8,5)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#5C6B80"))
            gravity = Gravity.CENTER
            isFocusable = true
            isFocusableInTouchMode = true
            isCursorVisible = true
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A2236"))
                cornerRadius = dp(10).toFloat()
            }
            setPadding(dp(10), dp(10), dp(10), dp(10))
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
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
            text = "✔ Salvar corrida"
            setTextColor(Color.parseColor("#052018"))
            textSize = 12.5f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(Color.parseColor("#1FE7A0")); cornerRadius = dp(10).toFloat() }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) }
            setOnClickListener {
                esconderTeclado()
                val km = edtKm.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
                salvar(valor, plataforma, km)
            }
        }

        val btnIgnorar = TextView(this).apply {
            text = "✕ Ignorar"
            setTextColor(Color.parseColor("#8B96AC"))
            textSize = 12.5f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(Color.parseColor("#1A2236")); cornerRadius = dp(10).toFloat() }
            setPadding(dp(14), dp(10), dp(14), dp(10))
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
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(120)
            width = dp(270)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        overlayView = container
        try { windowManager?.addView(container, params) } catch (e: Exception) { }

        handler.postDelayed({ esconderTeclado(); limpar(); stopSelf() }, 30_000L)
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
