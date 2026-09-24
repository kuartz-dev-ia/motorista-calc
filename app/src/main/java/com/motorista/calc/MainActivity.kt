package com.motorista.calc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    private val handler = Handler(Looper.getMainLooper())
    private val tickerRunnable = object : Runnable {
        override fun run() {
            atualizarTelaJornada()
            handler.postDelayed(this, 1_000L)
        }
    }

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
        findViewById<android.view.View>(R.id.btnEncerrarJornada).setOnClickListener {
            startActivity(Intent(this, EncerrarJornadaActivity::class.java))
        }
        findViewById<android.view.View>(R.id.btnGravar).setOnClickListener { alternarGravacao() }
        findViewById<android.view.View>(R.id.btnVerCorridas).setOnClickListener {
            startActivity(Intent(this, CorridasDaJornadaActivity::class.java))
        }
        findViewById<android.view.View>(R.id.btnAdicionarCorridaHome).setOnClickListener {
            startActivity(Intent(this, AdicionarCorridaActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnAbrirMaisOpcoes).setOnClickListener {
            startActivity(Intent(this, MaisOpcoesActivity::class.java))
        }

        findViewById<android.view.View>(R.id.navInicio).setOnClickListener { }
        findViewById<android.view.View>(R.id.navRelatorios).setOnClickListener { startActivity(Intent(this, WeeklyActivity::class.java)) }
        findViewById<android.view.View>(R.id.navHistorico).setOnClickListener { startActivity(Intent(this, HistoricoActivity::class.java)) }
        findViewById<android.view.View>(R.id.navConfig).setOnClickListener { startActivity(Intent(this, ParametrosActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        LicenseManager.verificarEmSegundoPlano(this)
        atualizarTrial()
        atualizarTelaJornada()
        atualizarMetaBrutaNecessaria()
        RideRecorderService.aoMudarEstado = { atualizarBotaoGravar() }
        handler.removeCallbacks(tickerRunnable)
        handler.postDelayed(tickerRunnable, 1_000L)
    }

    override fun onPause() {
        super.onPause()
        RideRecorderService.aoMudarEstado = null
        handler.removeCallbacks(tickerRunnable)
    }

    private fun atualizarMetaBrutaNecessaria() {
        val resultado = MetaMensalCalculator.calcular(this)
        val txtValor = findViewById<TextView>(R.id.txtMetaBrutaNecessaria)
        val txtSubtitulo = findViewById<TextView>(R.id.txtMetaBrutaSubtitulo)

        txtValor.text = "R$ %.2f/dia".format(resultado.metaBrutaDiaria)

        val diferenca = resultado.metaBrutaDiariaBase - resultado.metaBrutaDiaria
        val comparativo = when {
            resultado.ganhoAcumuladoMes <= 0.0 -> ""
            diferenca > 0.5 -> " • você está adiantado, R$ %.2f a menos que a meta base".format(diferenca)
            diferenca < -0.5 -> " • você está atrasado, R$ %.2f a mais que a meta base".format(-diferenca)
            else -> " • em dia com a meta base"
        }
        txtSubtitulo.text = "pra cobrir R$ %.2f/mês em %d dia(s) restantes%s".format(resultado.custoMensalTotal, resultado.diasRestantes, comparativo)
    }

    private fun selecionarChipMeta(selecionado: TextView, valor: String) {
        for ((chip, _) in chipsMeta) {
            chip.background = ContextCompat.getDrawable(this, if (chip == selecionado) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            chip.setTextColor(if (chip == selecionado) Color.parseColor("#06231F") else Color.parseColor("#8A94A3"))
        }
        edtMetaDiaria.setText(valor)
    }

    private fun selecionarChipCarga(selecionado: TextView, valor: String) {
        for ((chip, _) in chipsCarga) {
            chip.background = ContextCompat.getDrawable(this, if (chip == selecionado) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            chip.setTextColor(if (chip == selecionado) Color.parseColor("#06231F") else Color.parseColor("#8A94A3"))
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
            .putLong(RideAccessibilityService.PREF_ULTIMO_LEMBRETE_META, System.currentTimeMillis())
            .apply()

        if (android.provider.Settings.canDrawOverlays(this)) {
            ContextCompat.startForegroundService(this, Intent(this, ChatHeadService::class.java))
        }

        atualizarTelaJornada()
    }

    private fun atualizarTelaJornada() {
        val grupoNovaJornada = findViewById<android.view.View>(R.id.grupoNovaJornada)
        val grupoAndamento = findViewById<android.view.View>(R.id.grupoJornadaAndamento)
        val txtStatusTopo = findViewById<TextView>(R.id.txtStatusTopo)

        val jornada = JornadaStorage.jornadaAtiva(this)
        if (jornada == null) {
            grupoNovaJornada.visibility = android.view.View.VISIBLE
            grupoAndamento.visibility = android.view.View.GONE
            txtStatusTopo.text = "Nenhuma jornada ativa"
            atualizarPreviewMetaPorHora()
            atualizarMetaBrutaNecessaria()
        } else {
            grupoNovaJornada.visibility = android.view.View.GONE
            grupoAndamento.visibility = android.view.View.VISIBLE

            val segundosTotais = (System.currentTimeMillis() - jornada.dataInicioMillis) / 1000
            val horas = segundosTotais / 3600
            val minutos = (segundosTotais % 3600) / 60
            val segundos = segundosTotais % 60

            txtStatusTopo.text = "Jornada ativa · %02d:%02d:%02d".format(horas, minutos, segundos)

            val stats = JornadaStorage.calcularStats(this, jornada)

            findViewById<TextView>(R.id.txtJornadaMeta).text = "  da meta de R$%.0f".format(jornada.metaDiaria)
            findViewById<TextView>(R.id.txtJornadaPercentual).text = "↑ %.0f%%".format(stats.percentualMeta)
            findViewById<TextView>(R.id.txtJornadaGanho).text = "R$%.0f".format(stats.ganhoBruto)
            findViewById<TextView>(R.id.txtJornadaRPorHora).text = "R$%.0f".format(stats.valorPorHora)
            findViewById<TextView>(R.id.txtJornadaRPorKm).text = "R$%.2f".format(stats.valorPorKm)
            findViewById<TextView>(R.id.txtJornadaCombustivel).text = "R$%.0f".format(stats.custoCombustivel)

            val txtLucro = findViewById<TextView>(R.id.txtJornadaLucro)
            txtLucro.text = "R$ %.2f".format(stats.lucroLiquido)
            txtLucro.setTextColor(Color.parseColor(if (stats.lucroLiquido >= 0) "#F2F4F7" else "#C9807E"))

            val resultadoMeta = MetaMensalCalculator.calcular(this)
            findViewById<TextView>(R.id.txtMetaBrutaHoje).text = "R$ %.2f".format(resultadoMeta.metaBrutaDiaria)
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
        val btnGravar = findViewById<android.view.View>(R.id.btnGravar)
        val iconeGravar = findViewById<TextView>(R.id.iconeGravar)
        val labelGravar = findViewById<TextView>(R.id.labelGravar)
        if (RideRecorderService.emGravacao) {
            iconeGravar.text = "⏺️"
            labelGravar.text = "Gravando… toque p/ parar"
            btnGravar.background = ContextCompat.getDrawable(this, R.drawable.bg_cta_stop)
            labelGravar.setTextColor(Color.parseColor("#C9807E"))
        } else {
            iconeGravar.text = "🔴"
            labelGravar.text = "Gravar corrida"
            btnGravar.background = ContextCompat.getDrawable(this, R.drawable.bg_cta_start)
            labelGravar.setTextColor(Color.parseColor("#06231F"))
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
