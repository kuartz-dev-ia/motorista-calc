package com.motorista.calc

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private enum class Periodo {
        HOJE,
        SETE_DIAS,
        TRINTA_DIAS,
        TUDO
    }

    private var periodoAtual = Periodo.HOJE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dashboard)

        findViewById<TextView>(R.id.tabHoje).setOnClickListener {
            selecionarPeriodo(Periodo.HOJE)
        }

        findViewById<TextView>(R.id.tabSeteDias).setOnClickListener {
            selecionarPeriodo(Periodo.SETE_DIAS)
        }

        findViewById<TextView>(R.id.tabTrintaDias).setOnClickListener {
            selecionarPeriodo(Periodo.TRINTA_DIAS)
        }

        findViewById<TextView>(R.id.tabTudo).setOnClickListener {
            selecionarPeriodo(Periodo.TUDO)
        }

        selecionarPeriodo(Periodo.HOJE)
    }

    override fun onResume() {
        super.onResume()
        atualizarDashboard()
    }

    private fun selecionarPeriodo(periodo: Periodo) {
        periodoAtual = periodo
        atualizarTabs()
        atualizarDashboard()
    }

    private fun atualizarTabs() {

        val tabs = mapOf(
            Periodo.HOJE to R.id.tabHoje,
            Periodo.SETE_DIAS to R.id.tabSeteDias,
            Periodo.TRINTA_DIAS to R.id.tabTrintaDias,
            Periodo.TUDO to R.id.tabTudo
        )

        for ((periodo, id) in tabs) {

            val view = findViewById<TextView>(id)

            val selecionado = periodo == periodoAtual

            view.background = ContextCompat.getDrawable(
                this,
                if (selecionado) {
                    R.drawable.bg_chip_selected
                } else {
                    R.drawable.bg_chip_unselected
                }
            )

            view.setTextColor(
                Color.parseColor(
                    if (selecionado) {
                        "#08131A"
                    } else {
                        "#8B96AC"
                    }
                )
            )

            view.setTypeface(
                view.typeface,
                if (selecionado) {
                    Typeface.BOLD
                } else {
                    Typeface.NORMAL
                }
            )
        }
    }

    private fun buscarCorridas(): List<RegistroCorrida> {

        return when (periodoAtual) {

            Periodo.HOJE ->
                HistoricoStorage.listarDoDia(this)

            Periodo.SETE_DIAS ->
                HistoricoStorage.listarUltimosDias(this, 7)

            Periodo.TRINTA_DIAS ->
                HistoricoStorage.listarUltimosDias(this, 30)

            Periodo.TUDO ->
                HistoricoStorage.listarEntre(
                    this,
                    0L,
                    System.currentTimeMillis()
                )
        }
    }

    private fun atualizarDashboard() {

        val corridas = buscarCorridas()

        val faturamento =
            corridas.sumOf { it.valorTotal }

        val custo =
            corridas.sumOf {
                (it.valorTotal - (it.lucroLiquido ?: it.valorTotal))
                    .coerceAtLeast(0.0)
            }

        val lucro =
            corridas.sumOf {
                it.lucroLiquido ?: 0.0
            }

        val quantidade =
            corridas.size

        val aceitas =
            corridas.count {
                it.aceita && !it.cancelada
            }

        val recusadas =
            corridas.count {
                !it.aceita
            }

        val kmTotal =
            corridas.sumOf {
                it.distanciaTotalKm
            }

        val tempoTotalMin =
            corridas.sumOf {
                it.tempoTotalMin
            }

        val horas =
            tempoTotalMin / 60.0

        val mediaKm =
            if (kmTotal > 0) {
                faturamento / kmTotal
            } else {
                0.0
            }

        val mediaHora =
            if (horas > 0) {
                faturamento / horas
            } else {
                0.0
            }

        val lucroMedio =
            if (quantidade > 0) {
                lucro / quantidade
            } else {
                0.0
            }

        val taxaAceitacao =
            if (quantidade > 0) {
                (aceitas.toDouble() / quantidade.toDouble()) * 100.0
            } else {
                0.0
            }

        findViewById<TextView>(R.id.txtFaturamento).text =
            formatarMoeda(faturamento)

        findViewById<TextView>(R.id.txtLucro).text =
            formatarMoeda(lucro)

        findViewById<TextView>(R.id.txtCusto).text =
            formatarMoeda(custo)

        findViewById<TextView>(R.id.txtCorridas).text =
            quantidade.toString()

        findViewById<TextView>(R.id.txtAceitas).text =
            aceitas.toString()

        findViewById<TextView>(R.id.txtRecusadas).text =
            recusadas.toString()

        findViewById<TextView>(R.id.txtKm).text =
            "%.1f km".format(
                Locale.getDefault(),
                kmTotal
            )

        findViewById<TextView>(R.id.txtRPorKm).text =
            formatarMoeda(mediaKm)

        findViewById<TextView>(R.id.txtRPorHora).text =
            formatarMoeda(mediaHora)

        findViewById<TextView>(R.id.txtLucroMedio).text =
            formatarMoeda(lucroMedio)

        findViewById<TextView>(R.id.txtTaxaAceitacao).text =
            "%.0f%%".format(
                Locale.getDefault(),
                taxaAceitacao
            )

        montarMelhorCorrida(corridas)
        montarPiorCorrida(corridas)
        montarPlataformas(corridas)

        val vazio =
            findViewById<TextView>(R.id.txtSemDados)

        vazio.visibility =
            if (corridas.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun montarMelhorCorrida(
        corridas: List<RegistroCorrida>
    ) {

        val melhor =
            corridas.maxByOrNull {
                it.lucroLiquido ?: 0.0
            }

        val texto =
            findViewById<TextView>(R.id.txtMelhorCorrida)

        if (melhor == null) {

            texto.text = "Nenhuma corrida registrada."

            return
        }

        texto.text =
            "R$ %.2f • %.1f km • R$ %.2f/km".format(
                Locale.getDefault(),
                melhor.valorTotal,
                melhor.distanciaTotalKm,
                melhor.valorPorKm ?: 0.0
            )
    }

    private fun montarPiorCorrida(
        corridas: List<RegistroCorrida>
    ) {

        val pior =
            corridas.minByOrNull {
                it.lucroLiquido ?: Double.MAX_VALUE
            }

        val texto =
            findViewById<TextView>(R.id.txtPiorCorrida)

        if (pior == null) {

            texto.text = "Nenhuma corrida registrada."

            return
        }

        texto.text =
            "R$ %.2f • %.1f km • R$ %.2f/km".format(
                Locale.getDefault(),
                pior.valorTotal,
                pior.distanciaTotalKm,
                pior.valorPorKm ?: 0.0
            )
    }

    private fun montarPlataformas(
        corridas: List<RegistroCorrida>
    ) {

        val container =
            findViewById<LinearLayout>(
                R.id.containerPlataformas
            )

        container.removeAllViews()

        if (corridas.isEmpty()) {
            return
        }

        val grupos =
            corridas
                .groupBy {
                    it.plataforma
                }
                .mapValues {
                    it.value.sumOf { corrida ->
                        corrida.valorTotal
                    }
                }
                .toList()
                .sortedByDescending {
                    it.second
                }

        for ((plataforma, valor) in grupos) {

            val quantidade =
                corridas.count {
                    it.plataforma == plataforma
                }

            val linha =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    gravity =
                        android.view.Gravity.CENTER_VERTICAL

                    background =
                        ContextCompat.getDrawable(
                            this@DashboardActivity,
                            R.drawable.bg_card_dark
                        )

                    setPadding(
                        dp(14),
                        dp(12),
                        dp(14),
                        dp(12)
                    )

                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = dp(8)
                        }
                }

            val nome =
                TextView(this).apply {

                    text =
                        "$plataforma  •  $quantidade corrida(s)"

                    setTextColor(
                        Color.parseColor("#FFFFFF")
                    )

                    textSize = 12f

                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                }

            val total =
                TextView(this).apply {

                    text =
                        formatarMoeda(valor)

                    setTextColor(
                        Color.parseColor("#1FE7A0")
                    )

                    textSize = 13f

                    setTypeface(
                        typeface,
                        Typeface.BOLD
                    )
                }

            linha.addView(nome)
            linha.addView(total)

            container.addView(linha)
        }
    }

    private fun formatarMoeda(
        valor: Double
    ): String {

        return "R$ %.2f".format(
            Locale.getDefault(),
            valor
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
