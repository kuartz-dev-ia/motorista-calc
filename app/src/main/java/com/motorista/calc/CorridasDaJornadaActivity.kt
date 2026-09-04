package com.motorista.calc

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CorridasDaJornadaActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_corridas_jornada)
        container = findViewById(R.id.containerCorridasJornada)
    }

    override fun onResume() {
        super.onResume()
        atualizarLista()
    }

    private fun atualizarLista() {
        container.removeAllViews()
        val jornada = JornadaStorage.jornadaAtiva(this)

        if (jornada == null) {
            container.addView(TextView(this).apply {
                text = "Nenhuma jornada em andamento no momento."
                setTextColor(Color.parseColor("#8B96AC"))
                textSize = 13f
            })
            return
        }

        val corridas = HistoricoStorage.listarEntre(this, jornada.dataInicioMillis, System.currentTimeMillis())
            .filter { it.aceita && !it.cancelada }
            .sortedByDescending { it.dataHora }

        if (corridas.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Nenhuma corrida aceita ainda nessa jornada."
                setTextColor(Color.parseColor("#8B96AC"))
                textSize = 13f
            })
            return
        }

        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

        for (corrida in corridas) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(this@CorridasDaJornadaActivity, R.drawable.bg_card_dark)
                setPadding(24, 20, 24, 20)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 12
                }
            }

            val emoji = when (corrida.plataforma) {
                "Uber" -> "⬛"
                "99" -> "🟡"
                else -> "🚗"
            }

            val linhaTexto = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            linhaTexto.addView(TextView(this).apply {
                text = "$emoji ${formatoHora.format(Date(corrida.dataHora))} — R$ %.2f (%.1f km)".format(corrida.valorTotal, corrida.distanciaTotalKm)
                setTextColor(Color.parseColor("#FFFFFF"))
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            card.addView(linhaTexto)

            val linhaBotoes = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 10, 0, 0)
            }

            val btnEditar = TextView(this).apply {
                text = "✏️ Editar valor"
                setTextColor(Color.parseColor("#3DB8F5"))
                textSize = 12f
                setPadding(0, 0, 24, 0)
                setOnClickListener { abrirDialogoEditar(corrida) }
            }
            val btnExcluir = TextView(this).apply {
                text = "🗑️ Excluir"
                setTextColor(Color.parseColor("#F55757"))
                textSize = 12f
                setOnClickListener { confirmarExclusao(corrida) }
            }

            linhaBotoes.addView(btnEditar)
            linhaBotoes.addView(btnExcluir)
            card.addView(linhaBotoes)

            container.addView(card)
        }
    }

    private fun abrirDialogoEditar(corrida: RegistroCorrida) {
        val input = EditText(this).apply {
            setText("%.2f".format(corrida.valorTotal))
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#8B96AC"))
            background = ContextCompat.getDrawable(this@CorridasDaJornadaActivity, R.drawable.bg_input_verde)
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(this, R.style.DialogTemaEscuro)
            .setTitle("Editar valor da corrida")
            .setMessage("Use isso pra ajustar gorjeta ou reajuste de valor após aceitar.")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val novoValor = input.text.toString().toDoubleOrNull()
                if (novoValor == null || novoValor <= 0) {
                    Toast.makeText(this, "Valor inválido", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                HistoricoStorage.editarValor(this, corrida.id, novoValor)
                atualizarLista()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarExclusao(corrida: RegistroCorrida) {
        AlertDialog.Builder(this, R.style.DialogTemaEscuro)
            .setTitle("Excluir corrida")
            .setMessage("Tem certeza que quer excluir essa corrida do registro?")
            .setPositiveButton("Excluir") { _, _ ->
                HistoricoStorage.apagarRegistro(this, corrida.id)
                atualizarLista()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
