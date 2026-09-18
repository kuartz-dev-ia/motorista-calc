package com.motorista.calc

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class EventosActivity : AppCompatActivity() {

    private val CODIGO_ESCOLHER_IMAGEM = 701

    private lateinit var edtNome: EditText
    private lateinit var edtEndereco: EditText
    private lateinit var edtData: EditText
    private lateinit var edtHorarioInicio: EditText
    private lateinit var edtHorarioFim: EditText
    private lateinit var txtTextoExtraido: TextView
    private lateinit var containerLista: LinearLayout
    private var categoriaSelecionada = "Show/Festival"

    private val chips by lazy {
        listOf(
            findViewById<TextView>(R.id.chipShow) to "Show/Festival",
            findViewById<TextView>(R.id.chipJogo) to "Jogo",
            findViewById<TextView>(R.id.chipOutroEvento) to "Outro"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eventos)

        edtNome = findViewById(R.id.edtNomeEvento)
        edtEndereco = findViewById(R.id.edtEnderecoEvento)
        edtData = findViewById(R.id.edtDataEvento)
        edtHorarioInicio = findViewById(R.id.edtHorarioInicioEvento)
        edtHorarioFim = findViewById(R.id.edtHorarioFimEvento)
        txtTextoExtraido = findViewById(R.id.txtTextoExtraido)
        containerLista = findViewById(R.id.containerEventos)

        for ((chip, categoria) in chips) {
            chip.setOnClickListener { selecionarCategoria(chip, categoria) }
        }
        selecionarCategoria(findViewById(R.id.chipShow), "Show/Festival")

        findViewById<TextView>(R.id.btnExtrairImagem).setOnClickListener { escolherImagem() }
        findViewById<TextView>(R.id.btnSalvarEvento).setOnClickListener { salvar() }
    }

    override fun onResume() {
        super.onResume()
        atualizarLista()
    }

    private fun selecionarCategoria(selecionado: TextView, categoria: String) {
        categoriaSelecionada = categoria
        for ((chip, _) in chips) {
            chip.background = ContextCompat.getDrawable(this, if (chip == selecionado) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            chip.setTextColor(if (chip == selecionado) Color.parseColor("#08131A") else Color.parseColor("#8B96AC"))
        }
    }

    private fun escolherImagem() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        startActivityForResult(intent, CODIGO_ESCOLHER_IMAGEM)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CODIGO_ESCOLHER_IMAGEM && resultCode == Activity.RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try {
                val stream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(stream)
                stream?.close()
                if (bitmap == null) {
                    Toast.makeText(this, "Não consegui ler a imagem", Toast.LENGTH_LONG).show()
                    return
                }
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        txtTextoExtraido.text = visionText.text.ifBlank { "Nenhum texto encontrado na imagem." }
                        txtTextoExtraido.visibility = android.view.View.VISIBLE
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao ler o texto da imagem: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao processar imagem: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun salvar() {
        val nome = edtNome.text.toString().trim()
        val endereco = edtEndereco.text.toString().trim()

        if (nome.isBlank() || endereco.isBlank()) {
            Toast.makeText(this, "Preencha pelo menos o nome e o endereço", Toast.LENGTH_LONG).show()
            return
        }

        EventoStorage.adicionar(
            this, nome, endereco, categoriaSelecionada,
            edtData.text.toString().trim(),
            edtHorarioInicio.text.toString().trim(),
            edtHorarioFim.text.toString().trim()
        )

        edtNome.text.clear()
        edtEndereco.text.clear()
        edtData.text.clear()
        edtHorarioInicio.text.clear()
        edtHorarioFim.text.clear()
        txtTextoExtraido.visibility = android.view.View.GONE

        Toast.makeText(this, "Evento salvo", Toast.LENGTH_SHORT).show()
        atualizarLista()
    }

    private fun abrirNoMapa(endereco: String) {
        try {
            val uri = Uri.parse("geo:0,0?q=" + Uri.encode(endereco))
            val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val uri = Uri.parse("geo:0,0?q=" + Uri.encode(endereco))
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (e2: Exception) {
                Toast.makeText(this, "Não encontrei um app de mapas instalado", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun atualizarLista() {
        val eventos = EventoStorage.listarTodos(this)
        containerLista.removeAllViews()

        if (eventos.isEmpty()) {
            containerLista.addView(TextView(this).apply {
                text = "Nenhum evento cadastrado ainda."
                setTextColor(Color.parseColor("#8B96AC"))
                textSize = 13f
            })
            return
        }

        val emojiCategoria = mapOf("Show/Festival" to "🎤", "Jogo" to "⚽", "Outro" to "📍")

        for (evento in eventos) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(this@EventosActivity, R.drawable.bg_card_dark)
                setPadding(24, 20, 24, 20)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 12
                }
            }

            card.addView(TextView(this).apply {
                text = "${emojiCategoria[evento.categoria] ?: "📍"} ${evento.nome}"
                setTextColor(Color.WHITE)
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })

            card.addView(TextView(this).apply {
                text = evento.endereco
                setTextColor(Color.parseColor("#8B96AC"))
                textSize = 11.5f
                setPadding(0, 4, 0, 4)
            })

            val infoHorario = listOfNotNull(
                evento.dataTexto.ifBlank { null },
                evento.horarioInicio.ifBlank { null }?.let { "início $it" },
                evento.horarioFimEstimado.ifBlank { null }?.let { "término (previsto) $it" }
            ).joinToString("  •  ")

            if (infoHorario.isNotBlank()) {
                card.addView(TextView(this).apply {
                    text = infoHorario
                    setTextColor(Color.parseColor("#1FE7C4"))
                    textSize = 11.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0, 0, 0, 10)
                })
            }

            val linhaBotoes = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            linhaBotoes.addView(TextView(this).apply {
                text = "🗺️ Ver no mapa"
                setTextColor(Color.parseColor("#3DB8F5"))
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { abrirNoMapa(evento.endereco) }
            })

            linhaBotoes.addView(TextView(this).apply {
                text = "🗑️"
                textSize = 14f
                setOnClickListener {
                    EventoStorage.apagar(this@EventosActivity, evento.id)
                    atualizarLista()
                }
            })

            card.addView(linhaBotoes)
            containerLista.addView(card)
        }
    }
}
