package com.motorista.calc

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ManutencoesActivity : AppCompatActivity() {

    private lateinit var edtTipo: android.widget.EditText
    private lateinit var edtKm: android.widget.EditText
    private lateinit var edtCusto: android.widget.EditText
    private lateinit var edtIntervaloKm: android.widget.EditText
    private lateinit var edtIntervaloDias: android.widget.EditText
    private lateinit var containerPendencias: LinearLayout
    private lateinit var containerHistorico: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_manutencoes)

            edtTipo = findViewById(R.id.edtTipoManutencao)
            edtKm = findViewById(R.id.edtKmManutencao)
            edtCusto = findViewById(R.id.edtCustoManutencao)
            edtIntervaloKm = findViewById(R.id.edtIntervaloKm)
            edtIntervaloDias = findViewById(R.id.edtIntervaloDias)
            containerPendencias = findViewById(R.id.containerPendencias)
            containerHistorico = findViewById(R.id.containerHistoricoManutencoes)

            findViewById<android.view.View>(R.id.btnSalvarManutencao).setOnClickListener { salvarManutencao() }
        } catch (e: Exception) {
            mostrarErro("Erro ao abrir a tela: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            atualizarTela()
        } catch (e: Exception) {
            mostrarErro("Erro ao carregar manutenções: ${e.message}")
        }
    }

    private fun mostrarErro(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
        try {
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 80, 40, 40)
                setBackgroundColor(Color.parseColor("#070C14"))
            }
            layout.addView(TextView(this).apply {
                text = "Ocorreu um erro nesta tela:\n\n$mensagem"
                setTextColor(Color.parseColor("#F5576B"))
                textSize = 13f
            })
            setContentView(layout)
        } catch (e: Exception) { }
    }

    private fun salvarManutencao() {
        try {
            val tipo = edtTipo.text.toString().trim()
            val km = edtKm.text.toString().toDoubleOrNull()
            val custo = edtCusto.text.toString().toDoubleOrNull()
            val intervaloKm = edtIntervaloKm.text.toString().toDoubleOrNull()
            val intervaloDias = edtIntervaloDias.text.toString().toIntOrNull()

            if (tipo.isBlank() || km == null || km <= 0 || custo == null || custo < 0) {
                Toast.makeText(this, "Preencha tipo, km e custo corretamente", Toast.LENGTH_LONG).show()
                return
            }

            ManutencaoStorage.adicionar(this, tipo, km, custo, intervaloKm, intervaloDias)
            edtTipo.text.clear()
            edtKm.text.clear()
            edtCusto.text.clear()
            edtIntervaloKm.text.clear()
            edtIntervaloDias.text.clear()
            Toast.makeText(this, "Manutenção salva", Toast.LENGTH_SHORT).show()
            atualizarTela()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun atualizarTela() {
        val pendencias = try { ManutencaoStorage.pendencias(this) } catch (e: Exception) { emptyList() }
        containerPendencias.removeAllViews()

        if (pendencias.isEmpty()) {
            containerPendencias.addView(TextView(this).apply {
                text = "✅ Nenhuma manutenção pendente no momento."
                setTextColor(Color.parseColor("#1FE7A0"))
                textSize = 13f
            })
        } else {
            for (p in pendencias) {
                containerPendencias.addView(TextView(this).apply {
                    text = "⚠️ ${p.tipo} — hora de revisar de novo"
                    setTextColor(Color.parseColor("#F5576B"))
                    textSize = 13f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0, 4, 0, 4)
                })
            }
        }

        val historico = try { ManutencaoStorage.listarTodos(this) } catch (e: Exception) { emptyList() }
        containerHistorico.removeAllViews()

        if (historico.isEmpty()) {
            containerHistorico.addView(TextView(this).apply {
                text = "Nenhuma manutenção registrada ainda."
                setTextColor(Color.parseColor("#8B96AC"))
                textSize = 13f
            })
            return
        }

        val formato = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())

        for (m in historico) {
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 10, 0, 10)
            }

            val texto = TextView(this).apply {
                text = buildString {
                    append("${m.tipo} — ${formato.format(java.util.Date(m.dataHora))}")
                    append("\n%.0f km — R$ %.2f".format(m.kmRegistrado, m.custo))
                    if (m.proximaKm != null) append("\nPróxima: %.0f km".format(m.proximaKm))
                    if (m.proximaDataMillis != null) append("\nPróxima: ${formato.format(java.util.Date(m.proximaDataMillis))}")
                }
                textSize = 12f
                setTextColor(Color.parseColor("#E4E7EC"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            linha.addView(texto)

            val btnApagar = TextView(this).apply {
                text = "🗑️"
                textSize = 16f
                setOnClickListener {
                    try {
                        ManutencaoStorage.apagar(this@ManutencoesActivity, m.id)
                        atualizarTela()
                    } catch (e: Exception) {
                        Toast.makeText(this@ManutencoesActivity, "Erro ao apagar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            linha.addView(btnApagar)

            containerHistorico.addView(linha)
        }
    }
}
