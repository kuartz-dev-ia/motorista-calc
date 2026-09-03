package com.motorista.calc

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoricoActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historico)
        container = findViewById(R.id.containerJornadas)
    }

    override fun onResume() {
        super.onResume()
        atualizarTela()
    }

    private fun atualizarTela() {
        val jornadas = JornadaStorage.listarTodas(this).filter { it.dataFimMillis != null }
        val statsPorJornada = jornadas.map { it to JornadaStorage.calcularStats(this, it) }

        val totalGanho = statsPorJornada.sumOf { it.second.ganhoBruto }
        val totalMin = statsPorJornada.sumOf { it.second.tempoTrabalhadoMin }
        val horas = totalMin / 60.0
        val mediaPorHora = if (horas > 0) totalGanho / horas else 0.0

        findViewById<TextView>(R.id.txtResumoGanho).text = "R$ %.2f".format(totalGanho)
        findViewById<TextView>(R.id.txtResumoHoras).text = "%02d:%02d".format(totalMin / 60, totalMin % 60)
        findViewById<TextView>(R.id.txtResumoMedia).text = "R$ %.2f".format(mediaPorHora)
        findViewById<TextView>(R.id.txtResumoJornadas).text = "${jornadas.size}"

        container.removeAllViews()
        val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

        if (statsPorJornada.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Nenhuma jornada concluída ainda."
                setTextColor(Color.parseColor("#8B96AC"))
                textSize = 13f
            })
            return
        }

        for ((jornada, stats) in statsPorJornada.sortedByDescending { it.first.dataInicioMillis }) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(this@HistoricoActivity, R.drawable.bg_card_dark)
                setPadding(28, 28, 28, 28)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 20
                }
            }

            val cabecalho = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val textoCabecalho = TextView(this).apply {
                text = "${formatoData.format(Date(jornada.dataInicioMillis))}\n${formatoHora.format(Date(jornada.dataInicioMillis))} • %.1f km".format(stats.kmRodados)
                setTextColor(Color.parseColor("#FFFFFF"))
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val badgeMeta = TextView(this).apply {
                text = "%.0f%%".format(stats.percentualMeta)
                textSize = 11f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(16, 6, 16, 6)
                if (stats.percentualMeta >= 100) {
                    background = ContextCompat.getDrawable(this@HistoricoActivity, R.drawable.bg_chip_selected)
                    setTextColor(Color.parseColor("#08131A"))
                } else {
                    background = ContextCompat.getDrawable(this@HistoricoActivity, R.drawable.bg_chip_unselected)
                    setTextColor(Color.parseColor("#F5A623"))
                }
            }
            cabecalho.addView(textoCabecalho)
            cabecalho.addView(badgeMeta)
            card.addView(cabecalho)

            val linhaMetricas = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 20, 0, 12)
            }
            linhaMetricas.addView(criarColuna("Ganho", "R$ %.2f".format(stats.ganhoBruto), "#1FE7A0"))
            linhaMetricas.addView(criarColuna("R$/h", "R$ %.2f".format(stats.valorPorHora), "#3DB8F5"))
            linhaMetricas.addView(criarColuna("R$/km", "R$ %.2f".format(stats.valorPorKm), "#F5A623"))
            card.addView(linhaMetricas)

            val txtLucro = TextView(this).apply {
                text = "Lucro líquido real: R$ %.2f".format(stats.lucroLiquido)
                setTextColor(Color.parseColor(if (stats.lucroLiquido >= 0) "#1FE7A0" else "#F55757"))
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, 0, 0, 12)
            }
            card.addView(txtLucro)

            val btnExcluir = TextView(this).apply {
                text = "🗑️ Excluir"
                setTextColor(Color.parseColor("#F55757"))
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setOnClickListener {
                    AlertDialog.Builder(this@HistoricoActivity)
                        .setTitle("Excluir jornada")
                        .setMessage("Tem certeza que quer excluir esta jornada?")
                        .setPositiveButton("Excluir") { _, _ ->
                            JornadaStorage.apagar(this@HistoricoActivity, jornada.id)
                            atualizarTela()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
            card.addView(btnExcluir)

            container.addView(card)
        }
    }

    private fun criarColuna(rotulo: String, valor: String, cor: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(this@HistoricoActivity).apply {
                text = rotulo
                setTextColor(Color.parseColor("#8B96AC"))
                textSize = 10f
            })
            addView(TextView(this@HistoricoActivity).apply {
                text = valor
                setTextColor(Color.parseColor(cor))
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
            })
        }
    }
}
