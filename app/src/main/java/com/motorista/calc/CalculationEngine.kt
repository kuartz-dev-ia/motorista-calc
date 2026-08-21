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
    val lucroLiquidoEstimado: Double?,
    val valeAPena: Boolean,
    val motivo: String
)

/**
 * Motor de cálculo. Os parâmetros vêm das preferências do usuário (SharedPreferences).
 *
 * Importante sobre R$/hora: usamos o tempo EFETIVO (deslocamento até o passageiro + corrida)
 * como métrica principal de decisão, porque é o tempo real que o motorista fica indisponível
 * para outras corridas — não só o tempo pago da corrida.
 */
class CalculationEngine(
    private val precoCombustivelPorLitro: Double = 6.10,
    private val consumoKmPorLitro: Double = 12.0,
    private val minimoValorPorKm: Double = 1.50,
    private val minimoValorPorHora: Double = 25.0
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

        // Custo de combustível considera a distância RODADA total (indo buscar + corrida),
        // já que o carro gasta combustível nos dois trechos.
        val custoCombustivel = if (distanciaTotalRodada > 0) {
            (distanciaTotalRodada / consumoKmPorLitro) * precoCombustivelPorLitro
        } else null

        val lucroLiquido = if (valor != null && custoCombustivel != null) {
            valor - custoCombustivel
        } else null

        val motivos = mutableListOf<String>()
        var valeAPena = true

        // A decisão usa o valor/hora EFETIVO (mais realista) como critério principal.
        val referenciaHora = valorPorHoraEfetivo ?: valorPorHoraCorrida
        if (referenciaHora != null && referenciaHora < minimoValorPorHora) {
            valeAPena = false
            motivos.add("R$/hora abaixo do mínimo (%.2f < %.2f)".format(referenciaHora, minimoValorPorHora))
        }
        if (valorPorKmCalculado != null && valorPorKmCalculado < minimoValorPorKm) {
            valeAPena = false
            motivos.add("R$/km abaixo do mínimo (%.2f < %.2f)".format(valorPorKmCalculado, minimoValorPorKm))
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
            lucroLiquidoEstimado = lucroLiquido,
            valeAPena = valeAPena,
            motivo = motivos.joinToString("; ")
        )
    }
}
