package com.motorista.calc

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {
    private val NOMES_PREFS = listOf(
        RideAccessibilityService.PREFS_NAME,
        "motorista_calc_historico",
        "motorista_calc_jornadas",
        "motorista_calc_abastecimentos",
        "motorista_calc_manutencoes",
        "motorista_calc_documentos"
    )

    fun exportar(context: Context): File {
        val raiz = JSONObject()
        raiz.put("versao", 1)
        raiz.put("dataExportacao", System.currentTimeMillis())

        val prefsJson = JSONObject()
        for (nome in NOMES_PREFS) {
            val prefs = context.getSharedPreferences(nome, Context.MODE_PRIVATE)
            val obj = JSONObject()
            for ((chave, valor) in prefs.all) {
                obj.put(chave, serializarValor(valor))
            }
            prefsJson.put(nome, obj)
        }
        raiz.put("prefs", prefsJson)

        val pasta = File(context.getExternalFilesDir(null), "backup")
        if (!pasta.exists()) pasta.mkdirs()
        val nomeArquivo = "backup_motorista_calc_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".json"
        val arquivo = File(pasta, nomeArquivo)
        arquivo.writeText(raiz.toString())
        return arquivo
    }

    fun importar(context: Context, conteudo: String): Boolean {
        return try {
            val raiz = JSONObject(conteudo)
            val prefsJson = raiz.getJSONObject("prefs")
            for (nome in NOMES_PREFS) {
                if (!prefsJson.has(nome)) continue
                val obj = prefsJson.getJSONObject(nome)
                val prefs = context.getSharedPreferences(nome, Context.MODE_PRIVATE)
                val editor = prefs.edit()
                editor.clear()
                val chaves = obj.keys()
                while (chaves.hasNext()) {
                    val chave = chaves.next()
                    aplicarValor(editor, chave, obj.getJSONObject(chave))
                }
                editor.apply()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun serializarValor(v: Any?): JSONObject {
        val obj = JSONObject()
        when (v) {
            is Boolean -> { obj.put("t", "b"); obj.put("v", v) }
            is Float -> { obj.put("t", "f"); obj.put("v", v.toDouble()) }
            is Long -> { obj.put("t", "l"); obj.put("v", v) }
            is Int -> { obj.put("t", "i"); obj.put("v", v) }
            is String -> { obj.put("t", "s"); obj.put("v", v) }
            else -> { obj.put("t", "s"); obj.put("v", v?.toString() ?: "") }
        }
        return obj
    }

    private fun aplicarValor(editor: android.content.SharedPreferences.Editor, chave: String, obj: JSONObject) {
        when (obj.getString("t")) {
            "b" -> editor.putBoolean(chave, obj.getBoolean("v"))
            "f" -> editor.putFloat(chave, obj.getDouble("v").toFloat())
            "l" -> editor.putLong(chave, obj.getLong("v"))
            "i" -> editor.putInt(chave, obj.getInt("v"))
            else -> editor.putString(chave, obj.getString("v"))
        }
    }
}
