package com.motorista.calc

import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class CombustivelActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var chipGasolina: TextView
    private lateinit var chipEtanol: TextView
    private lateinit var chipGnv: TextView
    private lateinit var chipEletrico: TextView
    private lateinit var edtPrecoCombustivel: EditText
    private lateinit var edtConsumo: EditText
    private lateinit var txtLabelPreco: TextView
    private lateinit var txtLabelConsumo: TextView
    private lateinit var txtCustoPorKmPreview: TextView
    private lateinit var txtResumoCombustiveis: TextView

    private var combustivelSelecionado: String = "etanol"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_combustivel)

        prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)

        chipGasolina = findViewById(R.id.chipGasolina)
        chipEtanol = findViewById(R.id.chipEtanol)
        chipGnv = findViewById(R.id.chipGnv)
        chipEletrico = findViewById(R.id.chipEletrico)
        edtPrecoCombustivel = findViewById(R.id.edtPrecoCombustivel)
        edtConsumo = findViewById(R.id.edtConsumo)
        txtLabelPreco = findViewById(R.id.txtLabelPrecoCombustivel)
        txtLabelConsumo = findViewById(R.id.txtLabelConsumoCombustivel)
        txtCustoPorKmPreview = findViewById(R.id.txtCustoPorKmPreview)
        txtResumoCombustiveis = findViewById(R.id.txtResumoCombustiveis)

        combustivelSelecionado = prefs.getString(RideAccessibilityService.PREF_COMBUSTIVEL_ATIVO, "etanol") ?: "etanol"
        selecionarPill(combustivelSelecionado, carregarCampos = true)

        chipGasolina.setOnClickListener { trocarCombustivel("gasolina") }
        chipEtanol.setOnClickListener { trocarCombustivel("etanol") }
        chipGnv.setOnClickListener { trocarCombustivel("gnv") }
        chipEletrico.setOnClickListener { trocarCombustivel("eletrico") }

        val watcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { atualizarPreviewCustoPorKm() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        edtPrecoCombustivel.addTextChangedListener(watcher)
        edtConsumo.addTextChangedListener(watcher)

        findViewById<TextView>(R.id.btnSalvar).setOnClickListener { salvar() }

        atualizarResumoCombustiveis()
    }

    private fun salvar() {
        salvarCamposDoCombustivel(combustivelSelecionado)
        prefs.edit().putString(RideAccessibilityService.PREF_COMBUSTIVEL_ATIVO, combustivelSelecionado).apply()
        atualizarResumoCombustiveis()
        Toast.makeText(this, "Combustível salvo", Toast.LENGTH_SHORT).show()
    }

    private fun trocarCombustivel(novo: String) {
        salvarCamposDoCombustivel(combustivelSelecionado)
        selecionarPill(novo, carregarCampos = true)
    }

    private fun selecionarPill(tipo: String, carregarCampos: Boolean) {
        combustivelSelecionado = tipo

        val pills = mapOf("gasolina" to chipGasolina, "etanol" to chipEtanol, "gnv" to chipGnv, "eletrico" to chipEletrico)
        for ((chaveTipo, chip) in pills) {
            val selecionado = chaveTipo == tipo
            chip.background = ContextCompat.getDrawable(this, if (selecionado) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            chip.setTextColor(if (selecionado) Color.parseColor("#06231F") else Color.parseColor("#8A94A3"))
            chip.setTypeface(chip.typeface, if (selecionado) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }

        if (tipo == "eletrico") {
            txtLabelPreco.text = "Preço por kWh"
            txtLabelConsumo.text = "Consumo do veículo (km/kWh)"
            edtPrecoCombustivel.hint = "R$/kWh"
            edtConsumo.hint = "km/kWh"
        } else {
            txtLabelPreco.text = "Preço por litro"
            txtLabelConsumo.text = "Consumo do veículo (km/l)"
            edtPrecoCombustivel.hint = ""
            edtConsumo.hint = ""
        }

        if (carregarCampos) {
            val (preco, consumo) = obterValoresPadrao(tipo)
            edtPrecoCombustivel.setText(prefs.getFloat(chavePreco(tipo), preco).toString())
            edtConsumo.setText(prefs.getFloat(chaveConsumo(tipo), consumo).toString())
        }
        atualizarPreviewCustoPorKm()
    }

    private fun obterValoresPadrao(tipo: String): Pair<Float, Float> = when (tipo) {
        "gasolina" -> Pair(6.10f, 10.0f)
        "gnv" -> Pair(4.50f, 12.0f)
        "eletrico" -> Pair(0.70f, 6.0f)
        else -> Pair(4.20f, 7.0f)
    }

    private fun chavePreco(tipo: String) = when (tipo) {
        "gasolina" -> RideAccessibilityService.PREF_PRECO_GASOLINA
        "gnv" -> RideAccessibilityService.PREF_PRECO_GNV
        "eletrico" -> RideAccessibilityService.PREF_PRECO_ELETRICO
        else -> RideAccessibilityService.PREF_PRECO_ETANOL
    }

    private fun chaveConsumo(tipo: String) = when (tipo) {
        "gasolina" -> RideAccessibilityService.PREF_CONSUMO_GASOLINA
        "gnv" -> RideAccessibilityService.PREF_CONSUMO_GNV
        "eletrico" -> RideAccessibilityService.PREF_CONSUMO_ELETRICO
        else -> RideAccessibilityService.PREF_CONSUMO_ETANOL
    }

    private fun salvarCamposDoCombustivel(tipo: String) {
        val preco = edtPrecoCombustivel.text.toString().toFloatOrNull() ?: return
        val consumo = edtConsumo.text.toString().toFloatOrNull() ?: return
        prefs.edit()
            .putFloat(chavePreco(tipo), preco)
            .putFloat(chaveConsumo(tipo), consumo)
            .apply()
    }

    private fun atualizarPreviewCustoPorKm() {
        val preco = edtPrecoCombustivel.text.toString().toDoubleOrNull() ?: 0.0
        val consumo = edtConsumo.text.toString().toDoubleOrNull() ?: 0.0
        val custoPorKm = if (consumo > 0) preco / consumo else 0.0
        txtCustoPorKmPreview.text = "R$ %.2f/km".format(custoPorKm)
    }

    private fun atualizarResumoCombustiveis() {
        val tipos = listOf("gasolina" to "Gasolina", "etanol" to "Etanol", "gnv" to "GNV", "eletrico" to "Elétrico")
        val linhas = tipos.map { (chave, nome) ->
            val (precoPadrao, consumoPadrao) = obterValoresPadrao(chave)
            val preco = prefs.getFloat(chavePreco(chave), precoPadrao)
            val consumo = prefs.getFloat(chaveConsumo(chave), consumoPadrao)
            val custo = if (consumo > 0) preco / consumo else 0.0
            "$nome — R$ %.2f/km".format(custo)
        }
        txtResumoCombustiveis.text = linhas.joinToString("\n")
    }
}
