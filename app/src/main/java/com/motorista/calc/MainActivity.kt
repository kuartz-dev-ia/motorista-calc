package com.motorista.calc

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)

        val switchAtivo = findViewById<android.widget.Switch>(R.id.switchAtivo)
        switchAtivo.isChecked = prefs.getBoolean(RideAccessibilityService.PREF_MONITORAMENTO_ATIVO, true)
        switchAtivo.setOnCheckedChangeListener { _, ativado ->
            prefs.edit().putBoolean(RideAccessibilityService.PREF_MONITORAMENTO_ATIVO, ativado).apply()
        }

        findViewById<android.view.View>(R.id.btnHistorico).setOnClickListener {
            startActivity(Intent(this, HistoricoActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnPrints).setOnClickListener {
            startActivity(Intent(this, PrintsActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnParametros).setOnClickListener {
            startActivity(Intent(this, ParametrosActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        atualizarResumoHoje()
    }

    private fun atualizarResumoHoje() {
        val registros = HistoricoStorage.listarDoDia(this)
        val aceitas = registros.filter { it.aceita && !it.cancelada }

        val ganhoLiquido = aceitas.sumOf { it.lucroLiquido ?: 0.0 }
        val km = aceitas.sumOf { it.distanciaTotalKm }
        val horas = aceitas.sumOf { it.tempoTotalMin } / 60.0

        findViewById<android.widget.TextView>(R.id.txtGanhoHoje).text = "R$ %.2f".format(ganhoLiquido)
        findViewById<android.widget.TextView>(R.id.txtResumoHoje).text =
            "${aceitas.size} corridas aceitas • %.1f km • %.1f h".format(km, horas)
    }
}
