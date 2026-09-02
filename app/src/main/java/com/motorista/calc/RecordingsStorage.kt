package com.motorista.calc

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

object RecordingsStorage {

    private fun pastaBase(context: Context): File {
        val base = File(context.getExternalFilesDir(null), "gravacoes")
        if (!base.exists()) base.mkdirs()
        return base
    }

    fun novoArquivo(context: Context): File {
        val agora = java.util.Date()
        val pastaDia = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(agora)
        val pasta = File(pastaBase(context), pastaDia)
        if (!pasta.exists()) pasta.mkdirs()
        val nomeArquivo = "corrida_" + SimpleDateFormat("HHmmss_SSS", Locale.getDefault()).format(agora) + ".mp4"
        return File(pasta, nomeArquivo)
    }

    fun listarTodos(context: Context): List<File> {
        val base = pastaBase(context)
        val resultado = mutableListOf<File>()
        base.walkTopDown().forEach { arquivo ->
            if (arquivo.isFile && arquivo.extension == "mp4") resultado.add(arquivo)
        }
        return resultado
    }

    fun dataDoArquivo(arquivo: File): String {
        val partes = arquivo.parentFile?.path?.split(File.separatorChar) ?: return "Sem data"
        return if (partes.size >= 3) {
            "${partes[partes.size - 3]}-${partes[partes.size - 2]}-${partes[partes.size - 1]}"
        } else "Sem data"
    }

    fun apagar(arquivo: File) {
        try { arquivo.delete() } catch (e: Exception) { }
    }

    fun apagarTodosDoDia(context: Context, dataChave: String) {
        listarTodos(context).filter { dataDoArquivo(it) == dataChave }.forEach { apagar(it) }
    }
}
