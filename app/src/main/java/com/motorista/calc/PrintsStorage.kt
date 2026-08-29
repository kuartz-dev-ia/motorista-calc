package com.motorista.calc

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrintsStorage {
    private val formatoPasta = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    private val formatoArquivo = SimpleDateFormat("HHmmss_SSS", Locale.getDefault())

    fun pastaBase(context: Context): File {
        val base = File(context.getExternalFilesDir(null), "prints")
        if (!base.exists()) base.mkdirs()
        return base
    }

    fun salvar(context: Context, bitmap: Bitmap) {
        try {
            val agora = Date()
            val subPasta = File(pastaBase(context), formatoPasta.format(agora))
            if (!subPasta.exists()) subPasta.mkdirs()
            val arquivo = File(subPasta, "corrida_${formatoArquivo.format(agora)}.png")
            FileOutputStream(arquivo).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        } catch (e: Exception) {
            // ignora falha ao salvar print, não é crítico
        }
    }

    fun listarTodos(context: Context): List<File> {
        val base = pastaBase(context)
        val lista = mutableListOf<File>()
        base.walkTopDown().forEach { f ->
            if (f.isFile && f.extension.equals("png", ignoreCase = true)) lista.add(f)
        }
        return lista.sortedByDescending { it.lastModified() }
    }

    /** Reconstrói "yyyy-MM-dd" a partir da estrutura de pastas prints/yyyy/MM/dd/arquivo.png */
    fun dataDoArquivo(file: File): String {
        val dd = file.parentFile?.name ?: "??"
        val mm = file.parentFile?.parentFile?.name ?: "??"
        val yyyy = file.parentFile?.parentFile?.parentFile?.name ?: "????"
        return "$yyyy-$mm-$dd"
    }

    fun apagar(file: File): Boolean = try {
        file.delete()
    } catch (e: Exception) {
        false
    }

    fun apagarTodosDoDia(context: Context, dataChave: String) {
        listarTodos(context).filter { dataDoArquivo(it) == dataChave }.forEach { apagar(it) }
    }
}
