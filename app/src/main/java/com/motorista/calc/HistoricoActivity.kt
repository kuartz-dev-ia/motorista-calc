package com.motorista.calc

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoricoActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private val expandidos = mutableSetOf<Long>()

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
                isClickable = true
                isFocusable = true
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
            val setaExpandir = TextView(this).apply {
                text = if (jornada.id in expandidos) "▲" else "▼"
                setTextColor(Color.parseColor("#8B96AC"))
                textSize = 13f
                setPadding(16, 0, 0, 0)
            }
            cabecalho.addView(textoCabecalho)
            cabecalho.addView(badgeMeta)
            cabecalho.addView(setaExpandir)
            card.addView(cabecalho)

            val linhaMetricas = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 16, 0, 12)
            }
            linhaMetricas.addView(criarMiniCard("💲 Ganho", "R$ %.2f".format(stats.ganhoBruto), R.drawable.bg_tint_green, "#1FE7A0"))
            linhaMetricas.addView(criarMiniCard("🕐 R$/h", "R$ %.2f".format(stats.valorPorHora), R.drawable.bg_tint_blue, "#3DB8F5"))
            linhaMetricas.addView(criarMiniCard("📍 R$/km", "R$ %.2f".format(stats.valorPorKm), R.drawable.bg_tint_purple, "#9B6BF5"))
            card.addView(linhaMetricas)

            val linhaExtra = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 0, 0, 12)
            }
            linhaExtra.addView(criarMiniCard("⛽ Combustível", "R$ %.2f".format(stats.custoCombustivel), R.drawable.bg_tint_red, "#F5576B"))
            linhaExtra.addView(criarMiniCard("💧 Lucro líquido", "R$ %.2f".format(stats.lucroLiquido), R.drawable.bg_tint_green, if (stats.lucroLiquido >= 0) "#1FE7A0" else "#F5576B"))
            card.addView(linhaExtra)

            val containerDetalhes = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 8, 0, 12)
                visibility = if (jornada.id in expandidos) android.view.View.VISIBLE else android.view.View.GONE
            }
            montarDetalhesDaJornada(containerDetalhes, jornada, stats)
            card.addView(containerDetalhes)

            cabecalho.setOnClickListener {
                if (jornada.id in expandidos) {
                    expandidos.remove(jornada.id)
                    containerDetalhes.visibility = android.view.View.GONE
                    setaExpandir.text = "▼"
                } else {
                    expandidos.add(jornada.id)
                    containerDetalhes.visibility = android.view.View.VISIBLE
                    setaExpandir.text = "▲"
                }
            }

            val btnExcluir = TextView(this).apply {
                text = "🗑️ Excluir jornada"
                setTextColor(Color.parseColor("#F55757"))
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setOnClickListener {
                    AlertDialog.Builder(this@HistoricoActivity, R.style.DialogTemaEscuro)
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

    private fun criarMiniCard(rotulo: String, valor: String, fundoRes: Int, corValor: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@HistoricoActivity, fundoRes)
            setPadding(16, 14, 16, 14)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 8 }
            addView(TextView(this@HistoricoActivity).apply {
                text = rotulo
                setTextColor(Color.parseColor("#8B96AC"))
                textSize = 9f
            })
            addView(TextView(this@HistoricoActivity).apply {
                text = valor
                setTextColor(Color.parseColor(corValor))
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
            })
        }
    }

    private fun montarDetalhesDaJornada(container: LinearLayout, jornada: Jornada, stats: JornadaStats) {
        val fim = jornada.dataFimMillis ?: System.currentTimeMillis()
        val corridas = HistoricoStorage.listarEntre(this, jornada.dataInicioMillis, fim).filter { it.aceita && !it.cancelada }
        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

        val separador = android.view.View(this).apply {
            setBackgroundColor(Color.parseColor("#1A2236"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2).apply { bottomMargin = 10 }
        }
        container.addView(separador)

        if (corridas.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Nenhuma corrida individual registrada nessa jornada."
                setTextColor(Color.parseColor("#8B96AC"))
                textSize = 11f
            })
            return
        }

        container.addView(TextView(this).apply {
            text = "Corridas desta jornada:"
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, 6)
        })

        for (corrida in corridas.sortedByDescending { it.dataHora }) {
            val emoji = when (corrida.plataforma) {
                "Uber" -> "⬛"
                "99" -> "🟡"
                else -> "🚗"
            }

            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 4, 0, 4)
            }

            linha.addView(TextView(this).apply {
                text = "$emoji ${formatoHora.format(Date(corrida.dataHora))} — R$ %.2f (%.1f km)".format(corrida.valorTotal, corrida.distanciaTotalKm)
                setTextColor(Color.parseColor("#E4E7EC"))
                textSize = 11.5f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            linha.addView(TextView(this).apply {
                text = "✏️"
                textSize = 13f
                setPadding(12, 0, 12, 0)
                setOnClickListener { abrirDialogoEditar(corrida) }
            })

            linha.addView(TextView(this).apply {
                text = "🗑️"
                textSize = 13f
                setOnClickListener { confirmarExclusaoCorrida(corrida) }
            })

            container.addView(linha)
        }
    }

    private fun abrirDialogoEditar(corrida: RegistroCorrida) {
        val input = EditText(this).apply {
            setText("%.2f".format(corrida.valorTotal))
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#8B96AC"))
            background = ContextCompat.getDrawable(this@HistoricoActivity, R.drawable.bg_input_verde)
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(this, R.style.DialogTemaEscuro)
            .setTitle("Editar valor da corrida")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val novoValor = input.text.toString().toDoubleOrNull()
                if (novoValor == null || novoValor <= 0) {
                    Toast.makeText(this, "Valor inválido", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                HistoricoStorage.editarValor(this, corrida.id, novoValor)
                atualizarTela()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarExclusaoCorrida(corrida: RegistroCorrida) {
        AlertDialog.Builder(this, R.style.DialogTemaEscuro)
            .setTitle("Excluir corrida")
            .setMessage("Tem certeza que quer excluir essa corrida?")
            .setPositiveButton("Excluir") { _, _ ->
                HistoricoStorage.apagarRegistro(this, corrida.id)
                atualizarTela()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
