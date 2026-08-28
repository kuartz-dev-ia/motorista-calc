package com.motorista.calc

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoricoActivity : AppCompatActivity() {

    private lateinit var containerBoas: LinearLayout
    private lateinit var containerAceitas: LinearLayout
    private lateinit var txtResumoBoas: TextView
    private lateinit var txtResumoAceitas: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historico)

        txtResumoBoas = findViewById(R.id.txtResumoBoas)
        txtResumoAceitas = findViewById(R.id.txtResumoAceitas)
        containerBoas = findViewById(R.id.containerBoas)
        containerAceitas = findViewById(R.id.containerAceitas)
    }

    override fun onResume() {
        super.onResume()
        atualizarTela()
    }

    private fun atualizarTela() {
        val registros = HistoricoStorage.listarDoDia(this)
        val boas = registros.filter { it.valeAPena }
        val aceitas = registros.filter { it.aceita && !it.cancelada }

        txtResumoBoas.text = montarResumo(boas)
        renderizarLista(
            container = containerBoas,
            lista = boas,
            rotuloBotao = "Aceitei",
            mostrarBotao = { registro -> !registro.aceita },
            acaoBotao = { registro ->
                HistoricoStorage.marcarAceita(this, registro.id)
                atualizarTela()
            }
        )

        txtResumoAceitas.text = montarResumo(aceitas)
        renderizarLista(
            container = containerAceitas,
            lista = aceitas,
            rotuloBotao = "Cancelei",
            mostrarBotao = { true },
            acaoBotao = { registro ->
                HistoricoStorage.marcarCancelada(this, registro.id)
                atualizarTela()
            }
        )
    }

    private fun montarResumo(lista: List<RegistroCorrida>): String {
        if (lista.isEmpty()) return "Nenhuma corrida registrada hoje ainda."

        val totalGanho = lista.sumOf { it.valorTotal }
        val totalKm = lista.sumOf { it.distanciaTotalKm }
        val totalMin = lista.sumOf { it.tempoTotalMin }
        val horas = totalMin / 60.0
        val mediaPorKm = if (totalKm > 0) totalGanho / totalKm else null
        val mediaPorHora = if (horas > 0) totalGanho / horas else null

        return buildString {
            append("Corridas: ${lista.size}\n")
            append("Ganho total: R$ %.2f\n".format(totalGanho))
            append("Km total: %.1f km\n".format(totalKm))
            append("Tempo total: %.1f h\n".format(horas))
            mediaPorKm?.let { append("Média R$/km: %.2f\n".format(it)) }
            mediaPorHora?.let { append("Média R$/hora: %.2f\n".format(it)) }
        }
    }

    private fun renderizarLista(
        container: LinearLayout,
        lista: List<RegistroCorrida>,
        rotuloBotao: String,
        mostrarBotao: (RegistroCorrida) -> Boolean,
        acaoBotao: (RegistroCorrida) -> Unit
    ) {
        container.removeAllViews()
        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

        if (lista.isEmpty()) return

        for (registro in lista.sortedByDescending { it.dataHora }) {
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 12, 0, 12)
                gravity = Gravity.CENTER_VERTICAL
            }

            val texto = TextView(this).apply {
                text = buildString {
                    append(formatoHora.format(Date(registro.dataHora)))
                    append(" — R$ %.2f".format(registro.valorTotal))
                    append(" (%.1f km)".format(registro.distanciaTotalKm))
                    registro.valorPorKm?.let { append(" — R$/km %.2f".format(it)) }
                }
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            linha.addView(texto)

            if (mostrarBotao(registro)) {
                val btnAcao = Button(this).apply {
                    text = rotuloBotao
                    textSize = 11f
                    setOnClickListener { acaoBotao(registro) }
                }
                linha.addView(btnAcao)
            }

            container.addView(linha)
        }
    }
}
