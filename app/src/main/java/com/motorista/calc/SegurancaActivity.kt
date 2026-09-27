package com.motorista.calc

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SegurancaActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seguranca)

        prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)

        val edtLimitePausa = findViewById<EditText>(R.id.edtLimitePausa)
        preencherSeExistir(edtLimitePausa, RideAccessibilityService.PREF_LIMITE_PAUSA_HORAS)

        findViewById<TextView>(R.id.btnSalvar).setOnClickListener {
            prefs.edit().putFloat(RideAccessibilityService.PREF_LIMITE_PAUSA_HORAS, edtLimitePausa.text.toString().toFloatOrNull() ?: 3.0f).apply()
            Toast.makeText(this, "Segurança salva", Toast.LENGTH_SHORT).show()
        }
    }

    private fun preencherSeExistir(campo: EditText, chave: String) {
        if (prefs.contains(chave)) {
            val valor = prefs.getFloat(chave, 0f)
            if (valor != 0f) campo.setText(valor.toString())
        }
    }
}
