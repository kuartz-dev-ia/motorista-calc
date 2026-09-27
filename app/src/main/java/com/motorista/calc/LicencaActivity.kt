package com.motorista.calc

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LicencaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_licenca)

        findViewById<TextView>(R.id.txtIdDispositivo).text = "ID do aparelho: ${LicenseManager.obterIdDispositivo(this)}"

        findViewById<android.view.View>(R.id.btnCopiarIdDispositivo).setOnClickListener {
            val id = LicenseManager.obterIdDispositivo(this)
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("id_dispositivo", id))
            Toast.makeText(this, "ID copiado! Envie pra quem for liberar seu acesso.", Toast.LENGTH_LONG).show()
        }

        findViewById<android.view.View>(R.id.btnVerDebugLicenca).setOnClickListener { mostrarDebugLicenca() }
    }

    override fun onResume() {
        super.onResume()
        LicenseManager.verificarEmSegundoPlano(this)
        atualizarStatusLicenca()
    }

    private fun atualizarStatusLicenca() {
        val txtStatusLicenca = findViewById<TextView>(R.id.txtStatusLicenca)
        if (LicenseManager.estaBloqueadoExplicitamente(this)) {
            txtStatusLicenca.text = "⛔ Acesso bloqueado"
            txtStatusLicenca.setTextColor(Color.parseColor("#C9807E"))
        } else if (LicenseManager.estaLiberadoPorLicenca(this)) {
            txtStatusLicenca.text = "✔ Acesso liberado"
            txtStatusLicenca.setTextColor(Color.parseColor("#2FB4A6"))
        } else {
            txtStatusLicenca.text = "⏳ Aguardando liberação — envie o ID abaixo pro administrador"
            txtStatusLicenca.setTextColor(Color.parseColor("#8A94A3"))
        }
    }

    private fun mostrarDebugLicenca() {
        LicenseManager.verificarEmSegundoPlano(this)
        val diagnostico = LicenseManager.obterDiagnostico(this)

        val scrollView = android.widget.ScrollView(this)
        val textoView = TextView(this).apply {
            text = diagnostico
            setTextColor(Color.parseColor("#C7CDD6"))
            textSize = 12f
            setTextIsSelectable(true)
            setPadding(32, 24, 32, 24)
        }
        scrollView.addView(textoView)

        val dialog = AlertDialog.Builder(this, R.style.DialogTemaEscuro)
            .setTitle("🔍 Diagnóstico da Licença")
            .setView(scrollView)
            .setPositiveButton("Copiar tudo") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("debug_licenca", diagnostico))
                Toast.makeText(this, "Copiado!", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Fechar", null)
            .show()
        DialogUtils.aplicarCoresBotoes(dialog)
    }
}
