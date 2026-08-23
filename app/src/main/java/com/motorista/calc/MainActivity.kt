
package com.motorista.calc

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)

        val btnAtivarAcessibilidade = findViewById<android.widget.Button>(R.id.btnAtivarAcessibilidade)
        val btnPermitirOverlay = findViewById<android.widget.Button>(R.id.btnPermitirOverlay)
        val edtMinKm = findViewById<android.widget.EditText>(R.id.edtMinKm)
        val edtMinHora = findViewById<android.widget.EditText>(R.id.edtMinHora)
        val edtConsumo = findViewById<android.widget.EditText>(R.id.edtConsumo)
        val edtPrecoCombustivel = findViewById<android.widget.EditText>(R.id.edtPrecoCombustivel)
        val btnSalvar = findViewById<android.widget.Button>(R.id.btnSalvar)
        val txtStatus = findViewById<android.widget.TextView>(R.id.txtStatus)
        val btnVerDebug = findViewById<android.widget.Button>(R.id.btnVerDebug)
        val txtDebug = findViewById<android.widget.TextView>(R.id.txtDebug)

        edtMinKm.setText(prefs.getFloat(RideAccessibilityService.PREF_MIN_KM, 1.50f).toString())
        edtMinHora.setText(prefs.getFloat(RideAccessibilityService.PREF_MIN_HORA, 25.0f).toString())
        edtConsumo.setText(prefs.getFloat(RideAccessibilityService.PREF_CONSUMO, 12.0f).toString())
        edtPrecoCombustivel.setText(prefs.getFloat(RideAccessibilityService.PREF_PRECO_COMBUSTIVEL, 6.10f).toString())

        btnAtivarAcessibilidade.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnPermitirOverlay.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        btnSalvar.setOnClickListener {
            prefs.edit().apply {
                putFloat(RideAccessibilityService.PREF_MIN_KM, edtMinKm.text.toString().toFloatOrNull() ?: 1.50f)
                putFloat(RideAccessibilityService.PREF_MIN_HORA, edtMinHora.text.toString().toFloatOrNull() ?: 25.0f)
                putFloat(RideAccessibilityService.PREF_CONSUMO, edtConsumo.text.toString().toFloatOrNull() ?: 12.0f)
                putFloat(RideAccessibilityService.PREF_PRECO_COMBUSTIVEL, edtPrecoCombustivel.text.toString().toFloatOrNull() ?: 6.10f)
                apply()
            }
            android.widget.Toast.makeText(this, "Configurações salvas", android.widget.Toast.LENGTH_SHORT).show()
        }

        btnVerDebug.setOnClickListener {
            val texto = prefs.getString(RideAccessibilityService.PREF_ULTIMO_TEXTO, null)
            txtDebug.text = texto ?: "Nenhum texto capturado ainda. Abra o app de corrida e deixe uma tela de solicitação aparecer, depois volte aqui e toque de novo."
        }

        atualizarStatus(txtStatus)
    }

    override fun onResume() {
        super.onResume()
        findViewById<android.widget.TextView>(R.id.txtStatus)?.let { atualizarStatus(it) }
    }

    private fun atualizarStatus(txtStatus: android.widget.TextView) {
        val acessibilidadeAtiva = servicoDeAcessibilidadeEstaAtivo()
        val overlayPermitido = Settings.canDrawOverlays(this)
        txtStatus.text = buildString {
            append(if (acessibilidadeAtiva) "✅ Acessibilidade ativada\n" else "❌ Acessibilidade desativada\n")
            append(if (overlayPermitido) "✅ Permissão de overlay concedida" else "❌ Permissão de overlay pendente")
        }
    }

    private fun servicoDeAcessibilidadeEstaAtivo(): Boolean {
        val am = getSystemService<AccessibilityManager>() ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }
}
