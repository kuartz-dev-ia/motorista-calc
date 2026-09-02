package com.motorista.calc

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManutencoesActivity : AppCompatActivity() {

    private lateinit var edtTipo: EditText
    private lateinit var edtKm: EditText
    private lateinit var edtCusto: EditText
    private lateinit var edtIntervaloKm: EditText
    private lateinit var edtIntervaloDias: EditText
    private lateinit var containerPendencias: LinearLayout
    private lateinit var containerHistorico: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manutencoes)

        edtTipo = findViewById(R.id.edtTipoManutencao)
        edtKm = findViewById(R.id.edtKmManutencao)
        edtCusto = findViewById(R.id.edtCustoManutencao)
        edtIntervaloKm = findViewById(R.id.edtIntervaloKm)
        edtIntervaloDias = findViewById(R.id.edtIntervaloDias)
        containerPendencias = findViewById(R.id.containerPendencias)
        containerHistorico = findViewById(R.id.containerHistoricoManutencoes)

        findViewById<android.view.View>(R.id.btnSalvarManutencao).setOnClickListener {
            salvarManutencao()
        }
    }

    override fun onResume() {
        super.onResume()
        atualizarTela()
    }

    private fun salvarManutencao() {
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
    }

    private fun atualizarTela() {
        val pendencias = ManutencaoStorage.pendencias(this)
        containerPendencias.removeAllViews()

        if (pendencias.isEmpty()) {
            containerPendencias.addView(TextView(this).apply {
                text = "✅ Nenhuma manutenção pendente no momento."
                setTextColor(Color.parseColor("#8FB868"))
                textSize = 13f
            })
        } else {
            for (p in pendencias) {
                containerPendencias.addView(TextView(this).apply {
                    text = "⚠️ ${p.tipo} — hora de revisar de novo"
                    setTextColor(Color.parseColor("#F7C1C1"))
                    textSize = 13f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0, 4, 0, 4)
                })
            }
        }

        val historico = ManutencaoStorage.listarTodos(this)
        containerHistorico.removeAllViews()

        if (historico.isEmpty()) {
            containerHistorico.addView(TextView(this).apply {
                text = "Nenhuma manutenção registrada ainda."
                setTextColor(Color.parseColor("#9AA4B2"))
                textSize = 13f
            })
            return
        }

        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        for (m in historico) {
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 10, 0, 10)
            }

            val texto = TextView(this).apply {
                text = buildString {
                    append("${m.tipo} — ${formato.format(Date(m.dataHora))}")
                    append("\n%.0f km — R$ %.2f".format(m.kmRegistrado, m.custo))
                    if (m.proximaKm != null) append("\nPróxima: %.0f km".format(m.proximaKm))
                    if (m.proximaDataMillis != null) append("\nPróxima: ${formato.format(Date(m.proximaDataMillis))}")
                }
                textSize = 12f
                setTextColor(Color.parseColor("#E4E7EC"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            linha.addView(texto)

            val btnApagar = Button(this).apply {
                text = "🗑️"
                textSize = 12f
                setOnClickListener {
                    ManutencaoStorage.apagar(this@ManutencoesActivity, m.id)
                    atualizarTela()
                }
            }
            linha.addView(btnApagar)

            containerHistorico.addView(linha)
        }
    }
}
