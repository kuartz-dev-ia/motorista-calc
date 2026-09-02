package com.motorista.calc

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
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

    private fun uriDoArquivo(file: File): Uri =
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

    private fun abrirImagem(file: File) {
        try {
            val uri = uriDoArquivo(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/png")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Abrir print"))
        } catch (e: Exception) { }
    }

    private fun compartilharImagem(file: File) {
        try {
            val uri = uriDoArquivo(file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Compartilhar print"))
        } catch (e: Exception) { }
    }

    private fun atualizarLista() {
        container.removeAllViews()
        val arquivos = PrintsStorage.listarTodos(this)

        if (arquivos.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Nenhum print salvo ainda."
                setTextColor(Color.parseColor("#9AA4B2"))
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
                setTextColor(Color.parseColor("#F4F5F7"))
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
                    setPadding(16, 12, 16, 12)
                    background = androidx.core.content.ContextCompat.getDrawable(this@PrintsActivity, R.drawable.bg_card_dark)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = 10
                    }
                }

                val thumb = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(120, 120).apply { marginEnd = 16 }
                    try {
                        val opcoes = BitmapFactory.Options().apply { inSampleSize = 6 }
                        setImageBitmap(BitmapFactory.decodeFile(arquivo.absolutePath, opcoes))
                    } catch (e: Exception) { }
                    setOnClickListener { abrirImagem(arquivo) }
                }

                val txtHora = TextView(this).apply {
                    text = formatoHora.format(Date(arquivo.lastModified()))
                    textSize = 12f
                    setTextColor(Color.parseColor("#E4E7EC"))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val btnCompartilhar = Button(this).apply {
                    text = "☁️"
                    textSize = 13f
                    setOnClickListener { compartilharImagem(arquivo) }
                }

                val btnApagar = Button(this).apply {
                    text = "🗑️"
                    textSize = 13f
                    setOnClickListener {
                        PrintsStorage.apagar(arquivo)
                        atualizarLista()
                    }
                }

                linha.addView(thumb)
                linha.addView(txtHora)
                linha.addView(btnCompartilhar)
                linha.addView(btnApagar)
                container.addView(linha)
            }
        }
    }
}
