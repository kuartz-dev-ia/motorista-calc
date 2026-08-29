package com.motorista.calc

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrintsActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prints)
        container = findViewById(R.id.containerPrints)
    }

    override fun onResume() {
        super.onResume()
        atualizarLista()
    }

    private fun atualizarLista() {
        container.removeAllViews()
        val arquivos = PrintsStorage.listarTodos(this)

        if (arquivos.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Nenhum print salvo ainda."
                textSize = 14f
            })
            return
        }

        val porDia = arquivos.groupBy { PrintsStorage.dataDoArquivo(it) }
        val formatoHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        for ((dataChave, listaDoDia) in porDia) {
            val cabecalho = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 24, 0, 8)
            }
            val txtData = TextView(this).apply {
                text = "📅 $dataChave (${listaDoDia.size})"
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val btnApagarDia = Button(this).apply {
                text = "Apagar dia"
                textSize = 11f
                setOnClickListener {
                    PrintsStorage.apagarTodosDoDia(this@PrintsActivity, dataChave)
                    atualizarLista()
                }
            }
            cabecalho.addView(txtData)
            cabecalho.addView(btnApagarDia)
            container.addView(cabecalho)

            for (arquivo in listaDoDia.sortedByDescending { it.lastModified() }) {
                val linha = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, 8, 0, 8)
                }

                val thumb = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(140, 140)
                    try {
                        val opcoes = BitmapFactory.Options().apply { inSampleSize = 6 }
                        setImageBitmap(BitmapFactory.decodeFile(arquivo.absolutePath, opcoes))
                    } catch (e: Exception) {
                        // ignora falha ao gerar miniatura
                    }
                }

                val txtHora = TextView(this).apply {
                    text = formatoHora.format(Date(arquivo.lastModified()))
                    textSize = 12f
                    setPadding(16, 0, 16, 0)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val btnApagar = Button(this).apply {
                    text = "Apagar"
                    textSize = 11f
                    setOnClickListener {
                        PrintsStorage.apagar(arquivo)
                        atualizarLista()
                    }
                }

                linha.addView(thumb)
                linha.addView(txtHora)
                linha.addView(btnApagar)
                container.addView(linha)
            }
        }
    }
}
