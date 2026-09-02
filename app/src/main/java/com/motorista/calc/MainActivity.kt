package com.motorista.calc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private val CODIGO_PERMISSAO_GRAVACAO = 501

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)
        TrialManager.garantirInicializado(this)

        val switchAtivo = findViewById<android.widget.Switch>(R.id.switchAtivo)
        switchAtivo.setOnCheckedChangeListener { _, ativado ->
            prefs.edit().putBoolean(RideAccessibilityService.PREF_MONITORAMENTO_ATIVO, ativado).apply()
        }

        findViewById<android.view.View>(R.id.btnHistorico).setOnClickListener {
            startActivity(Intent(this, HistoricoActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnPrints).setOnClickListener {
            startActivity(Intent(this, PrintsActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnGravacoes).setOnClickListener {
            startActivity(Intent(this, RecordingsActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnParametros).setOnClickListener {
            startActivity(Intent(this, ParametrosActivity::class.java))
        }

        findViewById<android.widget.TextView>(R.id.btnGravar).setOnClickListener {
            alternarGravacao()
        }
    }

    override fun onResume() {
        super.onResume()
        atualizarResumoHoje()
        atualizarTrial()
        atualizarBotaoGravar()
        RideRecorderService.aoMudarEstado = { atualizarBotaoGravar() }
    }

    override fun onPause() {
        super.onPause()
        RideRecorderService.aoMudarEstado = null
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

    private fun atualizarTrial() {
        val txtTeste = findViewById<android.widget.TextView>(R.id.txtTeste)
        val switchAtivo = findViewById<android.widget.Switch>(R.id.switchAtivo)

        if (TrialManager.expirou(this)) {
            txtTeste.text = "⛔ Período de teste encerrado (${TrialManager.DIAS_DE_TESTE} dias). Fale com quem te passou o app pra continuar usando."
            txtTeste.setTextColor(Color.parseColor("#F7C1C1"))
            switchAtivo.isChecked = false
            switchAtivo.isEnabled = false
            prefs.edit().putBoolean(RideAccessibilityService.PREF_MONITORAMENTO_ATIVO, false).apply()
        } else {
            val restantes = TrialManager.diasRestantes(this)
            txtTeste.text = "🧪 Versão de teste: $restantes dia(s) restante(s)"
            txtTeste.setTextColor(Color.parseColor("#9AA4B2"))
            switchAtivo.isEnabled = true
            switchAtivo.isChecked = prefs.getBoolean(RideAccessibilityService.PREF_MONITORAMENTO_ATIVO, true)
        }
    }

    private fun atualizarBotaoGravar() {
        val btnGravar = findViewById<android.widget.TextView>(R.id.btnGravar)
        if (RideRecorderService.emGravacao) {
            btnGravar.text = "⏺️ Gravação iniciada — clique para encerrar"
            btnGravar.setTextColor(Color.parseColor("#C0DD97"))
        } else {
            btnGravar.text = "🔴 Iniciar gravação da corrida"
            btnGravar.setTextColor(Color.parseColor("#F7C1C1"))
        }
    }

    private fun alternarGravacao() {
        if (RideRecorderService.emGravacao) {
            startService(Intent(this, RideRecorderService::class.java).apply { action = RideRecorderService.ACTION_STOP })
            return
        }

        val temCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val temMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (!temCamera || !temMic) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                CODIGO_PERMISSAO_GRAVACAO
            )
            return
        }

        iniciarServicoDeGravacao()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CODIGO_PERMISSAO_GRAVACAO) {
            val concedidas = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (concedidas) {
                iniciarServicoDeGravacao()
            } else {
                android.widget.Toast.makeText(this, "Permissão de câmera/microfone é necessária para gravar", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun iniciarServicoDeGravacao() {
        val intent = Intent(this, RideRecorderService::class.java).apply { action = RideRecorderService.ACTION_START }
        ContextCompat.startForegroundService(this, intent)
    }
}
