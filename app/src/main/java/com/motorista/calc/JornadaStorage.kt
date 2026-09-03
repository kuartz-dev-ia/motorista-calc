package com.motorista.calc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Jornada(
    val id: Long,
    val dataInicioMillis: Long,
    var dataFimMillis: Long?,
    val metaDiaria: Double,
    val cargaHorariaHoras: Double,
    val odometroInicial: Double,
    var odometroFinal: Double?
)

data class JornadaStats(
    val ganhoBruto: Double,
    val kmRodados: Double,
    val tempoTrabalhadoMin: Int,
    val valorPorHora: Double,
    val valorPorKm: Double,
    val percentualMeta: Double
)

object JornadaStorage {
    private const val PREFS_NAME = "motorista_calc_jornadas"
    private const val CHAVE_LISTA = "lista_jornadas"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun listarTodas(context: Context): List<Jornada> {
        val json = prefs(context).getString(CHAVE_LISTA, null) ?: return emptyList()
        val array = try { JSONArray(json) } catch (e: Exception) { return emptyList() }
        val lista = mutableListOf<Jornada>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            lista.add(
                Jornada(
                    id = obj.getLong("id"),
                    dataInicioMillis = obj.getLong("dataInicioMillis"),
                    dataFimMillis = if (obj.has("dataFimMillis") && !obj.isNull("dataFimMillis")) obj.getLong("dataFimMillis") else null,
                    metaDiaria = obj.getDouble("metaDiaria"),
                    cargaHorariaHoras = obj.getDouble("cargaHorariaHoras"),
                    odometroInicial = obj.getDouble("odometroInicial"),
                    odometroFinal = if (obj.has("odometroFinal") && !obj.isNull("odometroFinal")) obj.getDouble("odometroFinal") else null
                )
            )
        }
        return lista
    }

    private fun salvarTudo(context: Context, lista: List<Jornada>) {
        val array = JSONArray()
        for (j in lista) {
            val obj = JSONObject()
            obj.put("id", j.id)
            obj.put("dataInicioMillis", j.dataInicioMillis)
            j.dataFimMillis?.let { obj.put("dataFimMillis", it) }
            obj.put("metaDiaria", j.metaDiaria)
            obj.put("cargaHorariaHoras", j.cargaHorariaHoras)
            obj.put("odometroInicial", j.odometroInicial)
            j.odometroFinal?.let { obj.put("odometroFinal", it) }
            array.put(obj)
        }
        prefs(context).edit().putString(CHAVE_LISTA, array.toString()).apply()
    }

    fun jornadaAtiva(context: Context): Jornada? = listarTodas(context).firstOrNull { it.dataFimMillis == null }

    fun iniciar(context: Context, metaDiaria: Double, cargaHorariaHoras: Double, odometroInicial: Double): Jornada {
        val lista = listarTodas(context).toMutableList()
        val nova = Jornada(System.currentTimeMillis(), System.currentTimeMillis(), null, metaDiaria, cargaHorariaHoras, odometroInicial, null)
        lista.add(0, nova)
        salvarTudo(context, lista)
        return nova
    }

    fun encerrar(context: Context, id: Long, odometroFinal: Double) {
        val lista = listarTodas(context).toMutableList()
        val idx = lista.indexOfFirst { it.id == id }
        if (idx >= 0) {
            lista[idx] = lista[idx].copy(dataFimMillis = System.currentTimeMillis(), odometroFinal = odometroFinal)
            salvarTudo(context, lista)
        }
    }

    fun calcularStats(context: Context, jornada: Jornada): JornadaStats {
        val fim = jornada.dataFimMillis ?: System.currentTimeMillis()
        val registros = HistoricoStorage.listarEntre(context, jornada.dataInicioMillis, fim).filter { it.aceita && !it.cancelada }

        val ganhoBruto = registros.sumOf { it.valorTotal }
        val kmRodados = registros.sumOf { it.distanciaTotalKm }
        val tempoTrabalhadoMin = ((fim - jornada.dataInicioMillis) / 60000).toInt()
        val horas = tempoTrabalhadoMin / 60.0
        val valorPorHora = if (horas > 0) ganhoBruto / horas else 0.0
        val valorPorKm = if (kmRodados > 0) ganhoBruto / kmRodados else 0.0
        val percentualMeta = if (jornada.metaDiaria > 0) (ganhoBruto / jornada.metaDiaria) * 100 else 0.0

        return JornadaStats(ganhoBruto, kmRodados, tempoTrabalhadoMin, valorPorHora, valorPorKm, percentualMeta)
    }
}
