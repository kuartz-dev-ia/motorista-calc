package com.motorista.calc

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LimitesActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_limites)

        prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)

        val edtMinKm = findViewById<EditText>(R.id.edtMinKm)
        val edtMinHora = findViewById<EditText>(R.id.edtMinHora)

        preencherSeExistir(edtMinKm, RideAccessibilityService.PREF_MIN_KM)
        preencherSeExistir(edtMinHora, RideAccessibilityService.PREF_MIN_HORA)

        findViewById<TextView>(R.id.btnSalvar).setOnClickListener {
            prefs.edit().apply {
                putFloat(RideAccessibilityService.PREF_MIN_KM, edtMinKm.text.toString().toFloatOrNull() ?: 1.50f)
                putFloat(RideAccessibilityService.PREF_MIN_HORA, edtMinHora.text.toString().toFloatOrNull() ?: 25.0f)
                apply()
            }
            Toast.makeText(this, "Limites salvos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun preencherSeExistir(campo: EditText, chave: String) {
        if (prefs.contains(chave)) {
            val valor = prefs.getFloat(chave, 0f)
            if (valor != 0f) campo.setText(valor.toString())
        }
    }
}
