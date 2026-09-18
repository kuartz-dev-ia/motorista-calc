package com.motorista.calc

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ContasActivity : AppCompatActivity() {

    private lateinit var edtNome: EditText
    private lateinit var edtValor: EditText
    private lateinit var edtDiaVencimento: EditText
    private lateinit var containerLista: LinearLayout
    private lateinit var txtTotalMensal: TextView
    private var categoriaSelecionada = "Casa"

    private val chips by lazy {
        listOf(
            findViewById<TextView>(R.id.chipCasa) to "Casa",
            findViewById<TextView>(R.id.chipCarro) to "Carro",
            findViewById<TextView>(R.id.chipMoto) to "Moto",
            findViewById<TextView>(R.id.chipCartao) to "Cartão",
            findViewById<TextView>(R.id.chipOutro) to "Outro"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contas)

        edtNome = findViewById(R.id.edtNomeConta)
        edtValor = findViewById(R.id.edtValorConta)
        edtDiaVencimento = findViewById(R.id.edtDiaVencimento)
        containerLista = findViewById(R.id.containerContas)
        txtTotalMensal = findViewById(R.id.txtTotalContas)

        for ((chip, categoria) in chips) {
            chip.setOnClickListener { selecionarCategoria(chip, categoria) }
        }
        selecionarCategoria(findViewById(R.id.chipCasa), "Casa")

        findViewById<TextView>(R.id.btnSalvarConta).setOnClickListener { salvar() }
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

    private fun salvar() {
        val nome = edtNome.text.toString().trim()
        val valor = edtValor.text.toString().toDoubleOrNull()
        val dia = edtDiaVencimento.text.toString().toIntOrNull()

        if (nome.isBlank() || valor == null || valor <= 0) {
            Toast.makeText(this, "Preencha o nome e o valor corretamente", Toast.LENGTH_LONG).show()
            return
        }
        if (dia != null && (dia < 1 || dia > 31)) {
            Toast.makeText(this, "Dia de vencimento precisa estar entre 1 e 31 (ou deixe vazio se não tiver data fixa)", Toast.LENGTH_LONG).show()
            return
        }

        ContaStorage.adicionar(this, nome, categoriaSelecionada, valor, dia, 3)
        edtNome.text.clear()
        edtValor.text.clear()
        edtDiaVencimento.text.clear()
        Toast.makeText(this, "Conta adicionada", Toast.LENGTH_SHORT).show()
        atualizarLista()
    }

    private fun atualizarLista() {
        val contas = ContaStorage.listarTodos(this)
        txtTotalMensal.text = "Total mensal: R$ %.2f".format(ContaStorage.totalMensal(this))

        containerLista.removeAllViews()
        if (contas.isEmpty()) {
            containerLista.addView(TextView(this).apply {
                text = "Nenhuma conta cadastrada ainda."
                setTextColor(Color.parseColor("#8B96AC"))
                textSize = 13f
            })
            return
        }

        val emojiCategoria = mapOf("Casa" to "🏠", "Carro" to "🚗", "Moto" to "🏍️", "Cartão" to "💳", "Outro" to "📦")

        for (conta in contas.sortedBy { it.diaVencimento ?: 99 }) {
            val (statusTexto, statusCor) = ContaStorage.statusDaConta(conta)

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(this@ContasActivity, R.drawable.bg_card_dark)
                setPadding(24, 20, 24, 20)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 12
                }
            }

            val linhaTopo = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            linhaTopo.addView(TextView(this).apply {
                text = "${emojiCategoria[conta.categoria] ?: "📦"} ${conta.nome}"
                setTextColor(Color.WHITE)
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            linhaTopo.addView(TextView(this).apply {
                text = "R$ %.2f".format(conta.valorMensal)
                setTextColor(Color.parseColor("#1FE7A0"))
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            card.addView(linhaTopo)

            val linhaBaixo = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 10, 0, 0)
            }
            linhaBaixo.addView(TextView(this).apply {
                text = statusTexto
                setTextColor(Color.parseColor(statusCor))
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    ContaStorage.alternarPaga(this@ContasActivity, conta.id)
                    atualizarLista()
                }
            })
            linhaBaixo.addView(TextView(this).apply {
                text = "🗑️"
                textSize = 14f
                setOnClickListener { confirmarExclusao(conta) }
            })
            card.addView(linhaBaixo)

            container_add(card)
        }
    }

    private fun container_add(view: android.view.View) {
        containerLista.addView(view)
    }

    private fun confirmarExclusao(conta: Conta) {
        AlertDialog.Builder(this, R.style.DialogTemaEscuro)
            .setTitle("Excluir conta")
            .setMessage("Tem certeza que quer excluir \"${conta.nome}\"?")
            .setPositiveButton("Excluir") { _, _ ->
                ContaStorage.apagar(this, conta.id)
                atualizarLista()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
