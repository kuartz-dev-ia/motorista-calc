package com.motorista.calc

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingsActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_recordings)
            container = findViewById(R.id.containerGravacoes)
        } catch (e: Exception) {
            mostrarErroNaTela("Erro ao abrir a tela: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            atualizarLista()
        } catch (e: Exception) {
            mostrarErroNaTela("Erro ao listar gravações: ${e.message}")
        }
    }

    private fun mostrarErroNaTela(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
        try {
            val layoutSeguranca = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 80, 40, 40)
                setBackgroundColor(Color.parseColor("#0E1013"))
            }
            layoutSeguranca.addView(TextView(this).apply {
                text = "Ocorreu um erro nesta tela:\n\n$mensagem"
                setTextColor(Color.parseColor("#F7C1C1"))
                textSize = 13f
            })
            setContentView(layoutSeguranca)
        } catch (e: Exception) {
            // Se nem isso funcionar, ao menos o Toast já apareceu.
        }
    }

    private fun uriDoArquivo(file: File): Uri =
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

    private fun abrirVideo(file: File) {
        try {
            val uri = uriDoArquivo(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Abrir gravação"))
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao abrir vídeo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun compartilharVideo(file: File) {
        try {
            val uri = uriDoArquivo(file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Enviar gravação (Drive, Gmail, etc.)"))
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao compartilhar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun atualizarLista() {
        container.removeAllViews()
        val arquivos = RecordingsStorage.listarTodos(this)

        if (arquivos.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Nenhuma gravação salva ainda."
                setTextColor(Color.parseColor("#9AA4B2"))
                textSize = 14f
            })
            return
        }

        val porDia = arquivos.groupBy { RecordingsStorage.dataDoArquivo(it) }
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
                    try {
                        RecordingsStorage.apagarTodosDoDia(this@RecordingsActivity, dataChave)
                        atualizarLista()
                    } catch (e: Exception) {
                        Toast.makeText(this@RecordingsActivity, "Erro ao apagar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
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

                val txtHora = TextView(this).apply {
                    text = try {
                        "🎬 " + formatoHora.format(Date(arquivo.lastModified())) + "  (%.1f MB)".format(arquivo.length() / 1024.0 / 1024.0)
                    } catch (e: Exception) {
                        "🎬 gravação"
                    }
                    textSize = 12f
                    setTextColor(Color.parseColor("#E4E7EC"))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener { abrirVideo(arquivo) }
                }

                val btnCompartilhar = Button(this).apply {
                    text = "☁️ Enviar"
                    textSize = 11f
                    setOnClickListener { compartilharVideo(arquivo) }
                }

                val btnApagar = Button(this).apply {
                    text = "Apagar"
                    textSize = 11f
                    setOnClickListener {
                        try {
                            RecordingsStorage.apagar(arquivo)
                            atualizarLista()
                        } catch (e: Exception) {
                            Toast.makeText(this@RecordingsActivity, "Erro ao apagar: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                linha.addView(txtHora)
                linha.addView(btnCompartilhar)
                linha.addView(btnApagar)
                container.addView(linha)
            }
        }
    }
}
