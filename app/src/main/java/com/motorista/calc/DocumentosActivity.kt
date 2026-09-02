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

class DocumentosActivity : AppCompatActivity() {

    private lateinit var edtTipo: EditText
    private lateinit var edtData: EditText
    private lateinit var containerPendencias: LinearLayout
    private lateinit var containerLista: LinearLayout

    private val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documentos)

        edtTipo = findViewById(R.id.edtTipoDocumento)
        edtData = findViewById(R.id.edtDataVencimento)
        containerPendencias = findViewById(R.id.containerPendenciasDocumentos)
        containerLista = findViewById(R.id.containerListaDocumentos)

        findViewById<android.view.View>(R.id.btnSalvarDocumento).setOnClickListener {
            salvarDocumento()
        }
    }

    override fun onResume() {
        super.onResume()
        atualizarTela()
    }

    private fun salvarDocumento() {
        val tipo = edtTipo.text.toString().trim()
        val dataTexto = edtData.text.toString().trim()

        if (tipo.isBlank()) {
            Toast.makeText(this, "Informe o tipo do documento", Toast.LENGTH_LONG).show()
            return
        }

        val dataVencimento = try {
            formatoData.isLenient = false
            formatoData.parse(dataTexto)?.time
        } catch (e: Exception) {
            null
        }

        if (dataVencimento == null) {
            Toast.makeText(this, "Data inválida. Use o formato dd/mm/aaaa", Toast.LENGTH_LONG).show()
            return
        }

        DocumentoStorage.adicionar(this, tipo, dataVencimento)
        edtTipo.text.clear()
        edtData.text.clear()
        Toast.makeText(this, "Documento salvo", Toast.LENGTH_SHORT).show()
        atualizarTela()
    }

    private fun atualizarTela() {
        val pendencias = DocumentoStorage.pendencias(this)
        containerPendencias.removeAllViews()

        if (pendencias.isEmpty()) {
            containerPendencias.addView(TextView(this).apply {
                text = "✅ Nenhum documento vencendo em breve."
                setTextColor(Color.parseColor("#8FB868"))
                textSize = 13f
            })
        } else {
            for (p in pendencias.sortedBy { it.dataVencimentoMillis }) {
                val dias = DocumentoStorage.diasRestantes(p)
                val texto = if (dias < 0) {
                    "⚠️ ${p.tipo} — VENCIDO há ${-dias} dia(s)"
                } else {
                    "⚠️ ${p.tipo} — vence em $dias dia(s)"
                }
                containerPendencias.addView(TextView(this).apply {
                    text = texto
                    setTextColor(Color.parseColor("#F7C1C1"))
                    textSize = 13f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0, 4, 0, 4)
                })
            }
        }

        val todos = DocumentoStorage.listarTodos(this)
        containerLista.removeAllViews()

        if (todos.isEmpty()) {
            containerLista.addView(TextView(this).apply {
                text = "Nenhum documento cadastrado ainda."
                setTextColor(Color.parseColor("#9AA4B2"))
                textSize = 13f
            })
            return
        }

        for (d in todos.sortedBy { it.dataVencimentoMillis }) {
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 10, 0, 10)
            }

            val texto = TextView(this).apply {
                text = "${d.tipo} — vence em ${formatoData.format(Date(d.dataVencimentoMillis))}"
                textSize = 12f
                setTextColor(Color.parseColor("#E4E7EC"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            linha.addView(texto)

            val btnApagar = Button(this).apply {
                text = "🗑️"
                textSize = 12f
                setOnClickListener {
                    DocumentoStorage.apagar(this@DocumentosActivity, d.id)
                    atualizarTela()
                }
            }
            linha.addView(btnApagar)

            containerLista.addView(linha)
        }
    }
}
