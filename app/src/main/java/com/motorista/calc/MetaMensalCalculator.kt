package com.motorista.calc

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ResultadoMetaMensal(
    val custoMensalTotal: Double,
    val diasTrabalho: Int,
    val metaBrutaDiaria: Double,
    val metaBrutaDiariaBase: Double,
    val ganhoAcumuladoMes: Double,
    val diasRestantes: Int
)

/** Soma os custos mensais que NÃO estão duplicados em outro lugar do app:
 * "Minhas Contas" + combustível estimado (baseado no km médio mensal). Os
 * "Custos fixos mensais" de Parâmetros (financiamento, seguro, IPVA,
 * licenciamento, manutenção, contas pessoais) ficam de fora dessa soma de
 * propósito — eles continuam valendo só pro cálculo de lucro líquido do card
 * de cada corrida (CalculationEngine), evitando contar o mesmo custo duas
 * vezes. Cabe ao usuário decidir onde cadastra cada conta.
 *
 * Além disso, a meta diária se reajusta sozinha dentro do mês corrente: se
 * você ganhou mais que a meta num dia, a meta dos próximos dias diminui; se
 * ganhou menos, aumenta — sempre dividindo o que falta pra bater a meta do
 * mês pelos dias de trabalho que ainda restam. O mês reinicia sozinho no
 * dia 1. */
object MetaMensalCalculator {

    private val formatoDia = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun calcular(context: Context): ResultadoMetaMensal {
        val prefs = context.getSharedPreferences(RideAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE)

        val totalMinhasContas = ContaStorage.totalMensal(context)

        val kmMes = prefs.getFloat(RideAccessibilityService.PREF_KM_MES, 3000f).toDouble()
        val (preco, consumo) = when (prefs.getString(RideAccessibilityService.PREF_COMBUSTIVEL_ATIVO, "etanol")) {
            "gasolina" -> Pair(prefs.getFloat(RideAccessibilityService.PREF_PRECO_GASOLINA, 6.10f).toDouble(), prefs.getFloat(RideAccessibilityService.PREF_CONSUMO_GASOLINA, 10.0f).toDouble())
            "gnv" -> Pair(prefs.getFloat(RideAccessibilityService.PREF_PRECO_GNV, 4.50f).toDouble(), prefs.getFloat(RideAccessibilityService.PREF_CONSUMO_GNV, 12.0f).toDouble())
            else -> Pair(prefs.getFloat(RideAccessibilityService.PREF_PRECO_ETANOL, 4.20f).toDouble(), prefs.getFloat(RideAccessibilityService.PREF_CONSUMO_ETANOL, 7.0f).toDouble())
        }
        val custoPorKm = if (consumo > 0) preco / consumo else 0.0
        val custoCombustivelMensal = kmMes * custoPorKm

        val custoMensalTotal = totalMinhasContas + custoCombustivelMensal

        val diasTrabalho = prefs.getInt(RideAccessibilityService.PREF_DIAS_TRABALHO_MES, 22).coerceAtLeast(1)
        val metaBrutaDiariaBase = custoMensalTotal / diasTrabalho

        val inicioMes = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val inicioHoje = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Só jornadas de dias já FECHADOS (antes de hoje) dentro do mês
        // corrente contam pro ganho acumulado — o dia de hoje ainda está em
        // andamento, então a meta de hoje considera o que já passou até
        // ontem.
        val jornadasAnteriores = JornadaStorage.listarTodas(context).filter {
            it.dataInicioMillis in inicioMes until inicioHoje
        }

        val ganhoAcumuladoMes = jornadasAnteriores.sumOf { JornadaStorage.calcularStats(context, it).ganhoBruto }
        val diasTrabalhadosAteOntem = jornadasAnteriores.map { formatoDia.format(Date(it.dataInicioMillis)) }.distinct().size

        val diasRestantes = (diasTrabalho - diasTrabalhadosAteOntem).coerceAtLeast(1)
        val valorMensalRestante = (custoMensalTotal - ganhoAcumuladoMes).coerceAtLeast(0.0)
        val metaBrutaDiariaAjustada = valorMensalRestante / diasRestantes

        return ResultadoMetaMensal(
            custoMensalTotal = custoMensalTotal,
            diasTrabalho = diasTrabalho,
            metaBrutaDiaria = metaBrutaDiariaAjustada,
            metaBrutaDiariaBase = metaBrutaDiariaBase,
            ganhoAcumuladoMes = ganhoAcumuladoMes,
            diasRestantes = diasRestantes
        )
    }
}
