package com.motorista.calc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
    val aceita: Boolean,
    val cancelada: Boolean
)

/** Guarda o histórico de corridas detectadas (boas e não boas) e quais delas o
 * motorista marcou como aceitas ou canceladas, usando SharedPreferences + JSON. */
object HistoricoStorage {
    private const val PREFS_NAME = "motorista_calc_historico"
    private const val CHAVE_REGISTROS = "registros"
    private const val MAX_REGISTROS = 500

    // Se a MESMA oferta (valor + distância parecidos) for detectada de novo dentro
    // desse tempo, é considerada a mesma corrida (ainda na tela, não uma nova) —
    // evita duplicar no histórico enquanto o motorista não decide.
    private const val JANELA_DEDUP_MS = 45_000L

    private val formatoDia = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun diaChaveDeHoje(): String = formatoDia.format(Date())

    fun adicionarRegistro(
        context: Context,
        valorTotal: Double,
        distanciaTotalKm: Double,
        tempoTotalMin: Int,
        valorPorKm: Double?,
        valorPorHora: Double?,
        lucroLiquido: Double?,
        valeAPena: Boolean
    ): Long {
        val agora = System.currentTimeMillis()
        val lista = lerTodos(context).toMutableList()

        val existente = lista.lastOrNull { r ->
            (agora - r.dataHora) < JANELA_DEDUP_MS &&
                abs(r.valorTotal - valorTotal) < 0.05 &&
                abs(r.distanciaTotalKm - distanciaTotalKm) < 0.15
        }
        if (existente != null) return existente.id

        val id = agora
        val registro = RegistroCorrida(
            id = id,
            dataHora = id,
            diaChave = diaChaveDeHoje(),
            valorTotal = valorTotal,
            distanciaTotalKm = distanciaTotalKm,
            tempoTotalMin = tempoTotalMin,
            valorPorKm = valorPorKm,
            valorPorHora = valorPorHora,
            lucroLiquido = lucroLiquido,
            valeAPena = valeAPena,
            aceita = false,
            cancelada = false
        )
        lista.add(registro)
        val podado = if (lista.size > MAX_REGISTROS) lista.takeLast(MAX_REGISTROS) else lista
        salvarTodos(context, podado)
        return id
    }

    fun marcarAceita(context: Context, id: Long) {
        val lista = lerTodos(context).toMutableList()
        val idx = lista.indexOfFirst { it.id == id }
        if (idx >= 0) {
            lista[idx] = lista[idx].copy(aceita = true)
            salvarTodos(context, lista)
        }
    }

    fun marcarCancelada(context: Context, id: Long) {
        val lista = lerTodos(context).toMutableList()
        val idx = lista.indexOfFirst { it.id == id }
        if (idx >= 0) {
            lista[idx] = lista[idx].copy(cancelada = true)
            salvarTodos(context, lista)
        }
    }

    fun listarDoDia(context: Context): List<RegistroCorrida> {
        val hoje = diaChaveDeHoje()
        return lerTodos(context).filter { it.diaChave == hoje }
    }

    private fun lerTodos(context: Context): List<RegistroCorrida> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(CHAVE_REGISTROS, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                RegistroCorrida(
                    id = obj.optLong("id"),
                    dataHora = obj.optLong("dataHora"),
                    diaChave = obj.optString("diaChave"),
                    valorTotal = obj.optDouble("valorTotal"),
                    distanciaTotalKm = obj.optDouble("distanciaTotalKm"),
                    tempoTotalMin = obj.optInt("tempoTotalMin"),
                    valorPorKm = if (obj.has("valorPorKm") && !obj.isNull("valorPorKm")) obj.optDouble("valorPorKm") else null,
                    valorPorHora = if (obj.has("valorPorHora") && !obj.isNull("valorPorHora")) obj.optDouble("valorPorHora") else null,
                    lucroLiquido = if (obj.has("lucroLiquido") && !obj.isNull("lucroLiquido")) obj.optDouble("lucroLiquido") else null,
                    valeAPena = obj.optBoolean("valeAPena"),
                    aceita = obj.optBoolean("aceita"),
                    cancelada = obj.optBoolean("cancelada")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun salvarTodos(context: Context, lista: List<RegistroCorrida>) {
        val array = JSONArray()
        lista.forEach { r ->
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
            obj.put("aceita", r.aceita)
            obj.put("cancelada", r.cancelada)
            array.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(CHAVE_REGISTROS, array.toString()).apply()
    }
}
