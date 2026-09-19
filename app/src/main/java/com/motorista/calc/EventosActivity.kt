package com.motorista.calc

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
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

        for ((chip, categoria) in chips) {
            chip.setOnClickListener { selecionarCategoria(chip, categoria) }
        }
        selecionarCategoria(findViewById(R.id.chipShow), "Show/Festival")

        findViewById<TextView>(R.id.btnExtrairImagem).setOnClickListener { escolherImagem() }
        findViewById<TextView>(R.id.btnSalvarEvento).setOnClickListener { salvar() }
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
                        val texto = visionText.text
                        if (texto.isBlank()) {
                            Toast.makeText(this, "Nenhum texto encontrado na imagem", Toast.LENGTH_LONG).show()
                            return@addOnSuccessListener
                        }
                        val endereco = EventoTextParser.extrairEndereco(texto)
                        val nome = EventoTextParser.extrairNome(texto, endereco)
                        val data2 = EventoTextParser.extrairData(texto)
                        val horario = EventoTextParser.extrairHorarioInicio(texto)

                        nome?.let { edtNome.setText(it) }
                        endereco?.let { edtEndereco.setText(it) }
                        data2?.let { edtData.setText(it) }
                        horario?.let { edtHorarioInicio.setText(it) }

                        Toast.makeText(this, "Dados extraídos — confira e ajuste se precisar", Toast.LENGTH_LONG).show()
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
            Toast.makeText(this, "Preencha pelo menos o nome e o endereço (extraia de uma imagem ou digite)", Toast.LENGTH_LONG).show()
            return
        }

        EventoStorage.adicionar(
            this, nome, endereco, categoriaSelecionada,
            edtData.text.toString().trim(),
            edtHorarioInicio.text.toString().trim(),
            edtHorarioFim.text.toString().trim()
        )

        Toast.makeText(this, "Evento salvo", Toast.LENGTH_SHORT).show()
        finish()
    }
}
