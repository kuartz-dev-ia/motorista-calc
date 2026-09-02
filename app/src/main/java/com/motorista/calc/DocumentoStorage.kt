package com.motorista.calc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Documento(
    val id: Long,
    val tipo: String,
    val dataVencimentoMillis: Long
)

object DocumentoStorage {
    private const val PREFS_NAME = "motorista_calc_documentos"
    private const val CHAVE_LISTA = "lista_documentos"
    const val DIAS_ANTECEDENCIA_ALERTA = 15

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun listarTodos(context: Context): List<Documento> {
        val json = prefs(context).getString(CHAVE_LISTA, null) ?: return emptyList()
        val array = try { JSONArray(json) } catch (e: Exception) { return emptyList() }
        val lista = mutableListOf<Documento>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            lista.add(Documento(obj.getLong("id"), obj.getString("tipo"), obj.getLong("dataVencimentoMillis")))
        }
        return lista
    }

    private fun salvarTudo(context: Context, lista: List<Documento>) {
        val array = JSONArray()
        for (d in lista) {
            val obj = JSONObject()
            obj.put("id", d.id)
            obj.put("tipo", d.tipo)
            obj.put("dataVencimentoMillis", d.dataVencimentoMillis)
            array.put(obj)
        }
        prefs(context).edit().putString(CHAVE_LISTA, array.toString()).apply()
    }

    fun adicionar(context: Context, tipo: String, dataVencimentoMillis: Long) {
        val lista = listarTodos(context).toMutableList()
        lista.add(Documento(System.currentTimeMillis(), tipo, dataVencimentoMillis))
        salvarTudo(context, lista)
    }

    fun apagar(context: Context, id: Long) {
        salvarTudo(context, listarTodos(context).filter { it.id != id })
    }

    fun diasRestantes(documento: Documento): Long {
        val diffMillis = documento.dataVencimentoMillis - System.currentTimeMillis()
        return diffMillis / (24L * 60 * 60 * 1000)
    }

    /** Documentos vencidos ou que vencem dentro do prazo de alerta. */
    fun pendencias(context: Context): List<Documento> {
        return listarTodos(context).filter { diasRestantes(it) <= DIAS_ANTECEDENCIA_ALERTA }
    }
}
