package com.motorista.calc

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WeeklyActivity : AppCompatActivity() {

    private enum class Periodo { HOJE, SEMANA, MES, TUDO }
    private var periodoAtual = Periodo.SEMANA

    private val diasSemana = arrayOf("D", "S", "T", "Q", "Q", "S", "S")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly)

        findViewById<android.view.View>(R.id.tabHoje).setOnClickListener { selecionarPeriodo(Periodo.HOJE) }
        findViewById<android.view.View>(R.id.tabSemana).setOnClickListener { selecionarPeriodo(Periodo.SEMANA) }
        findViewById<android.view.View>(R.id.tabMes).setOnClickListener { selecionarPeriodo(Periodo.MES) }
        findViewById<android.view.View>(R.id.tabTudo).setOnClickListener { selecionarPeriodo(Periodo.TUDO) }

        findViewById<android.view.View>(R.id.btnExportarCsv).setOnClickListener { exportarCsv() }

        selecionarPeriodo(Periodo.SEMANA)
    }

    private fun selecionarPeriodo(periodo: Periodo) {
        periodoAtual = periodo
        atualizarTabs()
        montarResumo()
    }

    private fun atualizarTabs() {
        val tabs = mapOf(
            Periodo.HOJE to R.id.tabHoje,
            Periodo.SEMANA to R.id.tabSemana,
            Periodo.MES to R.id.tabMes,
            Periodo.TUDO to R.id.tabTudo
        )
        for ((periodo, id) in tabs) {
            val view = findViewById<TextView>(id)
            val selecionado = periodo == periodoAtual
            view.background = ContextCompat.getDrawable(this, if (selecionado) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            view.setTextColor(Color.parseColor(if (selecionado) "#08131A" else "#8B96AC"))
            view.setTypeface(view.typeface, if (selecionado) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun inicioDoPeriodo(): Long {
        val cal = Calendar.getInstance()
        return when (periodoAtual) {
            Periodo.HOJE -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            Periodo.SEMANA -> System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            Periodo.MES -> System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            Periodo.TUDO -> 0L
        }
    }

    private fun montarResumo() {
        val inicio = inicioDoPeriodo()
        val jornadas = JornadaStorage.listarTodas(this).filter { it.dataInicioMillis >= inicio }
        val statsPorJornada = jornadas.map { JornadaStorage.calcularStats(this, it) }

        val totalGanho = statsPorJornada.sumOf { it.ganhoBruto }
        val totalKm = statsPorJornada.sumOf { it.kmRodados }
        val totalMin = statsPorJornada.sumOf { it.tempoTrabalhadoMin }
        val horas = totalMin / 60.0
        val mediaPorHora = if (horas > 0) totalGanho / horas else 0.0
        val totalMeta = jornadas.sumOf { it.metaDiaria }
        val taxaMedia = if (totalMeta > 0) (totalGanho / totalMeta) * 100 else 0.0

        findViewById<TextView>(R.id.txtTotalGanho).text = "R$ %.2f".format(totalGanho)
        findViewById<TextView>(R.id.txtHorasTrabalhadas).text = "%02d:%02d".format(totalMin / 60, totalMin % 60)
        findViewById<TextView>(R.id.txtMediaPorHora).text = "R$ %.2f".format(mediaPorHora)
        findViewById<TextView>(R.id.txtKmRodados).text = "%.0f".format(totalKm)

        findViewById<TextView>(R.id.txtTaxaMeta).text = "%.0f%%".format(taxaMedia)
        findViewById<TextView>(R.id.txtQtdJornadas).text = "${jornadas.size} jornada(s) no período"

        val barraMeta = findViewById<android.view.View>(R.id.barraMeta)
        val larguraMaxima = (280 * resources.displayMetrics.density).toInt()
        barraMeta.layoutParams = barraMeta.layoutParams.apply {
            width = (larguraMaxima * (taxaMedia / 100.0).coerceIn(0.0, 1.0)).toInt().coerceAtLeast(4)
        }
        barraMeta.setBackgroundColor(Color.parseColor(if (taxaMedia >= 100) "#1FE7A0" else "#F5A623"))
        barraMeta.requestLayout()

        montarGraficoUltimosDias()
    }

    private fun montarGraficoUltimosDias() {
        val formatoChave = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendario = Calendar.getInstance()

        val chaves = mutableListOf<String>()
        val labels = mutableListOf<String>()
        repeat(7) {
            chaves.add(0, formatoChave.format(calendario.time))
            labels.add(0, diasSemana[calendario.get(Calendar.DAY_OF_WEEK) - 1])
            calendario.add(Calendar.DAY_OF_MONTH, -1)
        }

        val todasJornadas = JornadaStorage.listarTodas(this)
        val totalPorDia = chaves.map { chave ->
            todasJornadas.filter { formatoChave.format(java.util.Date(it.dataInicioMillis)) == chave }
                .sumOf { JornadaStorage.calcularStats(this, it).ganhoBruto }
        }

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
                layoutParams = LinearLayout.LayoutParams(0, alturaMaximaPx, 1f).apply { marginStart = 4; marginEnd = 4 }
            }
            val barra = android.view.View(this).apply {
                setBackgroundColor(Color.parseColor(if (i == indiceMelhorDia) "#1FE7A0" else "#17415C"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, alturaPx)
            }
            colunaWrapper.addView(barra)
            containerBarras.addView(colunaWrapper)

            val txtLabel = TextView(this).apply {
                text = labels[i]
                textSize = 10f
                gravity = android.view.Gravity.CENTER
                setTextColor(Color.parseColor(if (i == indiceMelhorDia) "#1FE7A0" else "#8B96AC"))
                if (i == indiceMelhorDia) setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            containerLabels.addView(txtLabel)
        }
    }

    private fun exportarCsv() {
        try {
            val jornadas = JornadaStorage.listarTodas(this)
            val pasta = File(getExternalFilesDir(null), "exportacoes")
            if (!pasta.exists()) pasta.mkdirs()

            val nomeArquivo = "jornadas_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(java.util.Date()) + ".csv"
            val arquivo = File(pasta, nomeArquivo)
            val formatoDataHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            arquivo.bufferedWriter().use { writer ->
                writer.write("Inicio;Fim;Meta;Ganho Bruto;Km;Horas;R$ por Hora;R$ por Km;Custo Combustivel;Custo Fixo;Lucro Liquido;% Meta\n")
                for (j in jornadas.sortedByDescending { it.dataInicioMillis }) {
                    val stats = JornadaStorage.calcularStats(this, j)
                    writer.write(
                        listOf(
                            formatoDataHora.format(java.util.Date(j.dataInicioMillis)),
                            j.dataFimMillis?.let { formatoDataHora.format(java.util.Date(it)) } ?: "Em andamento",
                            "%.2f".format(j.metaDiaria),
                            "%.2f".format(stats.ganhoBruto),
                            "%.2f".format(stats.kmRodados),
                            "%.2f".format(stats.tempoTrabalhadoMin / 60.0),
                            "%.2f".format(stats.valorPorHora),
                            "%.2f".format(stats.valorPorKm),
                            "%.2f".format(stats.custoCombustivel),
                            "%.2f".format(stats.custoFixo),
                            "%.2f".format(stats.lucroLiquido),
                            "%.0f".format(stats.percentualMeta)
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
