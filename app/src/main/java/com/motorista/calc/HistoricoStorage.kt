package com.motorista.calc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RegistroCorrida(
    val id: Long,
    val dataHora: Long,
    val diaChave: String,
    val valorTotal: Double,
    val distanciaTotalKm: Double,
    val tempoTotalMin: Int,
    val valorPorKm: Double?,
    val valorPorHora: Double?,
    val lucroLiquido: Double?,
    val valeAPena: Boolean,
    val plataforma: String,
    var aceita: Boolean = false,
    var cancelada: Boolean = false
)

object HistoricoStorage {
    private const val PREFS_NAME = "motorista_calc_historico"
    private const val CHAVE_LISTA = "lista_registros"
    private const val MAX_REGISTROS = 500
    private const val JANELA_DEDUP_MS = 45_000L

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun carregarTudo(context: Context): MutableList<RegistroCorrida> {
        val json = prefs(context).getString(CHAVE_LISTA, null) ?: return mutableListOf()
        val array = try { JSONArray(json) } catch (e: Exception) { return mutableListOf() }
        val lista = mutableListOf<RegistroCorrida>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            lista.add(
                RegistroCorrida(
                    id = obj.getLong("id"),
                    dataHora = obj.getLong("dataHora"),
                    diaChave = obj.getString("diaChave"),
                    valorTotal = obj.getDouble("valorTotal"),
                    distanciaTotalKm = obj.getDouble("distanciaTotalKm"),
                    tempoTotalMin = obj.getInt("tempoTotalMin"),
                    valorPorKm = if (obj.has("valorPorKm") && !obj.isNull("valorPorKm")) obj.getDouble("valorPorKm") else null,
                    valorPorHora = if (obj.has("valorPorHora") && !obj.isNull("valorPorHora")) obj.getDouble("valorPorHora") else null,
                    lucroLiquido = if (obj.has("lucroLiquido") && !obj.isNull("lucroLiquido")) obj.getDouble("lucroLiquido") else null,
                    valeAPena = obj.optBoolean("valeAPena", true),
                    plataforma = obj.optString("plataforma", "Outro"),
                    aceita = obj.optBoolean("aceita", false),
                    cancelada = obj.optBoolean("cancelada", false)
                )
            )
        }
        return lista
    }

    private fun salvarTudo(context: Context, lista: List<RegistroCorrida>) {
        val array = JSONArray()
        for (r in lista) {
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("dataHora", r.dataHora)
            obj.put("diaChave", r.diaChave)
            obj.put("valorTotal", r.valorTotal)
            obj.put("distanciaTotalKm", r.distanciaTotalKm)
            obj.put("tempoTotalMin", r.tempoTotalMin)
            r.valorPorKm?.let { obj.put("valorPorKm", it) }
            r.valorPorHora?.let { obj.put("valorPorHora", it) }
            r.lucroLiquido?.let { obj.put("lucroLiquido", it) }
            obj.put("valeAPena", r.valeAPena)
            obj.put("plataforma", r.plataforma)
            obj.put("aceita", r.aceita)
            obj.put("cancelada", r.cancelada)
            array.put(obj)
        }
        prefs(context).edit().putString(CHAVE_LISTA, array.toString()).apply()
    }

    fun adicionarRegistro(
        context: Context,
        valorTotal: Double,
        distanciaTotalKm: Double,
        tempoTotalMin: Int,
        valorPorKm: Double?,
        valorPorHora: Double?,
        lucroLiquido: Double?,
        valeAPena: Boolean,
        plataforma: String
    ): Pair<Long, Boolean> {
        val lista = carregarTudo(context)
        val agora = System.currentTimeMillis()

        val duplicado = lista.firstOrNull {
            (agora - it.dataHora) < JANELA_DEDUP_MS &&
                Math.abs(it.valorTotal - valorTotal) < 0.05 &&
                Math.abs(it.distanciaTotalKm - distanciaTotalKm) < 0.15
        }
        if (duplicado != null) {
            return Pair(duplicado.id, false)
        }

        val id = agora
        val diaChave = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(agora))

        val novo = RegistroCorrida(
            id = id,
            dataHora = agora,
            diaChave = diaChave,
            valorTotal = valorTotal,
            distanciaTotalKm = distanciaTotalKm,
            tempoTotalMin = tempoTotalMin,
            valorPorKm = valorPorKm,
            valorPorHora = valorPorHora,
            lucroLiquido = lucroLiquido,
            valeAPena = valeAPena,
            plataforma = plataforma
        )

        lista.add(0, novo)
        val limitada = if (lista.size > MAX_REGISTROS) lista.take(MAX_REGISTROS) else lista
        salvarTudo(context, limitada)

        return Pair(id, true)
    }

    fun marcarAceita(context: Context, id: Long) {
        val lista = carregarTudo(context)
        lista.find { it.id == id }?.aceita = true
        salvarTudo(context, lista)
    }

    fun marcarCancelada(context: Context, id: Long) {
        val lista = carregarTudo(context)
        lista.find { it.id == id }?.cancelada = true
        salvarTudo(context, lista)
    }

    fun listarDoDia(context: Context): List<RegistroCorrida> {
        val hoje = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return carregarTudo(context).filter { it.diaChave == hoje }
    }

    fun listarUltimosDias(context: Context, dias: Int): List<RegistroCorrida> {
        val limite = System.currentTimeMillis() - dias.toLong() * 24 * 60 * 60 * 1000
        return carregarTudo(context).filter { it.dataHora >= limite }
    }

    fun listarEntre(context: Context, inicioMillis: Long, fimMillis: Long): List<RegistroCorrida> {
        return carregarTudo(context).filter { it.dataHora in inicioMillis..fimMillis }
    }
}
