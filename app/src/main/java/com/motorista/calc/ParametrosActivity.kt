package com.motorista.calc

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ParametrosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parametros)

        findViewById<android.view.View>(R.id.btnAbrirLicenca).setOnClickListener { startActivity(Intent(this, LicencaActivity::class.java)) }
        findViewById<android.view.View>(R.id.btnAbrirMetas).setOnClickListener { startActivity(Intent(this, MetasActivity::class.java)) }
        findViewById<android.view.View>(R.id.btnAbrirCombustivel).setOnClickListener { startActivity(Intent(this, CombustivelActivity::class.java)) }
        findViewById<android.view.View>(R.id.btnAbrirCustosFixos).setOnClickListener { startActivity(Intent(this, CustosFixosActivity::class.java)) }
        findViewById<android.view.View>(R.id.btnAbrirLimites).setOnClickListener { startActivity(Intent(this, LimitesActivity::class.java)) }
        findViewById<android.view.View>(R.id.btnAbrirSeguranca).setOnClickListener { startActivity(Intent(this, SegurancaActivity::class.java)) }
        findViewById<android.view.View>(R.id.btnAbrirDiagnostico).setOnClickListener { startActivity(Intent(this, DiagnosticoActivity::class.java)) }
        findViewById<android.view.View>(R.id.btnAbrirBackup).setOnClickListener { startActivity(Intent(this, BackupActivity::class.java)) }
        findViewById<android.view.View>(R.id.btnAbrirMaisConfiguracoes).setOnClickListener { startActivity(Intent(this, MaisOpcoesActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        LicenseManager.verificarEmSegundoPlano(this)
        atualizarStatusLicencaResumo()
    }

    private fun atualizarStatusLicencaResumo() {
        val txt = findViewById<TextView>(R.id.txtStatusLicencaResumo)
        if (LicenseManager.estaBloqueadoExplicitamente(this)) {
            txt.text = "⛔ Acesso bloqueado"
            txt.setTextColor(Color.parseColor("#C9807E"))
        } else if (LicenseManager.estaLiberadoPorLicenca(this)) {
            txt.text = "✔ Acesso liberado"
            txt.setTextColor(Color.parseColor("#2FB4A6"))
        } else {
            txt.text = "⏳ Aguardando liberação"
            txt.setTextColor(Color.parseColor("#8A94A3"))
        }
    }
}
