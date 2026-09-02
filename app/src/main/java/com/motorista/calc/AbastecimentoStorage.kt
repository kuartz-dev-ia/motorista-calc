package com.motorista.calc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Abastecimento(
    val id: Long,
    val dataHora: Long,
    val litros: Double,
    val valorTotal: Double,
    val kmAtual: Double
)

object AbastecimentoStorage {
    private const val PREFS_NAME = "motorista_calc_abastecimentos"
    private const val CHAVE_LISTA = "lista_abastecimentos"
    private const val MAX_REGISTROS = 200

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun listarTodos(context: Context): List<Abastecimento> {
        val json = prefs(context).getString(CHAVE_LISTA, null) ?: return emptyList()
        val array = try { JSONArray(json) } catch (e: Exception) { return emptyList() }
        val lista = mutableListOf<Abastecimento>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            lista.add(
                Abastecimento(
                    id = obj.getLong("id"),
                    dataHora = obj.getLong("dataHora"),
                    litros = obj.getDouble("litros"),
                    valorTotal = obj.getDouble("valorTotal"),
                    kmAtual = obj.getDouble("kmAtual")
                )
            )
        }
        return lista
    }

    private fun salvarTudo(context: Context, lista: List<Abastecimento>) {
        val array = JSONArray()
        for (a in lista) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("dataHora", a.dataHora)
            obj.put("litros", a.litros)
            obj.put("valorTotal", a.valorTotal)
            obj.put("kmAtual", a.kmAtual)
            array.put(obj)
        }
        prefs(context).edit().putString(CHAVE_LISTA, array.toString()).apply()
    }

    fun adicionar(context: Context, litros: Double, valorTotal: Double, kmAtual: Double) {
        val lista = listarTodos(context).toMutableList()
        lista.add(Abastecimento(System.currentTimeMillis(), System.currentTimeMillis(), litros, valorTotal, kmAtual))
        val limitada = if (lista.size > MAX_REGISTROS) lista.takeLast(MAX_REGISTROS) else lista
        salvarTudo(context, limitada)
    }

    fun apagar(context: Context, id: Long) {
        val lista = listarTodos(context).filter { it.id != id }
        salvarTudo(context, lista)
    }

    /** Calcula o consumo (km/l) entre cada abastecimento e o anterior, ordenado
     * por km do painel. Método "tanque cheio a tanque cheio": a distância
     * percorrida desde o abastecimento anterior dividida pelos litros postos agora. */
    fun calcularConsumos(lista: List<Abastecimento>): List<Pair<Abastecimento, Double?>> {
        val ordenada = lista.sortedBy { it.kmAtual }
        val resultado = mutableListOf<Pair<Abastecimento, Double?>>()
        for (i in ordenada.indices) {
            if (i == 0) {
                resultado.add(Pair(ordenada[i], null))
            } else {
                val distancia = ordenada[i].kmAtual - ordenada[i - 1].kmAtual
                val consumo = if (distancia > 0 && ordenada[i].litros > 0) distancia / ordenada[i].litros else null
                resultado.add(Pair(ordenada[i], consumo))
            }
        }
        return resultado.sortedByDescending { it.first.dataHora }
    }

    fun consumoMedio(lista: List<Abastecimento>): Double? {
        val consumos = calcularConsumos(lista).mapNotNull { it.second }
        return if (consumos.isNotEmpty()) consumos.average() else null
    }
}
