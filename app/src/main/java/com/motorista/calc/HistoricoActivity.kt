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

        val dialog = DatePickerDialog(
            this,
            R.style.DialogTemaEscuro,
            { _, ano, mes, dia ->
                val novaData = Calendar.getInstance().apply { set(ano, mes, dia, if (ehInicio) 0 else 23, if (ehInicio) 0 else 59, if (ehInicio) 0 else 59) }
                if (ehInicio) dataInicioMillis = novaData.timeInMillis else dataFimMillis = novaData.timeInMillis
                atualizarTextoDatas()
                atualizarConteudo()
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.setOnShowListener { DialogUtils.aplicarTemaCompleto(dialog) }
        dialog.show()
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
        findViewById<TextView>(R.id.txtLegendaGastos).text = "Gast
