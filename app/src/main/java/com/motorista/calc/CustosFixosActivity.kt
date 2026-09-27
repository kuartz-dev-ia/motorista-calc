package com.motorista.calc

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CustosFixosActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custos_fixos)

        prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)

        val edtFinanciamento = findViewById<EditText>(R.id.edtFinanciamento)
        val edtSeguro = findViewById<EditText>(R.id.edtSeguro)
        val edtIpva = findViewById<EditText>(R.id.edtIpva)
        val edtLicenciamento = findViewById<EditText>(R.id.edtLicenciamento)
        val edtManutencao = findViewById<EditText>(R.id.edtManutencao)
        val edtContasPessoais = findViewById<EditText>(R.id.edtContasPessoais)
        val edtKmMes = findViewById<EditText>(R.id.edtKmMes)

        preencherSeExistir(edtFinanciamento, RideAccessibilityService.PREF_FINANCIAMENTO)
        preencherSeExistir(edtSeguro, RideAccessibilityService.PREF_SEGURO)
        preencherSeExistir(edtIpva, RideAccessibilityService.PREF_IPVA)
        preencherSeExistir(edtLicenciamento, RideAccessibilityService.PREF_LICENCIAMENTO)
        preencherSeExistir(edtManutencao, RideAccessibilityService.PREF_MANUTENCAO)
        preencherSeExistir(edtContasPessoais, RideAccessibilityService.PREF_CONTAS_PESSOAIS)
        preencherSeExistir(edtKmMes, RideAccessibilityService.PREF_KM_MES)

        findViewById<TextView>(R.id.btnSalvar).setOnClickListener {
            prefs.edit().apply {
                putFloat(RideAccessibilityService.PREF_FINANCIAMENTO, edtFinanciamento.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_SEGURO, edtSeguro.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_IPVA, edtIpva.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_LICENCIAMENTO, edtLicenciamento.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_MANUTENCAO, edtManutencao.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_CONTAS_PESSOAIS, edtContasPessoais.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_KM_MES, edtKmMes.text.toString().toFloatOrNull() ?: 0f)
                apply()
            }
            Toast.makeText(this, "Custos fixos salvos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun preencherSeExistir(campo: EditText, chave: String) {
        if (prefs.contains(chave)) {
            val valor = prefs.getFloat(chave, 0f)
            if (valor != 0f) campo.setText(valor.toString())
        }
    }
}
