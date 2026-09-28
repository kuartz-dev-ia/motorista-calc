package com.motorista.calc

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
}
