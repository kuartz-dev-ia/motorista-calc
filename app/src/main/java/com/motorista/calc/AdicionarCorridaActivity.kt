package com.motorista.calc

import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class AdicionarCorridaActivity : AppCompatActivity() {

    private lateinit var edtValor: EditText
    private lateinit var edtKm: EditText
    private var plataformaSelecionada = "Outro"

    private val chips by lazy {
        listOf(
            findViewById<TextView>(R.id.chipPlataformaUber) to "Uber",
            findViewById<TextView>(R.id.chipPlataforma99) to "99",
            findViewById<TextView>(R.id.chipPlataformaOutro) to "Outro"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adicionar_corrida)

        if (JornadaStorage.jornadaAtiva(this) == null) {
            Toast.makeText(this, "Só é possível adicionar corridas com uma jornada em andamento.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        edtValor = findViewById(R.id.edtValorCorridaManual)
        edtKm = findViewById(R.id.edtKmCorridaManual)

        for ((chip, valor) in chips) {
            chip.setOnClickListener { selecionarPlataforma(chip, valor) }
        }
        selecionarPlataforma(findViewById(R.id.chipPlataformaOutro), "Outro")

        findViewById<TextView>(R.id.btnSalvarCorridaManual).setOnClickListener { salvar() }
        findViewById<TextView>(R.id.btnCancelarCorridaManual).setOnClickListener { finish() }
    }

    private fun selecionarPlataforma(selecionado: TextView, valor: String) {
        plataformaSelecionada = valor
        for ((chip, _) in chips) {
            chip.background = ContextCompat.getDrawable(this, if (chip == selecionado) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            chip.setTextColor(if (chip == selecionado) Color.parseColor("#08131A") else Color.parseColor("#8B96AC"))
        }
    }

    private fun calcularLucroEstimado(valor: Double, km: Double): Double? {
        if (km <= 0) return valor
        val prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)

        val (preco, consumo) = when (prefs.getString(RideAccessibilityService.PREF_COMBUSTIVEL_ATIVO, "etanol")) {
            "gasolina" -> Pair(prefs.getFloat(RideAccessibilityService.PREF_PRECO_GASOLINA, 6.10f).toDouble(), prefs.getFloat(RideAccessibilityService.PREF_CONSUMO_GASOLINA, 10.0f).toDouble())
            "gnv" -> Pair(prefs.getFloat(RideAccessibilityService.PREF_PRECO_GNV, 4.50f).toDouble(), prefs.getFloat(RideAccessibilityService.PREF_CONSUMO_GNV, 12.0f).toDouble())
            else -> Pair(prefs.getFloat(RideAccessibilityService.PREF_PRECO_ETANOL, 4.20f).toDouble(), prefs.getFloat(RideAccessibilityService.PREF_CONSUMO_ETANOL, 7.0f).toDouble())
        }
        val custoCombustivel = if (consumo > 0) (km / consumo) * preco else 0.0

        val financiamento = prefs.getFloat(RideAccessibilityService.PREF_FINANCIAMENTO, 0f).toDouble()
        val seguro = prefs.getFloat(RideAccessibilityService.PREF_SEGURO, 0f).toDouble()
        val ipvaAnual = prefs.getFloat(RideAccessibilityService.PREF_IPVA, 0f).toDouble()
        val licenciamentoAnual = prefs.getFloat(RideAccessibilityService.PREF_LICENCIAMENTO, 0f).toDouble()
        val manutencao = prefs.getFloat(RideAccessibilityService.PREF_MANUTENCAO, 0f).toDouble()
        val contasPessoais = prefs.getFloat(RideAccessibilityService.PREF_CONTAS_PESSOAIS, 0f).toDouble()
        val kmMes = prefs.getFloat(RideAccessibilityService.PREF_KM_MES, 3000f).toDouble()
        val custoFixoMensal = financiamento + seguro + (ipvaAnual / 12.0) + (licenciamentoAnual / 12.0) + manutencao + contasPessoais
        val custoFixoPorKm = if (kmMes > 0) custoFixoMensal / kmMes else 0.0

        return valor - custoCombustivel - (custoFixoPorKm * km)
    }

    private fun salvar() {
        val valor = edtValor.text.toString().toDoubleOrNull()
        val km = edtKm.text.toString().toDoubleOrNull()

        if (valor == null || valor <= 0 || km == null || km < 0) {
            Toast.makeText(this, "Preencha o valor e o km corretamente", Toast.LENGTH_LONG).show()
            return
        }

        val valorPorKm = if (km > 0) valor / km else null
        val lucro = calcularLucroEstimado(valor, km)

        val (id, _) = HistoricoStorage.adicionarRegistro(
            context = this,
            valorTotal = valor,
            distanciaTotalKm = km,
            tempoTotalMin = 0,
            valorPorKm = valorPorKm,
            valorPorHora = null,
            lucroLiquido = lucro,
            valeAPena = true,
            plataforma = plataformaSelecionada
        )
        HistoricoStorage.marcarAceita(this, id)

        Toast.makeText(this, "Corrida adicionada", Toast.LENGTH_SHORT).show()
        finish()
    }
}
