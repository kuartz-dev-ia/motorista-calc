package com.motorista.calc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Manutencao(
    val id: Long,
    val tipo: String,
    val dataHora: Long,
    val kmRegistrado: Double,
    val custo: Double,
    val proximaKm: Double?,
    val proximaDataMillis: Long?
)

object ManutencaoStorage {
    private const val PREFS_NAME = "motorista_calc_manutencoes"
    private const val CHAVE_LISTA = "lista_manutencoes"
    private const val MAX_REGISTROS = 200

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun listarTodos(context: Context): List<Manutencao> {
        val json = prefs(context).getString(CHAVE_LISTA, null) ?: return emptyList()
        val array = try { JSONArray(json) } catch (e: Exception) { return emptyList() }
        val lista = mutableListOf<Manutencao>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            lista.add(
                Manutencao(
                    id = obj.getLong("id"),
                    tipo = obj.getString("tipo"),
                    dataHora = obj.getLong("dataHora"),
                    kmRegistrado = obj.getDouble("kmRegistrado"),
                    custo = obj.getDouble("custo"),
                    proximaKm = if (obj.has("proximaKm") && !obj.isNull("proximaKm")) obj.getDouble("proximaKm") else null,
                    proximaDataMillis = if (obj.has("proximaDataMillis") && !obj.isNull("proximaDataMillis")) obj.getLong("proximaDataMillis") else null
                )
            )
        }
        return lista
    }

    private fun salvarTudo(context: Context, lista: List<Manutencao>) {
        val array = JSONArray()
        for (m in lista) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("tipo", m.tipo)
            obj.put("dataHora", m.dataHora)
            obj.put("kmRegistrado", m.kmRegistrado)
            obj.put("custo", m.custo)
            m.proximaKm?.let { obj.put("proximaKm", it) }
            m.proximaDataMillis?.let { obj.put("proximaDataMillis", it) }
            array.put(obj)
        }
        prefs(context).edit().putString(CHAVE_LISTA, array.toString()).apply()
    }

    fun adicionar(
        context: Context,
        tipo: String,
        kmRegistrado: Double,
        custo: Double,
        intervaloKm: Double?,
        intervaloDias: Int?
    ) {
        val lista = listarTodos(context).toMutableList()
        val proximaKm = intervaloKm?.let { kmRegistrado + it }
        val proximaData = intervaloDias?.let { System.currentTimeMillis() + it.toLong() * 24 * 60 * 60 * 1000 }

        lista.add(0, Manutencao(System.currentTimeMillis(), tipo, System.currentTimeMillis(), kmRegistrado, custo, proximaKm, proximaData))
        val limitada = if (lista.size > MAX_REGISTROS) lista.take(MAX_REGISTROS) else lista
        salvarTudo(context, limitada)
    }

    fun apagar(context: Context, id: Long) {
        salvarTudo(context, listarTodos(context).filter { it.id != id })
    }

    /** Km mais recente conhecida do carro, baseada no último abastecimento registrado. */
    fun kmAtualEstimada(context: Context): Double? {
        return AbastecimentoStorage.listarTodos(context).maxByOrNull { it.dataHora }?.kmAtual
    }

    /** Pra cada tipo de manutenção, pega o registro mais recente e verifica se
     * já passou do km ou da data prevista pra próxima. */
    fun pendencias(context: Context): List<Manutencao> {
        val lista = listarTodos(context)
        val maisRecentePorTipo = lista.groupBy { it.tipo }.mapValues { it.value.maxByOrNull { m -> it.value.indexOf(m) }!! }
        val kmAtual = kmAtualEstimada(context)
        val agora = System.currentTimeMillis()

        return lista.groupBy { it.tipo }
            .mapNotNull { (_, registros) -> registros.maxByOrNull { it.dataHora } }
            .filter { registro ->
                val venceuPorKm = registro.proximaKm != null && kmAtual != null && kmAtual >= registro.proximaKm
                val venceuPorData = registro.proximaDataMillis != null && agora >= registro.proximaDataMillis
                venceuPorKm || venceuPorData
            }
    }
}
