package com.motorista.calc

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MaisOpcoesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_mais_opcoes)

        findViewById<android.view.View>(
            R.id.btnManutencoes
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ManutencoesActivity::class.java
                )
            )
        }

        findViewById<android.view.View>(
            R.id.btnAbastecimentos
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    AbastecimentosActivity::class.java
                )
            )
        }

        findViewById<android.view.View>(
            R.id.btnDocumentos
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    DocumentosActivity::class.java
                )
            )
        }

        findViewById<android.view.View>(
            R.id.btnPrints
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    PrintsActivity::class.java
                )
            )
        }

        findViewById<android.view.View>(
            R.id.btnGravacoes
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    RecordingsActivity::class.java
                )
            )
        }

        adicionarBotaoDashboard()
    }

    /**
     * Adiciona o botão Dashboard no início
     * da lista de opções.
     *
     * O layout possui um ScrollView como raiz,
     * portanto precisamos acessar o LinearLayout
     * que está dentro dele.
     */
    private fun adicionarBotaoDashboard() {

        val scrollView =
            findViewById<ScrollView>(
                android.R.id.content
            )

        val raiz =
            scrollView.getChildAt(0) as? LinearLayout
                ?: return

        val botao =
            TextView(this).apply {

                text = "📊  Dashboard"

                setTextColor(
                    Color.WHITE
                )

                textSize = 14f

                setTypeface(
                    typeface,
                    Typeface.BOLD
                )

                gravity =
                    Gravity.CENTER_VERTICAL

                background =
                    ContextCompat.getDrawable(
                        this@MaisOpcoesActivity,
                        R.drawable.bg_card_dark
                    )

                setPadding(
                    dp(18),
                    dp(16),
                    dp(18),
                    dp(16)
                )

                isClickable = true
                isFocusable = true

                setOnClickListener {

                    startActivity(
                        Intent(
                            this@MaisOpcoesActivity,
                            DashboardActivity::class.java
                        )
                    )
                }
            }

        val parametros =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                setMargins(
                    dp(16),
                    dp(8),
                    dp(16),
                    dp(8)
                )
            }

        raiz.addView(
            botao,
            0,
            parametros
        )
    }

    private fun dp(
        valor: Int
    ): Int {

        return (
            valor *
                resources.displayMetrics.density
            ).toInt()
    }
}
