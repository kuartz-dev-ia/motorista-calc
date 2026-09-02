package com.motorista.calc

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WeeklyActivity : AppCompatActivity() {

    private val diasSemana = arrayOf("D", "S", "T", "Q", "Q", "S", "S")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly)

        findViewById<android.view.View>(R.id.btnExportarCsv).setOnClickListener {
            exportarCsv()
        }

        montarResumo()
    }

    private fun montarResumo() {
        val registros = HistoricoStorage.listarUltimosDias(this, 7)
        val aceitas = registros.filter { it.aceita && !it.cancelada }

        val formatoChave = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendario = Calendar.getInstance()

        val chaves = mutableListOf<String>()
        val labels = mutableListOf<String>()
        repeat(7) {
            chaves.add(0, formatoChave.format(calendario.time))
            labels.add(0, diasSemana[calendario.get(Calendar.DAY_OF_WEEK) - 1])
            calendario.add(Calendar.DAY_OF_MONTH, -1)
        }

        val totalPorDia = chaves.map { chave -> aceitas.filter { it.diaChave == chave }.sumOf { it.valorTotal } }
        val maiorValor = totalPorDia.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        val indiceMelhorDia = totalPorDia.indices.maxByOrNull { totalPorDia[it] } ?: -1

        val containerBarras = findViewById<LinearLayout>(R.id.containerBarras)
        val containerLabels = findViewById<LinearLayout>(R.id.containerLabels)
        containerBarras.removeAllViews()
        containerLabels.removeAllViews()

        val alturaMaximaPx = (120 * resources.displayMetrics.density).toInt()

        for (i in chaves.indices) {
            val valor = totalPorDia[i]
            val alturaPx = if (valor > 0) ((valor / maiorValor) * alturaMaximaPx).toInt().coerceAtLeast(6) else 4

            val colunaWrapper = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.BOTTOM
                layoutParams = LinearLayout.LayoutParams(0, alturaMaximaPx, 1f).apply {
                    marginStart = 4; marginEnd = 4
                }
            }
            val barra = android.view.View(this).apply {
                setBackgroundColor(if (i == indiceMelhorDia) Color.parseColor("#8FD35B") else Color.parseColor("#2E5A16"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, alturaPx)
            }
            colunaWrapper.addView(barra)
            containerBarras.addView(colunaWrapper)

            val txtLabel = TextView(this).apply {
                text = labels[i]
                textSize = 10f
                gravity = android.view.Gravity.CENTER
                setTextColor(if (i == indiceMelhorDia) Color.parseColor("#8FD35B") else Color.parseColor("#6C7686"))
                if (i == indiceMelhorDia) setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            containerLabels.addView(txtLabel)
        }

        val totalGanho = aceitas.sumOf { it.valorTotal }
        val totalHoras = aceitas.sumOf { it.tempoTotalMin } / 60.0
        val mediaHora = if (totalHoras > 0) totalGanho / totalHoras else 0.0
        val nomeMelhorDia = if (indiceMelhorDia in chaves.indices && totalPorDia[indiceMelhorDia] > 0) {
            SimpleDateFormat("EEEE", Locale("pt", "BR")).format(formatoChave.parse(chaves[indiceMelhorDia])!!)
                .replaceFirstChar { it.uppercase() }
        } else "—"

        findViewById<TextView>(R.id.txtMelhorDia).text = nomeMelhorDia
        findViewById<TextView>(R.id.txtMediaHora).text = "R$ %.0f".format(mediaHora)
        findViewById<TextView>(R.id.txtTotalSemana).text = "Total da semana: R$ %.2f (%d corridas)".format(totalGanho, aceitas.size)
    }

    private fun exportarCsv() {
        try {
            val registros = HistoricoStorage.listarUltimosDias(this, 30)
            val pasta = File(getExternalFilesDir(null), "exportacoes")
            if (!pasta.exists()) pasta.mkdirs()

            val nomeArquivo = "historico_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(java.util.Date()) + ".csv"
            val arquivo = File(pasta, nomeArquivo)

            val formatoDataHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            arquivo.bufferedWriter().use { writer ->
                writer.write("Data/Hora;Plataforma;Valor;Km;TempoMin;R$ por Km;R$ por Hora;Lucro Liquido;Aceita;Cancelada\n")
                for (r in registros.sortedByDescending { it.dataHora }) {
                    writer.write(
                        listOf(
                            formatoDataHora.format(java.util.Date(r.dataHora)),
                            r.plataforma,
                            "%.2f".format(r.valorTotal),
                            "%.2f".format(r.distanciaTotalKm),
                            r.tempoTotalMin.toString(),
                            r.valorPorKm?.let { "%.2f".format(it) } ?: "",
                            r.valorPorHora?.let { "%.2f".format(it) } ?: "",
                            r.lucroLiquido?.let { "%.2f".format(it) } ?: "",
                            if (r.aceita) "Sim" else "Não",
                            if (r.cancelada) "Sim" else "Não"
                        ).joinToString(";") + "\n"
                    )
                }
            }

            val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", arquivo)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Enviar planilha (Drive, Gmail, etc.)"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Erro ao exportar: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
