package com.motorista.calc

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class EventosListaActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eventos_lista)
        container = findViewById(R.id.containerEventosLista)

        findViewById<android.view.View>(R.id.btnAdicionarEvento).setOnClickListener {
            startActivity(Intent(this, EventosActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
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
        container.removeAllViews()

        if (eventos.isEmpty()) {
            container.addView(TextView(this).apply {
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
                background = ContextCompat.getDrawable(this@EventosListaActivity, R.drawable.bg_card_dark)
                setPadding(24, 20, 24, 20)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 12
                }
                isClickable = true
                isFocusable = true
                setOnClickListener { abrirNoMapa(evento.endereco) }
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
                    EventoStorage.apagar(this@EventosListaActivity, evento.id)
                    atualizarLista()
                }
            })

            card.addView(linhaBotoes)
            container.addView(card)
        }
    }
}
