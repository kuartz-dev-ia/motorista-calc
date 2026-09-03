package com.motorista.calc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private val CODIGO_PERMISSAO_GRAVACAO = 501

    private lateinit var edtMetaDiaria: EditText
    private lateinit var edtCargaHoraria: EditText
    private lateinit var edtOdometroInicial: EditText
    private lateinit var txtMetaPorHoraPreview: TextView

    private val chipsMeta by lazy {
        listOf(
            findViewById<TextView>(R.id.chipMeta200) to "200",
            findViewById<TextView>(R.id.chipMeta300) to "300",
            findViewById<TextView>(R.id.chipMeta400) to "400",
            findViewById<TextView>(R.id.chipMeta500) to "500",
            findViewById<TextView>(R.id.chipMeta600) to "600"
        )
    }

    private val chipsCarga by lazy {
        listOf(
            findViewById<TextView>(R.id.chipCarga6) to "6",
            findViewById<TextView>(R.id.chipCarga8) to "8",
            findViewById<TextView>(R.id.chipCarga10) to "10",
            findViewById<TextView>(R.id.chipCarga12) to "12"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)
        TrialManager.garantirInicializado(this)

        edtMetaDiaria = findViewById(R.id.edtMetaDiaria)
        edtCargaHoraria = findViewById(R.id.edtCargaHoraria)
        edtOdometroInicial = findViewById(R.id.edtOdometroInicial)
        txtMetaPorHoraPreview = findViewById(R.id.txtMetaPorHoraPreview)

        for ((chip, valor) in chipsMeta) {
            chip.setOnClickListener { selecionarChipMeta(chip, valor) }
        }
        for ((chip, valor) in chipsCarga) {
            chip.setOnClickListener { selecionarChipCarga(chip, valor) }
        }

        val watcherAtualizaPreview = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { atualizarPreviewMetaPorHora() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        edtMetaDiaria.addTextChangedListener(watcherAtualizaPreview)
        edtCargaHoraria.addTextChangedListener(watcherAtualizaPreview)

        findViewById<TextView>(R.id.btnIniciarJornada).setOnClickListener { iniciarJornada() }
        findViewById<TextView>(R.id.btnEncerrarJornada).setOnClickListener { encerrarJornada() }
        findViewById<TextView>(R.id.btnGravar).setOnClickListener { alternarGravacao() }

        findViewById<TextView>(R.id.btnManutencoes).setOnClickListener { startActivity(Intent(this, ManutencoesActivity::class.java)) }
        findViewById<TextView>(R.id.btnAbastecimentos).setOnClickListener { startActivity(Intent(this, AbastecimentosActivity::class.java)) }
        findViewById<TextView>(R.id.btnDocumentos).setOnClickListener { startActivity(Intent(this, DocumentosActivity::class.java)) }
        findViewById<TextView>(R.id.btnPrints).setOnClickListener { startActivity(Intent(this, PrintsActivity::class.java)) }
        findViewById<TextView>(R.id.btnGravacoes).setOnClickListener { startActivity(Intent(this, RecordingsActivity::class.java)) }

        findViewById<android.view.View>(R.id.navInicio).setOnClickListener { }
        findViewById<android.view.View>(R.id.navRelatorios).setOnClickListener { startActivity(Intent(this, WeeklyActivity::class.java)) }
        findViewById<android.view.View>(R.id.navHistorico).setOnClickListener { startActivity(Intent(this, HistoricoActivity::class.java)) }
        findViewById<android.view.View>(R.id.navConfig).setOnClickListener { startActivity(Intent(this, ParametrosActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        atualizarTrial()
        atualizarTelaJornada()
        RideRecorderService.aoMudarEstado = { atualizarBotaoGravar() }
    }

    override fun onPause() {
        super.onPause()
        RideRecorderService.aoMudarEstado = null
    }

    private fun selecionarChipMeta(selecionado: TextView, valor: String) {
        for ((chip, _) in chipsMeta) {
            chip.background = ContextCompat.getDrawable(this, if (chip == selecionado) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            chip.setTextColor(if (chip == selecionado) Color.parseColor("#08131A") else Color.parseColor("#8B96AC"))
        }
        edtMetaDiaria.setText(valor)
    }

    private fun selecionarChipCarga(selecionado: TextView, valor: String) {
        for ((chip, _) in chipsCarga) {
            chip.background = ContextCompat.getDrawable(this, if (chip == selecionado) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            chip.setTextColor(if (chip == selecionado) Color.parseColor("#08131A") else Color.parseColor("#8B96AC"))
        }
        edtCargaHoraria.setText(valor)
    }

    private fun atualizarPreviewMetaPorHora() {
        val meta = edtMetaDiaria.text.toString().toDoubleOrNull() ?: 0.0
        val carga = edtCargaHoraria.text.toString().toDoubleOrNull() ?: 0.0
        val metaPorHora = if (carga > 0) meta / carga else 0.0
        txtMetaPorHoraPreview.text = "R$ %.2f/h".format(metaPorHora)
    }

    private fun iniciarJornada() {
        if (TrialManager.expirou(this)) {
            Toast.makeText(this, "Período de teste encerrado.", Toast.LENGTH_LONG).show()
            return
        }

        val meta = edtMetaDiaria.text.toString().toDoubleOrNull()
        val carga = edtCargaHoraria.text.toString().toDoubleOrNull()
        val odometro = edtOdometroInicial.text.toString().toDoubleOrNull()

        if (meta == null || meta <= 0 || carga == null || carga <= 0 || odometro == null || odometro < 0) {
            Toast.makeText(this, "Preencha meta diária, carga horária e odômetro inicial", Toast.LENGTH_LONG).show()
            return
        }

        JornadaStorage.iniciar(this, meta, carga, odometro)
        prefs.edit()
            .putBoolean(RideAccessibilityService.PREF_MONITORAMENTO_ATIVO, true)
            .putLong(RideAccessibilityService.PREF_INICIO_SESSAO, System.currentTimeMillis())
            .apply()

        atualizarTelaJornada()
    }

    private fun encerrarJornada() {
        val jornada = JornadaStorage.jornadaAtiva(this) ?: return
        val stats = JornadaStorage.calcularStats(this, jornada)
        JornadaStorage.encerrar(this, jornada.id, jornada.odometroInicial + stats.kmRodados)

        prefs.edit()
            .putBoolean(RideAccessibilityService.PREF_MONITORAMENTO_ATIVO, false)
            .putLong(RideAccessibilityService.PREF_INICIO_SESSAO, 0L)
            .apply()

        Toast.makeText(this, "Jornada encerrada — ganho R$ %.2f".format(stats.ganhoBruto), Toast.LENGTH_LONG).show()
        atualizarTelaJornada()
    }

    private fun atualizarTelaJornada() {
        val grupoNovaJornada = findViewById<android.view.View>(R.id.grupoNovaJornada)
        val grupoAndamento = findViewById<android.view.View>(R.id.grupoJornadaAndamento)

        val jornada = JornadaStorage.jornadaAtiva(this)
        if (jornada == null) {
            grupoNovaJornada.visibility = android.view.View.VISIBLE
            grupoAndamento.visibility = android.view.View.GONE
            atualizarPreviewMetaPorHora()
        } else {
            grupoNovaJornada.visibility = android.view.View.GONE
            grupoAndamento.visibility = android.view.View.VISIBLE

            val stats = JornadaStorage.calcularStats(this, jornada)
            val horas = stats.tempoTrabalhadoMin / 60
            val minutos = stats.tempoTrabalhadoMin % 60

            findViewById<TextView>(R.id.txtJornadaTempo).text = "%02d:%02d".format(horas, minutos)
            findViewById<TextView>(R.id.txtJornadaMeta).text = "Meta: R$ %.0f (%.0f%% atingida)".format(jornada.metaDiaria, stats.percentualMeta)
            findViewById<TextView>(R.id.txtJornadaGanho).text = "R$ %.2f".format(stats.ganhoBruto)
            findViewById<TextView>(R.id.txtJornadaRPorHora).text = "R$ %.2f".format(stats.valorPorHora)
            findViewById<TextView>(R.id.txtJornadaKm).text = "%.1f".format(stats.kmRodados)
        }

        atualizarBotaoGravar()
    }

    private fun atualizarTrial() {
        val cardAviso = findViewById<android.view.View>(R.id.cardAviso)
        val txtTeste = findViewById<TextView>(R.id.txtTeste)

        if (TrialManager.expirou(this)) {
            cardAviso.visibility = android.view.View.VISIBLE
            txtTeste.text = "⛔ Monitoramento desativado. Entre em contato pra reativar."
            prefs.edit().putBoolean(RideAccessibilityService.PREF_MONITORAMENTO_ATIVO, false).apply()
        } else {
            cardAviso.visibility = android.view.View.GONE
        }
    }

    private fun atualizarBotaoGravar() {
        val btnGravar = findViewById<TextView>(R.id.btnGravar)
        if (RideRecorderService.emGravacao) {
            btnGravar.text = "⏺️ Gravação iniciada — clique para encerrar"
            btnGravar.setTextColor(Color.parseColor("#1FE7A0"))
        } else {
            btnGravar.text = "🔴 Iniciar gravação da corrida"
            btnGravar.setTextColor(Color.parseColor("#F55757"))
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
                Toast.makeText(this, "Permissão de câmera/microfone é necessária para gravar", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun iniciarServicoDeGravacao() {
        val intent = Intent(this, RideRecorderService::class.java).apply { action = RideRecorderService.ACTION_START }
        ContextCompat.startForegroundService(this, intent)
    }
}
