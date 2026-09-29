package com.motorista.calc

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService

class DiagnosticoActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostico)

        prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)

        findViewById<TextView>(R.id.btnAtivarAcessibilidade).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<TextView>(R.id.btnPermitirOverlay).setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }

        findViewById<TextView>(R.id.btnVerDebugOcr).setOnClickListener { mostrarDebugOcr() }
    }

    override fun onResume() {
        super.onResume()
        atualizarStatus()
    }

    private fun atualizarStatus() {
        val txtStatus = findViewById<TextView>(R.id.txtStatus)
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

    private fun mostrarDebugOcr() {
        val status = prefs.getString(RideAccessibilityService.PREF_STATUS_OCR, null) ?: "Nenhum status registrado ainda."
        val log = prefs.getString(RideAccessibilityService.PREF_ULTIMO_TEXTO, null) ?: "Nenhum texto lido ainda."

        val conteudoCompleto = "STATUS ATUAL:\n$status\n\n----------\n\nÚLTIMOS TEXTOS LIDOS (mais recente primeiro):\n\n$log"

        val scrollView = android.widget.ScrollView(this)
        val textoView = TextView(this).apply {
            text = conteudoCompleto
            setTextColor(Color.parseColor("#C7CDD6"))
            textSize = 11f
            setTextIsSelectable(true)
            setPadding(32, 24, 32, 24)
        }
        scrollView.addView(textoView)

        val dialog = AlertDialog.Builder(this, R.style.DialogTemaEscuro)
            .setTitle("Debug do OCR")
            .setView(scrollView)
            .setPositiveButton("Copiar tudo") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("debug_ocr", conteudoCompleto))
                android.widget.Toast.makeText(this, "Copiado! Cole numa mensagem pra me enviar.", android.widget.Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Fechar", null)
            .show()
        DialogUtils.aplicarCoresBotoes(dialog)
    }
}
