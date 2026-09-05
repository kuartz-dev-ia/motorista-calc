package com.motorista.calc

import android.Manifest
import android.app.AlertDialog
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
            handler.postDelayed(this, 15_000L)
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
        findViewById<TextView>(R.id.btnEncerrarJornada).setOnClickListener {
            startActivity(Intent(this, EncerrarJornadaActivity::class.java))
        }
        findViewById<TextView>(R.id.btnGravar).setOnClickListener { alternarGravacao() }
        findViewById<TextView>(R.id.btnVerCorridas).setOnClickListener {
            startActivity(Intent(this, CorridasDaJornadaActivity::class.java))
        }
        findViewById<TextView>(R.id.btnAdicionarCorridaHome).setOnClickListener {
            startActivity(Intent(this, AdicionarCorridaActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnAbrirMaisOpcoes).setOnClickListener {
            startActivity(Intent(this, MaisOpcoesActivity::class.java))
        }
        findViewById<TextView>(R.id.btnVerDicas).setOnClickListener { mostrarDicas() }

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
        handler.removeCallbacks(tickerRunnable)
        handler.postDelayed(tickerRunnable, 15_000L)
    }

    override fun onPause() {
        super.onPause()
        RideRecorderService.aoMudarEstado = null
        handler.removeCallbacks(tickerRunnable)
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
            .putLong(RideAccessibilityService.PREF_ULTIMO_LEMBRETE_META, System.currentTimeMillis())
            .apply()

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
            findViewById<TextView>(R.id.txtJornadaMeta).text = "R$%.0f".format(jornada.metaDiaria)
            findViewById<TextView>(R.id.txtJornadaPercentual).text = "%.0f%%".format(stats.percentualMeta)
            findViewById<TextView>(R.id.txtJornadaGanho).text = "R$ %.2f".format(stats.ganhoBruto)
            findViewById<TextView>(R.id.txtJornadaRPorHora).text = "R$ %.2f".format(stats.valorPorHora)
            findViewById<TextView>(R.id.txtJornadaKm).text = "%.1f".format(stats.kmRodados)
            findViewById<TextView>(R.id.txtJornadaCombustivel).text = "R$ %.2f".format(stats.custoCombustivel)

            val txtLucro = findViewById<TextView>(R.id.txtJornadaLucro)
            txtLucro.text = "R$ %.2f".format(stats.lucroLiquido)
            txtLucro.setTextColor(Color.parseColor(if (stats.lucroLiquido >= 0) "#1FE7A0" else "#F55757"))

            val anel = findViewById<android.view.View>(R.id.anelMeta)
            anel.setBackgroundResource(if (stats.percentualMeta >= 100) R.drawable.bg_ring_progress else R.drawable.bg_ring_progress_baixo)
        }

        atualizarBotaoGravar()
    }

    private fun mostrarDicas() {
        val dicas = mutableListOf<String>()

        val jornadaAtiva = JornadaStorage.jornadaAtiva(this)
        val ultimaJornada = jornadaAtiva ?: JornadaStorage.listarTodas(this).firstOrNull { it.dataFimMillis != null }

        if (ultimaJornada != null) {
            val stats = JornadaStorage.calcularStats(this, ultimaJornada)
            if (stats.ganhoBruto > 0) {
                val percentualCombustivel = (stats.custoCombustivel / stats.ganhoBruto) * 100
                if (percentualCombustivel > 25) {
                    dicas.add("⛽ O combustível está consumindo %.0f%% do seu ganho bruto — considere revisar o consumo do carro ou o combustível usado em Config.".format(percentualCombustivel))
                }
            }
            if (stats.valorPorHora > 0 && stats.valorPorHora < 20) {
                dicas.add("🕐 Seu R$/hora está em R$ %.2f, abaixo do recomendado. Avalie evitar corridas muito longas em horários de trânsito parado.".format(stats.valorPorHora))
            }
        }

        val consumoMedio = AbastecimentoStorage.consumoMedio(AbastecimentoStorage.listarTodos(this))
        if (consumoMedio != null) {
            dicas.add("📊 Seu consumo real medido é de %.1f km/l — confira se esse valor está atualizado em Config > Combustível.".format(consumoMedio))
        }

        val manutencoesPendentes = ManutencaoStorage.pendencias(this)
        if (manutencoesPendentes.isNotEmpty()) {
            dicas.add("🔧 Você tem ${manutencoesPendentes.size} manutenção(ões) pendente(s) — resolver isso evita gastos maiores depois.")
        }

        val documentosPendentes = DocumentoStorage.pendencias(this)
        if (documentosPendentes.isNotEmpty()) {
            dicas.add("📄 Tem documento(s) vencendo ou vencido(s) — dá uma olhada na aba Documentos.")
        }

        if (dicas.isEmpty()) {
            dicas.add("✅ Está tudo em ordem por enquanto! Continue registrando corridas, abastecimentos e manutenções pra eu te dar dicas cada vez mais precisas.")
        }

        val dialog = AlertDialog.Builder(this, R.style.DialogTemaEscuro)
            .setTitle("🤖 Dicas do Agente financeiro")
            .setMessage(dicas.joinToString("\n\n"))
            .setPositiveButton("Entendi", null)
            .show()
        DialogUtils.aplicarCoresBotoes(dialog)
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
            btnGravar.text = "⏺️  Gravação iniciada — toque para encerrar"
            btnGravar.background = ContextCompat.getDrawable(this, R.drawable.bg_cta_stop)
        } else {
            btnGravar.text = "▶  Iniciar Gravação da Corrida"
            btnGravar.background = ContextCompat.getDrawable(this, R.drawable.bg_cta_start)
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
