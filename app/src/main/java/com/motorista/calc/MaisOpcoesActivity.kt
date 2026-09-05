package com.motorista.calc

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MaisOpcoesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mais_opcoes)

        findViewById<android.view.View>(R.id.btnManutencoes).setOnClickListener { startActivity(Intent(this, ManutencoesActivity::class.java)) }
        findViewById<android.view.View>(R.id.btnAbastecimentos).setOnClickListener { startActivity(Intent(this, AbastecimentosActivity::class.java)) }
        findViewById<android.view.View>(R.id.btnDocumentos).setOnClickListener { startActivity(Intent(this, DocumentosActivity::class.java)) }
        findViewById<android.view.View>(R.id.btnPrints).setOnClickListener { startActivity(Intent(this, PrintsActivity::class.java)) }
        findViewById<android.view.View>(R.id.btnGravacoes).setOnClickListener { startActivity(Intent(this, RecordingsActivity::class.java)) }
    }
}
