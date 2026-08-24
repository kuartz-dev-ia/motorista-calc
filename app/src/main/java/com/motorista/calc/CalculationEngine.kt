package com.motorista.calc

data class RideInfo(
    val valorTotal: Double?,
    val valorPorKmExibido: Double?,      // já vem calculado pelo app (Uber/99), usamos pra conferência
    val surgeMultiplicador: Double?,     // ex: 2,1x — tarifa dinâmica
    val avaliacaoPassageiro: Double?,
    val viagemLonga: Boolean,
    val verificado: Boolean,
    val tempoPickupMin: Int?,            // tempo até chegar no passageiro (não pago)
    val distanciaPickupKm: Double?,
    val tempoCorridaMin: Int?,           // tempo da corrida em si (embarque -> destino)
    val distanciaCorridaKm: Double?
) {
    /** Tempo total "no relógio" do motorista: deslocamento até o passageiro + corrida. */
    val tempoEfetivoMin: Int?
        get() = if (tempoPickupMin != null || tempoCorridaMin != null)
            (tempoPickupMin ?: 0) + (tempoCorridaMin ?: 0)
        else null
}

data class RideResult(
    val valorPorKmCalculado: Double?,
    val valorPorKmExibido: Double?,
    val valorPorHoraCorrida: Double?,     // considerando só o tempo da corrida
    val valorPorHoraEfetivo: Double?,     // considerando também o deslocamento até o passageiro
    val custoCombustivelEstimado: Double?,
    val custoFixoEstimado: Double?,       // parcela de financiamento/seguro/IPVA/etc rateada nessa corrida
    val lucroLiquidoEstimado: Double?,    // valor da corrida - combustível - custos fixos rateados
    val valeAPena: Boolean,
    val motivo: String
)

/**
 * Motor de cálculo. Os parâmetros vêm das preferências do usuário (SharedPreferences).
 *
 * custoFixoPorKm: soma de todos os custos fixos mensais (financiamento, seguro, IPVA/12,
 * licenciamento/12, manutenção programada, contas pessoais) dividida pelos km rodados por
 * mês. Isso dá quanto de custo fixo "pesa" em cada km rodado, e é descontado do valor da
 * corrida junto com o combustível pra chegar no ganho líquido real.
 */
class CalculationEngine(
    private val precoCombustivelPorLitro: Double = 6.10,
    private val consumoKmPorLitro: Double = 12.0,
    private val minimoValorPorKm: Double = 1.50,
    private val minimoValorPorHora: Double = 25.0,
    private val custoFixoPorKm: Double = 0.0
) {

    fun calcular(ride: RideInfo): RideResult {
        val valor = ride.valorTotal
        val distanciaCorrida = ride.distanciaCorridaKm
        val distanciaTotalRodada = (ride.distanciaPickupKm ?: 0.0) + (distanciaCorrida ?: 0.0)

        val valorPorKmCalculado = if (valor != null && distanciaCorrida != null && distanciaCorrida > 0) {
            valor / distanciaCorrida
        } else null

        val valorPorHoraCorrida = if (valor != null && ride.tempoCorridaMin != null && ride.tempoCorridaMin > 0) {
            valor / (ride.tempoCorridaMin / 60.0)
        } else null

        val tempoEfetivo = ride.tempoEfetivoMin
        val valorPorHoraEfetivo = if (valor != null && tempoEfetivo != null && tempoEfetivo > 0) {
            valor / (tempoEfetivo / 60.0)
        } else null

        val custoCombustivel = if (distanciaTotalRodada > 0) {
            (distanciaTotalRodada / consumoKmPorLitro) * precoCombustivelPorLitro
        } else null

        val custoFixoEstimado = if (custoFixoPorKm > 0 && distanciaTotalRodada > 0) {
            custoFixoPorKm * distanciaTotalRodada
        } else null

        val lucroLiquido = if (valor != null && custoCombustivel != null) {
            valor - custoCombustivel - (custoFixoEstimado ?: 0.0)
        } else null

        val motivos = mutableListOf<String>()
        var valeAPena = true

        val referenciaHora = valorPorHoraEfetivo ?: valorPorHoraCorrida
        if (referenciaHora != null && referenciaHora < minimoValorPorHora) {
            valeAPena = false
            motivos.add("R$/hora abaixo do mínimo (%.2f < %.2f)".format(referenciaHora, minimoValorPorHora))
        }
        if (valorPorKmCalculado != null && valorPorKmCalculado < minimoValorPorKm) {
            valeAPena = false
            motivos.add("R$/km abaixo do mínimo (%.2f < %.2f)".format(valorPorKmCalculado, minimoValorPorKm))
        }
        if (lucroLiquido != null && lucroLiquido <= 0) {
            valeAPena = false
            motivos.add("Ganho líquido seria zero ou negativo após combustível e custos fixos (R$ %.2f)".format(lucroLiquido))
        }
        if (valorPorKmCalculado == null && referenciaHora == null) {
            motivos.add("Dados insuficientes para avaliar")
        }
        if (motivos.isEmpty()) {
            motivos.add("Dentro dos parâmetros configurados")
        }

        return RideResult(
            valorPorKmCalculado = valorPorKmCalculado,
            valorPorKmExibido = ride.valorPorKmExibido,
            valorPorHoraCorrida = valorPorHoraCorrida,
            valorPorHoraEfetivo = valorPorHoraEfetivo,
            custoCombustivelEstimado = custoCombustivel,
            custoFixoEstimado = custoFixoEstimado,
            lucroLiquidoEstimado = lucroLiquido,
            valeAPena = valeAPena,
            motivo = motivos.joinToString("; ")
        )
    }
}
