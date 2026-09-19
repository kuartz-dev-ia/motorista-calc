package com.motorista.calc

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ContasActivity : AppCompatActivity() {

    private lateinit var edtNome: EditText
    private lateinit var edtValor: EditText
    private lateinit var btnEscolherData: TextView
    private lateinit var containerLista: LinearLayout
    private lateinit var txtTotalMensal: TextView
    private lateinit var txtTituloForm: TextView
    private lateinit var btnSalvar: TextView
    private lateinit var btnCancelarEdicao: TextView

    private var categoriaSelecionada = "Casa"
    private var dataVencimentoSelecionada: Long? = null
    private var idEmEdicao: Long? = null

    private val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

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
        btnEscolherData = findViewById(R.id.btnEscolherDataConta)
        containerLista = findViewById(R.id.containerContas)
        txtTotalMensal = findViewById(R.id.txtTotalContas)
        txtTituloForm = findViewById(R.id.txtTituloFormConta)
        btnSalvar = findViewById(R.id.btnSalvarConta)
        btnCancelarEdicao = findViewById(R.id.btnCancelarEdicaoConta)

        for ((chip, categoria) in chips) {
            chip.setOnClickListener { selecionarCategoria(chip, categoria) }
        }
        selecionarCategoria(findViewById(R.id.chipCasa), "Casa")

        btnEscolherData.setOnClickListener { abrirSeletorData() }
        btnSalvar.setOnClickListener { salvar() }
        btnCancelarEdicao.setOnClickListener { cancelarEdicao() }
    }

    override fun onResume() {
        super.onResume()
        atualizarLista()
    }

    private fun abrirSeletorData() {
        val cal = Calendar.getInstance()
        dataVencimentoSelecionada?.let { cal.timeInMillis = it }

        DatePickerDialog(
            this,
            R.style.DialogTemaEscuro,
            { _, ano, mes, dia ->
                val escolhida = Calendar.getInstance().apply {
                    set(ano, mes, dia, 12, 0, 0)
                }
                dataVencimentoSelecionada = escolhida.timeInMillis
                btnEscolherData.text = "📅 ${formatoData.format(escolhida.time)}"
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
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

        if (nome.isBlank() || valor == null || valor <= 0) {
            Toast.makeText(this, "Preencha o nome e o valor corretamente", Toast.LENGTH_LONG).show()
            return
        }

        val id = idEmEdicao
        if (id == null) {
            ContaStorage.adicionar(this, nome, categoriaSelecionada, valor, dataVencimentoSelecionada, 3)
            Toast.makeText(this, "Conta adicionada", Toast.LENGTH_SHORT).show()
        } else {
            ContaStorage.atualizar(this, id, nome, categoriaSelecionada, valor, dataVencimentoSelecionada, 3)
            Toast.makeText(this, "Conta atualizada", Toast.LENGTH_SHORT).show()
        }

        limparFormulario()
        atualizarLista()
    }

    private fun limparFormulario() {
        idEmEdicao = null
        edtNome.text.clear()
        edtValor.text.clear()
        dataVencimentoSelecionada = null
        btnEscolherData.text = "📅 Toque para escolher a data"
        selecionarCategoria(findViewById(R.id.chipCasa), "Casa")
        txtTituloForm.text = "Nova conta"
        btnSalvar.text = "Salvar conta"
        btnCancelarEdicao.visibility = android.view.View.GONE
    }

    private fun cancelarEdicao() {
        limparFormulario()
    }

    private fun editarConta(conta: Conta) {
        idEmEdicao = conta.id
        edtNome.setText(conta.nome)
        edtValor.setText("%.2f".format(conta.valorMensal))
        dataVencimentoSelecionada = conta.dataVencimentoMillis
        btnEscolherData.text = conta.dataVencimentoMillis?.let { "📅 ${formatoData.format(java.util.Date(it))}" } ?: "📅 Toque para escolher a data"

        val chipCategoria = chips.firstOrNull { it.second == conta.categoria }?.first ?: findViewById(R.id.chipCasa)
        selecionarCategoria(chipCategoria, conta.categoria)

        txtTituloForm.text = "Editando conta"
        btnSalvar.text = "Atualizar conta"
        btnCancelarEdicao.visibility = android.view.View.VISIBLE

        edtNome.requestFocus()
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

        for (conta in contas.sortedBy { it.dataVencimentoMillis ?: Long.MAX_VALUE }) {
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

            conta.dataVencimentoMillis?.let {
                card.addView(TextView(this).apply {
                    text = "Vencimento: ${formatoData.format(java.util.Date(it))}"
                    setTextColor(Color.parseColor("#8B96AC"))
                    textSize = 11f
                    setPadding(0, 4, 0, 0)
                })
            }

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
                    if (conta.dataVencimentoMillis != null) {
                        ContaStorage.alternarPaga(this@ContasActivity, conta.id)
                        atualizarLista()
                    }
                }
            })
            linhaBaixo.addView(TextView(this).apply {
                text = "✏️"
                textSize = 14f
                setPadding(0, 0, 24, 0)
                setOnClickListener { editarConta(conta) }
            })
            linhaBaixo.addView(TextView(this).apply {
                text = "🗑️"
                textSize = 14f
                setOnClickListener { confirmarExclusao(conta) }
            })
            card.addView(linhaBaixo)

            containerLista.addView(card)
        }
    }

    private fun confirmarExclusao(conta: Conta) {
        AlertDialog.Builder(this, R.style.DialogTemaEscuro)
            .setTitle("Excluir conta")
            .setMessage("Tem certeza que quer excluir \"${conta.nome}\"?")
            .setPositiveButton("Excluir") { _, _ ->
                ContaStorage.apagar(this, conta.id)
                if (idEmEdicao == conta.id) limparFormulario()
                atualizarLista()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
