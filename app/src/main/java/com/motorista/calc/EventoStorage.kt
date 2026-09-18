package com.motorista.calc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Evento(
    val id: Long,
    val nome: String,
    val endereco: String,
    val categoria: String,
    val dataTexto: String,
    val horarioInicio: String,
    val horarioFimEstimado: String
)

object EventoStorage {
    private const val PREFS_NAME = "motorista_calc_eventos"
    private const val CHAVE_LISTA = "lista_eventos"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun listarTodos(context: Context): List<Evento> {
        val json = prefs(context).getString(CHAVE_LISTA, null) ?: return emptyList()
        val array = try { JSONArray(json) } catch (e: Exception) { return emptyList() }
        val lista = mutableListOf<Evento>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            lista.add(
                Evento(
                    id = obj.getLong("id"),
                    nome = obj.getString("nome"),
                    endereco = obj.getString("endereco"),
                    categoria = obj.optString("categoria", "Outro"),
                    dataTexto = obj.optString("dataTexto", ""),
                    horarioInicio = obj.optString("horarioInicio", ""),
                    horarioFimEstimado = obj.optString("horarioFimEstimado", "")
                )
            )
        }
        return lista
    }

    private fun salvarTudo(context: Context, lista: List<Evento>) {
        val array = JSONArray()
        for (e in lista) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("nome", e.nome)
            obj.put("endereco", e.endereco)
            obj.put("categoria", e.categoria)
            obj.put("dataTexto", e.dataTexto)
            obj.put("horarioInicio", e.horarioInicio)
            obj.put("horarioFimEstimado", e.horarioFimEstimado)
            array.put(obj)
        }
        prefs(context).edit().putString(CHAVE_LISTA, array.toString()).apply()
    }

    fun adicionar(context: Context, nome: String, endereco: String, categoria: String, dataTexto: String, horarioInicio: String, horarioFim: String) {
        val lista = listarTodos(context).toMutableList()
        lista.add(0, Evento(System.currentTimeMillis(), nome, endereco, categoria, dataTexto, horarioInicio, horarioFim))
        salvarTudo(context, lista)
    }

    fun apagar(context: Context, id: Long) {
        salvarTudo(context, listarTodos(context).filter { it.id != id })
    }
}
