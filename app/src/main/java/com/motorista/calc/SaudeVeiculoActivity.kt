package com.motorista.calc

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SaudeVeiculoActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saude_veiculo)
        container = findViewById(R.id.containerItensSaude)
    }

    override fun onResume() {
        super.onResume()
        atualizarTela()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun atualizarTela() {
        val saudeGeral = ManutencaoStorage.saudeGeral(this)
        val txtSaudeGeral = findViewById<TextView>(R.id.txtSaudeGeral)
        val corSaude = corPorPercentual(saudeGeral)

        if (saudeGeral != null) {
            txtSaudeGeral.text = "%.0f%%".format(saudeGeral)
            txtSaudeGeral.setTextColor(Color.parseColor(corSaude))
        } else {
            txtSaudeGeral.text = "—"
            txtSaudeGeral.setTextColor(Color.parseColor("#8B96AC"))
        }

        val kmAtual = ManutencaoStorage.kmAtualEstimada(this)
        val itens = ManutencaoStorage.itensAtuais(this)

        container.removeAllViews()

        if (itens.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Nenhum item de manutenção cadastrado ainda. Cadastre em 🔧 Manutenções."
                setTextColor(Color.parseColor("#8B96AC"))
                textSize = 13f
            })
            return
        }

        for (item in itens.sortedBy { it.tipo }) {
            val percentual = ManutencaoStorage.percentualSaude(item, kmAtual)
            val cor = corPorPercentual(percentual)

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(this@SaudeVeiculoActivity, R.drawable.bg_card_dark)
                setPadding(dp(16), dp(14), dp(16), dp(14))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(10)
                }
            }

            val linhaTopo = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            linhaTopo.addView(TextView(this).apply {
                text = item.tipo
                setTextColor(Color.WHITE)
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            linhaTopo.addView(TextView(this).apply {
                text = if (percentual != null) "%.0f%%".format(percentual) else "sem lembrete"
                setTextColor(Color.parseColor(cor))
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            card.addView(linhaTopo)

            if (percentual != null && item.proximaKm != null && kmAtual != null) {
                val kmRestante = item.proximaKm - kmAtual
                card.addView(TextView(this).apply {
                    text = if (kmRestante >= 0) "%.0f km restantes".format(kmRestante) else "Venceu há %.0f km".format(-kmRestante)
                    setTextColor(Color.parseColor("#8B96AC"))
                    textSize = 11f
                    setPadding(0, dp(2), 0, dp(6))
                })

                val trilho = android.widget.FrameLayout(this).apply {
                    background = ContextCompat.getDrawable(this@SaudeVeiculoActivity, R.drawable.bg_input_verde)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8)).apply {
                        bottomMargin = dp(10)
                    }
                }
                val preenchido = android.view.View(this).apply {
                    setBackgroundColor(Color.parseColor(cor))
                }
                trilho.addView(preenchido, android.widget.FrameLayout.LayoutParams(0, dp(8)))
                trilho.post {
                    val largura = (trilho.width * (percentual / 100.0)).toInt().coerceAtLeast(dp(4))
                    preenchido.layoutParams = preenchido.layoutParams.apply { width = largura }
                    preenchido.requestLayout()
                }
                card.addView(trilho)
            } else {
                card.addView(TextView(this).apply {
                    text = "Sem lembrete por km configurado para esse item."
                    setTextColor(Color.parseColor("#8B96AC"))
                    textSize = 11f
                    setPadding(0, dp(2), 0, dp(10))
                })
            }

            val linhaBotoes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            linhaBotoes.addView(TextView(this).apply {
                text = "✔ Registrar serviço"
                setTextColor(Color.parseColor("#1FE7A0"))
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    ManutencaoStorage.registrarServico(this@SaudeVeiculoActivity, item)
                    atualizarTela()
                }
            })
            linhaBotoes.addView(TextView(this).apply {
                text = "🗑️"
                textSize = 14f
                setOnClickListener {
                    ManutencaoStorage.apagar(this@SaudeVeiculoActivity, item.id)
                    atualizarTela()
                }
            })
            card.addView(linhaBotoes)

            container.addView(card)
        }
    }

    private fun corPorPercentual(percentual: Double?): String {
        if (percentual == null) return "#8B96AC"
        return when {
            percentual >= 50 -> "#1FE7A0"
            percentual >= 20 -> "#F5A623"
            else -> "#F5576B"
        }
    }
}
