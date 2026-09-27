package com.motorista.calc

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MetasActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var edtMetaSemanal: EditText
    private lateinit var edtMetaMensal: EditText
    private lateinit var edtDiasTrabalhoMes: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metas)

        prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)
        edtMetaSemanal = findViewById(R.id.edtMetaSemanal)
        edtMetaMensal = findViewById(R.id.edtMetaMensal)
        edtDiasTrabalhoMes = findViewById(R.id.edtDiasTrabalhoMes)

        preencherSeExistir(edtMetaSemanal, RideAccessibilityService.PREF_META_SEMANAL)
        preencherSeExistir(edtMetaMensal, RideAccessibilityService.PREF_META_MENSAL)
        edtDiasTrabalhoMes.setText(prefs.getInt(RideAccessibilityService.PREF_DIAS_TRABALHO_MES, 22).toString())

        val watcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { atualizarPreviewMetaBruta() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        edtDiasTrabalhoMes.addTextChangedListener(watcher)

        findViewById<TextView>(R.id.btnSalvar).setOnClickListener { salvar() }

        atualizarPreviewMetaBruta()
    }

    private fun preencherSeExistir(campo: EditText, chave: String) {
        if (prefs.contains(chave)) {
            val valor = prefs.getFloat(chave, 0f)
            if (valor != 0f) campo.setText(valor.toString())
        }
    }

    private fun atualizarPreviewMetaBruta() {
        val diasDigitados = edtDiasTrabalhoMes.text.toString().toIntOrNull()
        val diasOriginais = prefs.getInt(RideAccessibilityService.PREF_DIAS_TRABALHO_MES, 22)
        if (diasDigitados != null && diasDigitados > 0) {
            prefs.edit().putInt(RideAccessibilityService.PREF_DIAS_TRABALHO_MES, diasDigitados).apply()
        }

        val resultado = MetaMensalCalculator.calcular(this)
        findViewById<TextView>(R.id.txtMetaBrutaPreviewConfig).text = "R$ %.2f/dia".format(resultado.metaBrutaDiaria)
        findViewById<TextView>(R.id.txtMetaBrutaSubtituloConfig).text = "pra cobrir R$ %.2f/mês em %d dia(s) restantes (meta base R$ %.2f/dia)".format(resultado.custoMensalTotal, resultado.diasRestantes, resultado.metaBrutaDiariaBase)

        if (diasDigitados == null || diasDigitados <= 0) {
            prefs.edit().putInt(RideAccessibilityService.PREF_DIAS_TRABALHO_MES, diasOriginais).apply()
        }
    }

    private fun salvar() {
        prefs.edit().apply {
            putFloat(RideAccessibilityService.PREF_META_SEMANAL, edtMetaSemanal.text.toString().toFloatOrNull() ?: 0f)
            putFloat(RideAccessibilityService.PREF_META_MENSAL, edtMetaMensal.text.toString().toFloatOrNull() ?: 0f)
            putInt(RideAccessibilityService.PREF_DIAS_TRABALHO_MES, edtDiasTrabalhoMes.text.toString().toIntOrNull() ?: 22)
            apply()
        }
        atualizarPreviewMetaBruta()
        Toast.makeText(this, "Metas salvas", Toast.LENGTH_SHORT).show()
    }
}
