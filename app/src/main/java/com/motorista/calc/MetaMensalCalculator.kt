package com.motorista.calc

import android.content.Context

data class ResultadoMetaMensal(
    val custoMensalTotal: Double,
    val diasTrabalho: Int,
    val metaBrutaDiaria: Double
)

/** Soma TODOS os custos mensais já cadastrados no app (custos fixos de
 * Parâmetros + Minhas Contas + estimativa de combustível baseada no km médio
 * mensal) e divide pelos dias que a pessoa pretende trabalhar no mês,
 * mostrando quanto precisa faturar BRUTO por dia só pra cobrir as contas. */
object MetaMensalCalculator {

    fun calcular(context: Context): ResultadoMetaMensal {
        val prefs = context.getSharedPreferences(RideAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE)

        val financiamento = prefs.getFloat(RideAccessibilityService.PREF_FINANCIAMENTO, 0f).toDouble()
        val seguro = prefs.getFloat(RideAccessibilityService.PREF_SEGURO, 0f).toDouble()
        val ipvaAnual = prefs.getFloat(RideAccessibilityService.PREF_IPVA, 0f).toDouble()
        val licenciamentoAnual = prefs.getFloat(RideAccessibilityService.PREF_LICENCIAMENTO, 0f).toDouble()
        val manutencao = prefs.getFloat(RideAccessibilityService.PREF_MANUTENCAO, 0f).toDouble()
        val contasPessoaisParametros = prefs.getFloat(RideAccessibilityService.PREF_CONTAS_PESSOAIS, 0f).toDouble()
        val custosFixosParametros = financiamento + seguro + (ipvaAnual / 12.0) + (licenciamentoAnual / 12.0) + manutencao + contasPessoaisParametros

        val totalMinhasContas = ContaStorage.totalMensal(context)

        val kmMes = prefs.getFloat(RideAccessibilityService.PREF_KM_MES, 3000f).toDouble()
        val (preco, consumo) = when (prefs.getString(RideAccessibilityService.PREF_COMBUSTIVEL_ATIVO, "etanol")) {
            "gasolina" -> Pair(prefs.getFloat(RideAccessibilityService.PREF_PRECO_GASOLINA, 6.10f).toDouble(), prefs.getFloat(RideAccessibilityService.PREF_CONSUMO_GASOLINA, 10.0f).toDouble())
            "gnv" -> Pair(prefs.getFloat(RideAccessibilityService.PREF_PRECO_GNV, 4.50f).toDouble(), prefs.getFloat(RideAccessibilityService.PREF_CONSUMO_GNV, 12.0f).toDouble())
            else -> Pair(prefs.getFloat(RideAccessibilityService.PREF_PRECO_ETANOL, 4.20f).toDouble(), prefs.getFloat(RideAccessibilityService.PREF_CONSUMO_ETANOL, 7.0f).toDouble())
        }
        val custoPorKm = if (consumo > 0) preco / consumo else 0.0
        val custoCombustivelMensal = kmMes * custoPorKm

        val custoMensalTotal = custosFixosParametros + totalMinhasContas + custoCombustivelMensal

        val diasTrabalho = prefs.getInt(RideAccessibilityService.PREF_DIAS_TRABALHO_MES, 22).coerceAtLeast(1)
        val metaBrutaDiaria = custoMensalTotal / diasTrabalho

        return ResultadoMetaMensal(custoMensalTotal, diasTrabalho, metaBrutaDiaria)
    }
}
