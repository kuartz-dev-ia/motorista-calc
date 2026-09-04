package com.motorista.calc

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EncerrarJornadaActivity : AppCompatActivity() {

    private lateinit var edtValorUber: EditText
    private lateinit var edtValor99: EditText
    private lateinit var edtKmTotal: EditText
    private lateinit var txtTotalPreview: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encerrar_jornada)

        edtValorUber = findViewById(R.id.edtValorUberFinal)
        edtValor99 = findViewById(R.id.edtValor99Final)
        edtKmTotal = findViewById(R.id.edtKmTotalFinal)
        txtTotalPreview = findViewById(R.id.txtTotalPreview)

        val watcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { atualizarPreview() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        edtValorUber.addTextChangedListener(watcher)
        edtValor99.addTextChangedListener(watcher)

        findViewById<TextView>(R.id.btnConfirmarEncerramento).setOnClickListener { confirmarEncerramento() }
        findViewById<TextView>(R.id.btnCancelarEncerramento).setOnClickListener { finish() }
    }

    private fun atualizarPreview() {
        val uber = edtValorUber.text.toString().toDoubleOrNull() ?: 0.0
        val n99 = edtValor99.text.toString().toDoubleOrNull() ?: 0.0
        txtTotalPreview.text = "R$ %.2f".format(uber + n99)
    }

    private fun confirmarEncerramento() {
        val jornada = JornadaStorage.jornadaAtiva(this)
        if (jornada == null) {
            Toast.makeText(this, "Nenhuma jornada em andamento.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val valorUber = edtValorUber.text.toString().toDoubleOrNull()
        val valor99 = edtValor99.text.toString().toDoubleOrNull()
        val kmTotal = edtKmTotal.text.toString().toDoubleOrNull()

        if (valorUber == null || valorUber < 0 || valor99 == null || valor99 < 0 || kmTotal == null || kmTotal <= 0) {
            Toast.makeText(this, "Preencha os 3 campos corretamente (km precisa ser maior que 0)", Toast.LENGTH_LONG).show()
            return
        }

        JornadaStorage.encerrarComResumo(this, jornada.id, valorUber, valor99, kmTotal)

        val prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .putBoolean(RideAccessibilityService.PREF_MONITORAMENTO_ATIVO, false)
            .putLong(RideAccessibilityService.PREF_INICIO_SESSAO, 0L)
            .apply()

        val jornadaAtualizada = JornadaStorage.listarTodas(this).first { it.id == jornada.id }
        val stats = JornadaStorage.calcularStats(this, jornadaAtualizada)

        Toast.makeText(
            this,
            "Jornada encerrada — ganho R$ %.2f, lucro líquido R$ %.2f".format(stats.ganhoBruto, stats.lucroLiquido),
            Toast.LENGTH_LONG
        ).show()

        finish()
    }
}
