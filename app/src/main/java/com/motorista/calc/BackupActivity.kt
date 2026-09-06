package com.motorista.calc

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider

class BackupActivity : AppCompatActivity() {

    private val CODIGO_IMPORTAR = 601

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)

        findViewById<TextView>(R.id.btnExportarBackup).setOnClickListener { exportar() }
        findViewById<TextView>(R.id.btnImportarBackup).setOnClickListener { escolherArquivoParaImportar() }
    }

    private fun exportar() {
        try {
            val arquivo = BackupManager.exportar(this)
            val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", arquivo)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Salvar backup (Drive, Gmail, etc.)"))
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao exportar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun escolherArquivoParaImportar() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, CODIGO_IMPORTAR)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CODIGO_IMPORTAR && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val conteudo = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (conteudo == null) {
                    Toast.makeText(this, "Não consegui ler o arquivo", Toast.LENGTH_LONG).show()
                    return
                }
                val sucesso = BackupManager.importar(this, conteudo)
                if (sucesso) {
                    Toast.makeText(this, "Backup restaurado! Feche e abra o app de novo.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Arquivo de backup inválido", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao importar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
