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
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

class ParametrosActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences

    private lateinit var chipGasolina: TextView
    private lateinit var chipEtanol: TextView
    private lateinit var chipGnv: TextView
    private lateinit var edtPrecoCombustivel: EditText
    private lateinit var edtConsumo: EditText
    private lateinit var txtCustoPorKmPreview: TextView
    private lateinit var txtResumoCombustiveis: TextView

    private var combustivelSelecionado: String = "etanol"

    private var contadorToquesTitulo = 0
    private var ultimoToqueTituloEm = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parametros)

        prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)

        chipGasolina = findViewById(R.id.chipGasolina)
        chipEtanol = findViewById(R.id.chipEtanol)
        chipGnv = findViewById(R.id.chipGnv)
        edtPrecoCombustivel = findViewById(R.id.edtPrecoCombustivel)
        edtConsumo = findViewById(R.id.edtConsumo)
        txtCustoPorKmPreview = findViewById(R.id.txtCustoPorKmPreview)
        txtResumoCombustiveis = findViewById(R.id.txtResumoCombustiveis)

        val edtChaveLicenca = findViewById<EditText>(R.id.edtChaveLicenca)
        val btnAtivarLicenca = findViewById<TextView>(R.id.btnAtivarLicenca)
        val txtTituloConfig = findViewById<TextView>(R.id.txtTituloConfig)
        val cardGeradorChaveDev = findViewById<android.view.View>(R.id.cardGeradorChaveDev)
        val txtChaveGerada = findViewById<TextView>(R.id.txtChaveGerada)
        val btnGerarChave = findViewById<TextView>(R.id.btnGerarChave)
        val btnCopiarChaveGerada = findViewById<TextView>(R.id.btnCopiarChaveGerada)

        val btnAtivarAcessibilidade = findViewById<TextView>(R.id.btnAtivarAcessibilidade)
        val btnPermitirOverlay = findViewById<TextView>(R.id.btnPermitirOverlay)
        val btnVerDebugOcr = findViewById<TextView>(R.id.btnVerDebugOcr)
        val edtMinKm = findViewById<EditText>(R.id.edtMinKm)
        val edtMinHora = findViewById<EditText>(R.id.edtMinHora)
        val edtFinanciamento = findViewById<EditText>(R.id.edtFinanciamento)
        val edtSeguro = findViewById<EditText>(R.id.edtSeguro)
        val edtIpva = findViewById<EditText>(R.id.edtIpva)
        val edtLicenciamento = findViewById<EditText>(R.id.edtLicenciamento)
        val edtManutencao = findViewById<EditText>(R.id.edtManutencao)
        val edtContasPessoais = findViewById<EditText>(R.id.edtContasPessoais)
        val edtKmMes = findViewById<EditText>(R.id.edtKmMes)
        val edtLimitePausa = findViewById<EditText>(R.id.edtLimitePausa)
        val edtMetaSemanal = findViewById<EditText>(R.id.edtMetaSemanal)
        val edtMetaMensal = findViewById<EditText>(R.id.edtMetaMensal)
        val btnSalvar = findViewById<TextView>(R.id.btnSalvar)
        val btnSalvarCombustivel = findViewById<TextView>(R.id.btnSalvarCombustivel)
        val txtStatus = findViewById<TextView>(R.id.txtStatus)

        findViewById<TextView>(R.id.txtIdDispositivo).text = "ID do aparelho: ${LicenseManager.obterIdDispositivo(this)}"

        combustivelSelecionado = prefs.getString(RideAccessibilityService.PREF_COMBUSTIVEL_ATIVO, "etanol") ?: "etanol"
        selecionarPill(combustivelSelecionado, carregarCampos = true)

        preencherSeExistir(edtMinKm, RideAccessibilityService.PREF_MIN_KM)
        preencherSeExistir(edtMinHora, RideAccessibilityService.PREF_MIN_HORA)
        preencherSeExistir(edtFinanciamento, RideAccessibilityService.PREF_FINANCIAMENTO)
        preencherSeExistir(edtSeguro, RideAccessibilityService.PREF_SEGURO)
        preencherSeExistir(edtIpva, RideAccessibilityService.PREF_IPVA)
        preencherSeExistir(edtLicenciamento, RideAccessibilityService.PREF_LICENCIAMENTO)
        preencherSeExistir(edtManutencao, RideAccessibilityService.PREF_MANUTENCAO)
        preencherSeExistir(edtContasPessoais, RideAccessibilityService.PREF_CONTAS_PESSOAIS)
        preencherSeExistir(edtKmMes, RideAccessibilityService.PREF_KM_MES)
        preencherSeExistir(edtLimitePausa, RideAccessibilityService.PREF_LIMITE_PAUSA_HORAS)
        preencherSeExistir(edtMetaSemanal, RideAccessibilityService.PREF_META_SEMANAL)
        preencherSeExistir(edtMetaMensal, RideAccessibilityService.PREF_META_MENSAL)

        val watcherPreview = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { atualizarPreviewCustoPorKm() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        edtPrecoCombustivel.addTextChangedListener(watcherPreview)
        edtConsumo.addTextChangedListener(watcherPreview)

        chipGasolina.setOnClickListener { trocarCombustivel("gasolina") }
        chipEtanol.setOnClickListener { trocarCombustivel("etanol") }
        chipGnv.setOnClickListener { trocarCombustivel("gnv") }

        btnAtivarAcessibilidade.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnPermitirOverlay.setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }

        btnVerDebugOcr.setOnClickListener { mostrarDebugOcr() }

        findViewById<android.view.View>(R.id.btnAbrirBackup).setOnClickListener {
            startActivity(Intent(this, BackupActivity::class.java))
        }

        btnAtivarLicenca.setOnClickListener {
            val chave = edtChaveLicenca.text.toString()
            if (chave.isBlank()) {
                Toast.makeText(this, "Digite a chave de licença", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(this, "Esse campo é do sistema antigo de chave local — a liberação agora é feita remotamente pelo ID do aparelho acima.", Toast.LENGTH_LONG).show()
        }

        // Toca 7x seguidas no título "Config" pra revelar o gerador de
        // chaves — não é visível pro usuário comum, só pra você.
        txtTituloConfig.setOnClickListener {
            val agora = System.currentTimeMillis()
            if (agora - ultimoToqueTituloEm > 2000) contadorToquesTitulo = 0
            ultimoToqueTituloEm = agora
            contadorToquesTitulo++
            if (contadorToquesTitulo >= 7) {
                contadorToquesTitulo = 0
                cardGeradorChaveDev.visibility = android.view.View.VISIBLE
                Toast.makeText(this, "Painel interno liberado", Toast.LENGTH_SHORT).show()
            }
        }

        btnGerarChave.setOnClickListener {
            txtChaveGerada.text = "Sistema migrado pro Firebase — use o ID do aparelho pra liberar."
        }

        btnCopiarChaveGerada.setOnClickListener {
            val id = LicenseManager.obterIdDispositivo(this)
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("id_dispositivo", id))
            Toast.makeText(this, "ID do aparelho copiado!", Toast.LENGTH_SHORT).show()
        }

        val acaoSalvarCombustivel = {
            salvarCamposDoCombustivel(combustivelSelecionado)
            prefs.edit().putString(RideAccessibilityService.PREF_COMBUSTIVEL_ATIVO, combustivelSelecionado).apply()
            atualizarResumoCombustiveis()
            Toast.makeText(this, "Combustível salvo", Toast.LENGTH_SHORT).show()
        }
        btnSalvarCombustivel.setOnClickListener { acaoSalvarCombustivel() }

        btnSalvar.setOnClickListener {
            acaoSalvarCombustivel()
            prefs.edit().apply {
                putFloat(RideAccessibilityService.PREF_MIN_KM, edtMinKm.text.toString().toFloatOrNull() ?: 1.50f)
                putFloat(RideAccessibilityService.PREF_MIN_HORA, edtMinHora.text.toString().toFloatOrNull() ?: 25.0f)
                putFloat(RideAccessibilityService.PREF_FINANCIAMENTO, edtFinanciamento.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_SEGURO, edtSeguro.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_IPVA, edtIpva.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_LICENCIAMENTO, edtLicenciamento.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_MANUTENCAO, edtManutencao.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_CONTAS_PESSOAIS, edtContasPessoais.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_KM_MES, edtKmMes.text.toString().toFloatOrNull() ?: 3000f)
                putFloat(RideAccessibilityService.PREF_LIMITE_PAUSA_HORAS, edtLimitePausa.text.toString().toFloatOrNull() ?: 3.0f)
                putFloat(RideAccessibilityService.PREF_META_SEMANAL, edtMetaSemanal.text.toString().toFloatOrNull() ?: 0f)
                putFloat(RideAccessibilityService.PREF_META_MENSAL, edtMetaMensal.text.toString().toFloatOrNull() ?: 0f)
                apply()
            }
            Toast.makeText(this, "Configurações salvas", Toast.LENGTH_SHORT).show()
        }

        atualizarStatus(txtStatus)
        atualizarResumoCombustiveis()
        atualizarStatusLicenca()
    }

    private fun atualizarStatusLicenca() {
        val txtStatusLicenca = findViewById<TextView>(R.id.txtStatusLicenca)
        if (LicenseManager.estaLiberadoPorLicenca(this)) {
            txtStatusLicenca.text = "✔ Licença ativada — uso liberado"
            txtStatusLicenca.setTextColor(Color.parseColor("#2FB4A6"))
        } else {
            val dias = TrialManager.diasRestantes(this)
            if (dias > 0) {
                txtStatusLicenca.text = "Teste gratuito — $dias dia(s) restante(s)"
                txtStatusLicenca.setTextColor(Color.parseColor("#8A94A3"))
            } else {
                txtStatusLicenca.text = "⛔ Teste gratuito encerrado — envie o ID do aparelho abaixo pra liberar"
                txtStatusLicenca.setTextColor(Color.parseColor("#C9807E"))
            }
        }
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
                Toast.makeText(this, "Copiado! Cole numa mensagem pra me enviar.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Fechar", null)
            .show()
        DialogUtils.aplicarCoresBotoes(dialog)
    }

    private fun trocarCombustivel(novo: String) {
        salvarCamposDoCombustivel(combustivelSelecionado)
        selecionarPill(novo, carregarCampos = true)
    }

    private fun selecionarPill(tipo: String, carregarCampos: Boolean) {
        combustivelSelecionado = tipo

        val pills = mapOf("gasolina" to chipGasolina, "etanol" to chipEtanol, "gnv" to chipGnv)
        for ((chaveTipo, chip) in pills) {
            val selecionado = chaveTipo == tipo
            chip.background = ContextCompat.getDrawable(this, if (selecionado) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            chip.setTextColor(if (selecionado) Color.parseColor("#06231F") else Color.parseColor("#8A94A3"))
            chip.setTypeface(chip.typeface, if (selecionado) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }

        if (carregarCampos) {
            val (preco, consumo) = obterValoresPadrao(tipo)
            edtPrecoCombustivel.setText(prefs.getFloat(chavePreco(tipo), preco).toString())
            edtConsumo.setText(prefs.getFloat(chaveConsumo(tipo), consumo).toString())
        }
        atualizarPreviewCustoPorKm()
    }

    private fun obterValoresPadrao(tipo: String): Pair<Float, Float> = when (tipo) {
        "gasolina" -> Pair(6.10f, 10.0f)
        "gnv" -> Pair(4.50f, 12.0f)
        else -> Pair(4.20f, 7.0f)
    }

    private fun chavePreco(tipo: String) = when (tipo) {
        "gasolina" -> RideAccessibilityService.PREF_PRECO_GASOLINA
        "gnv" -> RideAccessibilityService.PREF_PRECO_GNV
        else -> RideAccessibilityService.PREF_PRECO_ETANOL
    }

    private fun chaveConsumo(tipo: String) = when (tipo) {
        "gasolina" -> RideAccessibilityService.PREF_CONSUMO_GASOLINA
        "gnv" -> RideAccessibilityService.PREF_CONSUMO_GNV
        else -> RideAccessibilityService.PREF_CONSUMO_ETANOL
    }

    private fun salvarCamposDoCombustivel(tipo: String) {
        val preco = edtPrecoCombustivel.text.toString().toFloatOrNull() ?: return
        val consumo = edtConsumo.text.toString().toFloatOrNull() ?: return
        prefs.edit()
            .putFloat(chavePreco(tipo), preco)
            .putFloat(chaveConsumo(tipo), consumo)
            .apply()
    }

    private fun atualizarPreviewCustoPorKm() {
        val preco = edtPrecoCombustivel.text.toString().toDoubleOrNull() ?: 0.0
        val consumo = edtConsumo.text.toString().toDoubleOrNull() ?: 0.0
        val custoPorKm = if (consumo > 0) preco / consumo else 0.0
        txtCustoPorKmPreview.text = "R$ %.2f/km".format(custoPorKm)
    }

    private fun atualizarResumoCombustiveis() {
        val tipos = listOf("gasolina" to "Gasolina", "etanol" to "Etanol", "gnv" to "GNV")
        val linhas = tipos.map { (chave, nome) ->
            val (precoPadrao, consumoPadrao) = obterValoresPadrao(chave)
            val preco = prefs.getFloat(chavePreco(chave), precoPadrao)
            val consumo = prefs.getFloat(chaveConsumo(chave), consumoPadrao)
            val custo = if (consumo > 0) preco / consumo else 0.0
            "$nome — R$ %.2f/km".format(custo)
        }
        txtResumoCombustiveis.text = linhas.joinToString("\n")
    }

    private fun preencherSeExistir(campo: EditText, chave: String) {
        if (prefs.contains(chave)) {
            campo.setText(prefs.getFloat(chave, 0f).toString())
        }
    }

    override fun onResume() {
        super.onResume()
        LicenseManager.verificarEmSegundoPlano(this)
        findViewById<TextView>(R.id.txtStatus)?.let { atualizarStatus(it) }
        atualizarStatusLicenca()
    }

    private fun atualizarStatus(txtStatus: TextView) {
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
