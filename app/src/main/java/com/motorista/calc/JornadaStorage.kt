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
    var odometroFinal: Double?,
    var valorFinalUber: Double? = null,
    var valorFinal99: Double? = null,
    var kmFinalInformado: Double? = null
)

data class JornadaStats(
    val ganhoBruto: Double,
    val kmRodados: Double,
    val tempoTrabalhadoMin: Int,
    val valorPorHora: Double,
    val valorPorKm: Double,
    val percentualMeta: Double,
    val custoCombustivel: Double,
    val custoFixo: Double,
    val lucroLiquido: Double
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
                    odometroFinal = if (obj.has("odometroFinal") && !obj.isNull("odometroFinal")) obj.getDouble("odometroFinal") else null,
                    valorFinalUber = if (obj.has("valorFinalUber") && !obj.isNull("valorFinalUber")) obj.getDouble("valorFinalUber") else null,
                    valorFinal99 = if (obj.has("valorFinal99") && !obj.isNull("valorFinal99")) obj.getDouble("valorFinal99") else null,
                    kmFinalInformado = if (obj.has("kmFinalInformado") && !obj.isNull("kmFinalInformado")) obj.getDouble("kmFinalInformado") else null
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
            j.valorFinalUber?.let { obj.put("valorFinalUber", it) }
            j.valorFinal99?.let { obj.put("valorFinal99", it) }
            j.kmFinalInformado?.let { obj.put("kmFinalInformado", it) }
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

    /** Encerra a jornada com o resumo final informado manualmente pelo
     * motorista: quanto fez na Uber, quanto fez na 99, e o km total rodado.
     * Isso vira a fonte "oficial" de ganho/km daquela jornada — mais preciso
     * que a soma automática, já que cobre gorjetas, ajustes e trajetos sem
     * corrida (ex: voltando pra casa vazio). */
    fun encerrarComResumo(context: Context, id: Long, valorUber: Double, valor99: Double, kmTotal: Double) {
        val lista = listarTodas(context).toMutableList()
        val idx = lista.indexOfFirst { it.id == id }
        if (idx >= 0) {
            lista[idx] = lista[idx].copy(
                dataFimMillis = System.currentTimeMillis(),
                valorFinalUber = valorUber,
                valorFinal99 = valor99,
                kmFinalInformado = kmTotal
            )
            salvarTudo(context, lista)
        }
    }

    fun apagar(context: Context, id: Long) {
        salvarTudo(context, listarTodas(context).filter { it.id != id })
    }

    private fun obterPrecoEConsumoAtivos(prefs: android.content.SharedPreferences): Pair<Double, Double> {
        return when (prefs.getString(RideAccessibilityService.PREF_COMBUSTIVEL_ATIVO, "etanol")) {
            "gasolina" -> Pair(
                prefs.getFloat(RideAccessibilityService.PREF_PRECO_GASOLINA, 6.10f).toDouble(),
                prefs.getFloat(RideAccessibilityService.PREF_CONSUMO_GASOLINA, 10.0f).toDouble()
            )
            "gnv" -> Pair(
                prefs.getFloat(RideAccessibilityService.PREF_PRECO_GNV, 4.50f).toDouble(),
                prefs.getFloat(RideAccessibilityService.PREF_CONSUMO_GNV, 12.0f).toDouble()
            )
            else -> Pair(
                prefs.getFloat(RideAccessibilityService.PREF_PRECO_ETANOL, 4.20f).toDouble(),
                prefs.getFloat(RideAccessibilityService.PREF_CONSUMO_ETANOL, 7.0f).toDouble()
            )
        }
    }

    fun calcularStats(context: Context, jornada: Jornada): JornadaStats {
        val fim = jornada.dataFimMillis ?: System.currentTimeMillis()
        val temResumoManual = jornada.dataFimMillis != null &&
            jornada.valorFinalUber != null && jornada.valorFinal99 != null && jornada.kmFinalInformado != null

        val ganhoBruto: Double
        val kmRodados: Double

        if (temResumoManual) {
            ganhoBruto = (jornada.valorFinalUber ?: 0.0) + (jornada.valorFinal99 ?: 0.0)
            kmRodados = jornada.kmFinalInformado ?: 0.0
        } else {
            val registros = HistoricoStorage.listarEntre(context, jornada.dataInicioMillis, fim).filter { it.aceita && !it.cancelada }
            ganhoBruto = registros.sumOf { it.valorTotal }
            kmRodados = if (jornada.dataFimMillis != null && jornada.odometroFinal != null) {
                (jornada.odometroFinal!! - jornada.odometroInicial).coerceAtLeast(0.0)
            } else {
                registros.sumOf { it.distanciaTotalKm }
            }
        }

        val tempoTrabalhadoMin = ((fim - jornada.dataInicioMillis) / 60000).toInt()
        val horas = tempoTrabalhadoMin / 60.0
        val valorPorHora = if (horas > 0) ganhoBruto / horas else 0.0
        val valorPorKm = if (kmRodados > 0) ganhoBruto / kmRodados else 0.0
        val percentualMeta = if (jornada.metaDiaria > 0) (ganhoBruto / jornada.metaDiaria) * 100 else 0.0

        val prefs = context.getSharedPreferences(RideAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE)
        val (precoAtivo, consumoAtivo) = obterPrecoEConsumoAtivos(prefs)
        val custoCombustivel = if (consumoAtivo > 0) (kmRodados / consumoAtivo) * precoAtivo else 0.0

        val financiamento = prefs.getFloat(RideAccessibilityService.PREF_FINANCIAMENTO, 0f).toDouble()
        val seguro = prefs.getFloat(RideAccessibilityService.PREF_SEGURO, 0f).toDouble()
        val ipvaAnual = prefs.getFloat(RideAccessibilityService.PREF_IPVA, 0f).toDouble()
        val licenciamentoAnual = prefs.getFloat(RideAccessibilityService.PREF_LICENCIAMENTO, 0f).toDouble()
        val manutencao = prefs.getFloat(RideAccessibilityService.PREF_MANUTENCAO, 0f).toDouble()
        val contasPessoais = prefs.getFloat(RideAccessibilityService.PREF_CONTAS_PESSOAIS, 0f).toDouble()
        val kmMes = prefs.getFloat(RideAccessibilityService.PREF_KM_MES, 3000f).toDouble()
        val custoFixoMensal = financiamento + seguro + (ipvaAnual / 12.0) + (licenciamentoAnual / 12.0) + manutencao + contasPessoais
        val custoFixoPorKm = if (kmMes > 0) custoFixoMensal / kmMes else 0.0
        val custoFixo = custoFixoPorKm * kmRodados

        val lucroLiquido = ganhoBruto - custoCombustivel - custoFixo

        return JornadaStats(ganhoBruto, kmRodados, tempoTrabalhadoMin, valorPorHora, valorPorKm, percentualMeta, custoCombustivel, custoFixo, lucroLiquido)
    }
}
