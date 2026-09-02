package com.motorista.calc

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService

class ParametrosActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parametros)

        prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)

        val btnAtivarAcessibilidade = findViewById<android.widget.Button>(R.id.btnAtivarAcessibilidade)
        val btnPermitirOverlay = findViewById<android.widget.Button>(R.id.btnPermitirOverlay)
        val edtMinKm = findViewById<android.widget.EditText>(R.id.edtMinKm)
        val edtMinHora = findViewById<android.widget.EditText>(R.id.edtMinHora)
        val edtConsumo = findViewById<android.widget.EditText>(R.id.edtConsumo)
        val edtPrecoCombustivel = findViewById<android.widget.EditText>(R.id.edtPrecoCombustivel)
        val edtFinanciamento = findViewById<android.widget.EditText>(R.id.edtFinanciamento)
        val edtSeguro = findViewById<android.widget.EditText>(R.id.edtSeguro)
        val edtIpva = findViewById<android.widget.EditText>(R.id.edtIpva)
        val edtLicenciamento = findViewById<android.widget.EditText>(R.id.edtLicenciamento)
        val edtManutencao = findViewById<android.widget.EditText>(R.id.edtManutencao)
        val edtContasPessoais = findViewById<android.widget.EditText>(R.id.edtContasPessoais)
        val edtKmMes = findViewById<android.widget.EditText>(R.id.edtKmMes)
        val edtLimitePausa = findViewById<android.widget.EditText>(R.id.edtLimitePausa)
        val btnSalvar = findViewById<android.widget.TextView>(R.id.btnSalvar)
        val txtStatus = findViewById<android.widget.TextView>(R.id.txtStatus)

        preencherSeExistir(edtMinKm, RideAccessibilityService.PREF_MIN_KM)
        preencherSeExistir(edtMinHora, RideAccessibilityService.PREF_MIN_HORA)
        preencherSeExistir(edtConsumo, RideAccessibilityService.PREF_CONSUMO)
        preencherSeExistir(edtPrecoCombustivel, RideAccessibilityService.PREF_PRECO_COMBUSTIVEL)
        preencherSeExistir(edtFinanciamento, RideAccessibilityService.PREF_FINANCIAMENTO)
        preencherSeExistir(edtSeguro, RideAccessibilityService.PREF_SEGURO)
        preencherSeExistir(edtIpva, RideAccessibilityService.PREF_IPVA)
        preencherSeExistir(edtLicenciamento, RideAccessibilityService.PREF_LICENCIAMENTO)
        preencherSeExistir(edtManutencao, RideAccessibilityService.PREF_MANUTENCAO)
        preencherSeExistir(edtContasPessoais, RideAccessibilityService.PREF_CONTAS_PESSOAIS)
        preencherSeExistir(edtKmMes, RideAccessibilityService.PREF_KM_MES)
        preencherSeExistir(edtLimitePausa, RideAccessibilityService.PREF_LIMITE_PAUSA_HORAS)

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
                putFloat(RideAccessibilityService.PREF_FINANCIAMENTO, edtFinanciamento.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_SEGURO, edtSeguro.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_IPVA, edtIpva.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_LICENCIAMENTO, edtLicenciamento.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_MANUTENCAO, edtManutencao.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_CONTAS_PESSOAIS, edtContasPessoais.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_KM_MES, edtKmMes.text.toString().toFloatOrNull() ?: 3000f)
                putFloat(RideAccessibilityService.PREF_LIMITE_PAUSA_HORAS, edtLimitePausa.text.toString().toFloatOrNull() ?: 3.0f)
                apply()
            }
            android.widget.Toast.makeText(this, "Configurações salvas", android.widget.Toast.LENGTH_SHORT).show()
        }

        atualizarStatus(txtStatus)
    }

    private fun preencherSeExistir(campo: android.widget.EditText, chave: String) {
        if (prefs.contains(chave)) {
            campo.setText(prefs.getFloat(chave, 0f).toString())
        }
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
