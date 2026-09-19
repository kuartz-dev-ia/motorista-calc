package com.motorista.calc

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MaisOpcoesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_mais_opcoes)

        findViewById<View>(R.id.btnManutencoes).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ManutencoesActivity::class.java
                )
            )
        }

        findViewById<View>(R.id.btnAbastecimentos).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    AbastecimentosActivity::class.java
                )
            )
        }

        findViewById<View>(R.id.btnDocumentos).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    DocumentosActivity::class.java
                )
            )
        }

        findViewById<View>(R.id.btnContas).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ContasActivity::class.java
                )
            )
        }

        findViewById<View>(R.id.btnSaudeVeiculo).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    SaudeVeiculoActivity::class.java
                )
            )
        }

        findViewById<View>(R.id.btnPrints).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    PrintsActivity::class.java
                )
            )
        }

        findViewById<View>(R.id.btnEventos).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    EventosListaActivity::class.java
                )
            )
        }

        findViewById<View>(R.id.btnGravacoes).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    RecordingsActivity::class.java
                )
            )
        }
    }
}
