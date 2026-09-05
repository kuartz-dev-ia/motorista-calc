package com.motorista.calc

import android.app.AlertDialog
import android.app.DatePickerDialog
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistoricoActivity : AppCompatActivity() {

    private enum class Aba { GERAL, METAS, MEDIAS, POR_VIAGEM }
    private var abaAtual = Aba.GERAL

    private var dataInicioMillis: Long = 0L
    private var dataFimMillis: Long = 0L

    private val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val expandidos = mutableSetOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historico)

        val calFim = Calendar.getInstance()
        dataFimMillis = calFim.timeInMillis
        calFim.add(Calendar.DAY_OF_MONTH, -30)
        dataInicioMillis = calFim.timeInMillis

        findViewById<TextView>(R.id.btnDataInicio).setOnClickListener { abrirSeletorData(true) }
        findViewById<TextView>(R.id.btnDataFim).setOnClickListener { abrirSeletorData(false) }

        findViewById<TextView>(R.id.tabGeral).setOnClickListener { selecionarAba(Aba.GERAL) }
        findViewById<TextView>(R.id.tabMetas).setOnClickListener { selecionarAba(Aba.METAS) }
        findViewById<TextView>(R.id.tabMedias).setOnClickListener { selecionarAba(Aba.MEDIAS) }
        findViewById<TextView>(R.id.tabPorViagem).setOnClickListener { selecionarAba(Aba.POR_VIAGEM) }
    }

    override fun onResume() {
        super.onResume()
        atualizarTextoDatas()
        atualizarConteudo()
    }

    private fun abrirSeletorData(ehInicio: Boolean) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = if (ehInicio) dataInicioMillis else dataFimMillis

        DatePickerDialog(
            this,
            R.style.DialogTemaEscuro,
            { _, ano, mes, dia ->
                val novaData = Calendar.getInstance().apply { set(ano, mes, dia, if (ehInicio) 0 else 23, if (ehInicio) 0 else 59, if (ehInicio) 0 else 59) }
                if (ehInicio) dataInicioMillis = novaData.timeInMillis else dataFimMillis = novaData.timeInMillis
                atualizarTextoDatas()
                atualizarConteudo()
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun atualizarTextoDatas() {
        findViewById<TextView>(R.id.btnDataInicio).text = "📅 ${formatoData.format(Date(dataInicioMillis))}"
        findViewById<TextView>(R.id.btnDataFim).text = "📅 ${formatoData.format(Date(dataFimMillis))}"
    }

    private fun selecionarAba(aba: Aba) {
        abaAtual = aba

        val tabs = mapOf(Aba.GERAL to R.id.tabGeral, Aba.METAS to R.id.tabMetas, Aba.MEDIAS to R.id.tabMedias, Aba.POR_VIAGEM to R.id.tabPorViagem)
        for ((chave, id) in tabs) {
            val view = findViewById<TextView>(id)
            val selecionado = chave == abaAtual
            view.background = ContextCompat.getDrawable(this, if (selecionado) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            view.setTextColor(Color.parseColor(if (selecionado) "#08131A" else "#8B96AC"))
            view.setTypeface(view.typeface, if (selecionado) Typeface.BOLD else Typeface.NORMAL)
        }

        findViewById<android.view.View>(R.id.grupoGeral).visibility = if (aba == Aba.GERAL) android.view.View.VISIBLE else android.view.View.GONE
        findViewById<android.view.View>(R.id.grupoMetas).visibility = if (aba == Aba.METAS) android.view.View.VISIBLE else android.view.View.GONE
        findViewById<android.view.View>(R.id.grupoMedias).visibility = if (aba == Aba.MEDIAS) android.view.View.VISIBLE else android.view.View.GONE
        findViewById<android.view.View>(R.id.grupoPorViagem).visibility = if (aba == Aba.POR_VIAGEM) android.view.View.VISIBLE else android.view.View.GONE

        atualizarConteudo()
    }

    private fun jornadasDoPeriodo(): List<Jornada> {
        return JornadaStorage.listarTodas(this).filter {
            it.dataFimMillis != null && it.dataInicioMillis in dataInicioMillis..dataFimMillis
        }
    }

    private fun atualizarConteudo() {
        val jornadas = jornadasDoPeriodo()
        val stats = jornadas.map { it to JornadaStorage.calcularStats(this, it) }

        when (abaAtual) {
            Aba.GERAL -> montarGeral(jornadas, stats)
            Aba.METAS -> montarMetas(stats)
            Aba.MEDIAS -> montarMedias(jornadas, stats)
            Aba.POR_VIAGEM -> montarPorViagem(jornadas)
        }
    }

    private fun montarGeral(jornadas: List<Jornada>, stats: List<Pair<Jornada, JornadaStats>>) {
        val faturamentoBruto = stats.sumOf { it.second.ganhoBruto }
        val gastos = stats.sumOf { it.second.custoCombustivel + it.second.custoFixo }
        val lucroLiquido = faturamentoBruto - gastos
        val diasTrabalhados = jornadas.map { formatoData.format(Date(it.dataInicioMillis)) }.distinct().size
        val kmRodados = stats.sumOf { it.second.kmRodados }
        val totalMin = stats.sumOf { it.second.tempoTrabalhadoMin }
        val horasTrabalhadas = totalMin / 60.0
        val velocidadeMedia = if (horasTrabalhadas > 0) kmRodados / horasTrabalhadas else 0.0
        val totalViagens = jornadas.sumOf { j ->
            HistoricoStorage.listarEntre(this, j.dataInicioMillis, j.dataFimMillis ?: System.currentTimeMillis())
                .count { it.aceita && !it.cancelada }
        }
        val ganhoPorKm = if (kmRodados > 0) faturamentoBruto / kmRodados else 0.0
        val ganhoPorHora = if (horasTrabalhadas > 0) faturamentoBruto / horasTrabalhadas else 0.0

        findViewById<TextView>(R.id.txtFaturamentoBruto).text = "R$ %.2f".format(faturamentoBruto)
        findViewById<TextView>(R.id.txtLucroLiquido).text = "R$ %.2f".format(lucroLiquido)
        findViewById<TextView>(R.id.txtGastos).text = "R$ %.2f".format(gastos)
        findViewById<TextView>(R.id.txtDiasTrabalhados).text = "$diasTrabalhados"
        findViewById<TextView>(R.id.txtKmRodadosGeral).text = "%.0f".format(kmRodados)
        findViewById<TextView>(R.id.txtHorasTrabalhadasGeral).text = "%02d:%02d".format(totalMin / 60, totalMin % 60)
        findViewById<TextView>(R.id.txtVelocidadeMedia).text = "%.0f km/h".format(velocidadeMedia)
        findViewById<TextView>(R.id.txtTotalViagens).text = "$totalViagens"
        findViewById<TextView>(R.id.txtGanhoPorKm).text = "R$ %.2f".format(ganhoPorKm)
        findViewById<TextView>(R.id.txtGanhoPorHora).text = "R$ %.2f".format(ganhoPorHora)

        val total = gastos + lucroLiquido.coerceAtLeast(0.0)
        val percLucro = if (total > 0) (lucroLiquido.coerceAtLeast(0.0) / total) * 100 else 0.0
        val percGastos = if (total > 0) (gastos / total) * 100 else 0.0

        val barraLucro = findViewById<android.view.View>(R.id.barraLucro)
        val barraGastos = findViewById<android.view.View>(R.id.barraGastos)
        barraLucro.layoutParams = (barraLucro.layoutParams as LinearLayout.LayoutParams).apply { weight = percLucro.toFloat().coerceAtLeast(0.5f) }
        barraGastos.layoutParams = (barraGastos.layoutParams as LinearLayout.LayoutParams).apply { weight = percGastos.toFloat().coerceAtLeast(0.5f) }
        barraLucro.requestLayout()
        barraGastos.requestLayout()

        findViewById<TextView>(R.id.txtLegendaLucro).text = "Lucro %.0f%%".format(percLucro)
        findViewById<TextView>(R.id.txtLegendaGastos).text = "Gastos %.0f%%".format(percGastos)
    }

    private fun montarMetas(stats: List<Pair<Jornada, JornadaStats>>) {
        val container = findViewById<LinearLayout>(R.id.containerJornadas)
        container.removeAllViews()

        if (stats.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Nenhuma jornada concluída nesse período."
                setTextColor(Color.parseColor("#8B96AC"))
                textSize = 13f
            })
            return
        }

        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

        for ((jornada, stat) in stats.sortedByDescending { it.first.dataInicioMillis }) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(this@HistoricoActivity, R.drawable.bg_card_dark)
                setPadding(28, 28, 28, 28)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 }
            }

            val cabecalho = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
            }
            val textoCabecalho = TextView(this).apply {
                text = "${formatoData.format(Date(jornada.dataInicioMillis))} • ${formatoHora.format(Date(jornada.dataInicioMillis))} • %.1f km".format(stat.kmRodados)
                setTextColor(Color.parseColor("#FFFFFF"))
                textSize = 12.5f
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val badgeMeta = TextView(this).apply {
                text = "%.0f%%".format(stat.percentualMeta)
                textSize = 11f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(16, 6, 16, 6)
                if (stat.percentualMeta >= 100) {
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

            val linhaMetricas = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 16, 0, 12) }
            linhaMetricas.addView(criarMiniCard("💲 Ganho", "R$ %.2f".format(stat.ganhoBruto), R.drawable.bg_tint_green, "#1FE7A0"))
            linhaMetricas.addView(criarMiniCard("🕐 R$/h", "R$ %.2f".format(stat.valorPorHora), R.drawable.bg_tint_blue, "#3DB8F5"))
            linhaMetricas.addView(criarMiniCard("💧 Lucro", "R$ %.2f".format(stat.lucroLiquido), R.drawable.bg_tint_green, if (stat.lucroLiquido >= 0) "#1FE7A0" else "#F5576B"))
            card.addView(linhaMetricas)

            val btnExcluir = TextView(this).apply {
                text = "🗑️ Excluir jornada"
                setTextColor(Color.parseColor("#F55757"))
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setOnClickListener {
                    val dialog = AlertDialog.Builder(this@HistoricoActivity, R.style.DialogTemaEscuro)
                        .setTitle("Excluir jornada")
                        .setMessage("Tem certeza que quer excluir esta jornada?")
                        .setPositiveButton("Excluir") { _, _ ->
                            JornadaStorage.apagar(this@HistoricoActivity, jornada.id)
                            atualizarConteudo()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                    DialogUtils.aplicarCoresBotoes(dialog)
                }
            }
            card.addView(btnExcluir)

            container.addView(card)
        }
    }

    private fun montarMedias(jornadas: List<Jornada>, stats: List<Pair<Jornada, JornadaStats>>) {
        val diasTrabalhados = jornadas.map { formatoData.format(Date(it.dataInicioMillis)) }.distinct().size
        val faturamentoBruto = stats.sumOf { it.second.ganhoBruto }
        val kmRodados = stats.sumOf { it.second.kmRodados }
        val totalMin = stats.sumOf { it.second.tempoTrabalhadoMin }
        val horasTrabalhadas = totalMin / 60.0

        val mediaGanhoDia = if (diasTrabalhados > 0) faturamentoBruto / diasTrabalhados else 0.0
        val mediaKmDia = if (diasTrabalhados > 0) kmRodados / diasTrabalhados else 0.0
        val mediaHorasDia = if (diasTrabalhados > 0) horasTrabalhadas / diasTrabalhados else 0.0
        val mediaRPorHora = if (horasTrabalhadas > 0) faturamentoBruto / horasTrabalhadas else 0.0

        findViewById<TextView>(R.id.txtMediaGanhoDia).text = "R$ %.2f".format(mediaGanhoDia)
        findViewById<TextView>(R.id.txtMediaKmDia).text = "%.1f".format(mediaKmDia)
        findViewById<TextView>(R.id.txtMediaHorasDia).text = "%.1f".format(mediaHorasDia)
        findViewById<TextView>(R.id.txtMediaRPorHora).text = "R$ %.2f".format(mediaRPorHora)
    }

    private fun montarPorViagem(jornadas: List<Jornada>) {
        val container = findViewById<LinearLayout>(R.id.containerPorViagem)
        container.removeAllViews()

        val corridas = jornadas.flatMap { j ->
            HistoricoStorage.listarEntre(this, j.dataInicioMillis, j.dataFimMillis ?: System.currentTimeMillis())
                .filter { it.aceita && !it.cancelada }
        }.sortedByDescending { it.dataHora }

        if (corridas.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Nenhuma corrida individual registrada nesse período."
                setTextColor(Color.parseColor("#8B96AC"))
                textSize = 13f
            })
            return
        }

        val formatoDataHora = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

        for (corrida in corridas) {
            val emoji = when (corrida.plataforma) {
                "Uber" -> "⬛"
                "99" -> "🟡"
                else -> "🚗"
            }

            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = ContextCompat.getDrawable(this@HistoricoActivity, R.drawable.bg_card_dark)
                setPadding(20, 16, 20, 16)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 8 }
            }

            linha.addView(TextView(this).apply {
                text = "$emoji ${formatoDataHora.format(Date(corrida.dataHora))} — %.1f km".format(corrida.distanciaTotalKm)
                setTextColor(Color.parseColor("#E4E7EC"))
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            linha.addView(TextView(this).apply {
                text = "R$ %.2f".format(corrida.valorTotal)
                setTextColor(Color.parseColor("#1FE7A0"))
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
            })

            container.addView(linha)
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
}
