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
}
