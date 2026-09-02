package com.motorista.calc

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AbastecimentosActivity : AppCompatActivity() {

    private lateinit var edtLitros: EditText
    private lateinit var edtValor: EditText
    private lateinit var edtKm: EditText
    private lateinit var txtConsumoMedio: TextView
    private lateinit var containerLista: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_abastecimentos)

        edtLitros = findViewById(R.id.edtLitros)
        edtValor = findViewById(R.id.edtValorAbastecimento)
        edtKm = findViewById(R.id.edtKmAtual)
        txtConsumoMedio = findViewById(R.id.txtConsumoMedio)
        containerLista = findViewById(R.id.containerAbastecimentos)

        findViewById<android.view.View>(R.id.btnSalvarAbastecimento).setOnClickListener {
            salvarAbastecimento()
        }

        findViewById<android.view.View>(R.id.btnUsarConsumoReal).setOnClickListener {
            usarConsumoRealNosCalculos()
        }
    }

    override fun onResume() {
        super.onResume()
        atualizarTela()
    }

    private fun salvarAbastecimento() {
        val litros = edtLitros.text.toString().toDoubleOrNull()
        val valor = edtValor.text.toString().toDoubleOrNull()
        val km = edtKm.text.toString().toDoubleOrNull()

        if (litros == null || litros <= 0 || valor == null || valor <= 0 || km == null || km <= 0) {
            Toast.makeText(this, "Preencha litros, valor e km corretamente", Toast.LENGTH_LONG).show()
            return
        }

        AbastecimentoStorage.adicionar(this, litros, valor, km)
        edtLitros.text.clear()
        edtValor.text.clear()
        edtKm.text.clear()
        Toast.makeText(this, "Abastecimento salvo", Toast.LENGTH_SHORT).show()
        atualizarTela()
    }

    private fun usarConsumoRealNosCalculos() {
        val lista = AbastecimentoStorage.listarTodos(this)
        val media = AbastecimentoStorage.consumoMedio(lista)
        if (media == null) {
            Toast.makeText(this, "Ainda não há dados suficientes (registre pelo menos 2 abastecimentos)", Toast.LENGTH_LONG).show()
            return
        }
        val prefs = getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putFloat(RideAccessibilityService.PREF_CONSUMO, media.toFloat()).apply()
        Toast.makeText(this, "Consumo atualizado nos Parâmetros: %.1f km/l".format(media), Toast.LENGTH_LONG).show()
    }

    private fun atualizarTela() {
        val lista = AbastecimentoStorage.listarTodos(this)
        val comConsumo = AbastecimentoStorage.calcularConsumos(lista)
        val media = AbastecimentoStorage.consumoMedio(lista)
        val totalGasto = lista.sumOf { it.valorTotal }

        txtConsumoMedio.text = if (media != null) {
            "Consumo médio real: %.1f km/l  •  Total gasto: R$ %.2f".format(media, totalGasto)
        } else {
            "Registre pelo menos 2 abastecimentos pra calcular o consumo real"
        }

        containerLista.removeAllViews()
        val formato = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

        if (comConsumo.isEmpty()) {
            containerLista.addView(TextView(this).apply {
                text = "Nenhum abastecimento registrado ainda."
                setTextColor(Color.parseColor("#9AA4B2"))
                textSize = 13f
            })
            return
        }

        for ((abastecimento, consumo) in comConsumo) {
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 10, 0, 10)
            }

            val texto = TextView(this).apply {
                text = buildString {
                    append(formato.format(Date(abastecimento.dataHora)))
                    append(" — %.1fL — R$ %.2f — %.0f km".format(abastecimento.litros, abastecimento.valorTotal, abastecimento.kmAtual))
                    if (consumo != null) append("\n➜ %.1f km/l nesse trecho".format(consumo))
                }
                textSize = 12f
                setTextColor(Color.parseColor("#E4E7EC"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            linha.addView(texto)

            val btnApagar = Button(this).apply {
                text = "🗑️"
                textSize = 12f
                setOnClickListener {
                    AbastecimentoStorage.apagar(this@AbastecimentosActivity, abastecimento.id)
                    atualizarTela()
                }
            }
            linha.addView(btnApagar)

            containerLista.addView(linha)
        }
    }
}
