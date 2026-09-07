package com.motorista.calc

/**
 * Camada de análise da corrida.
 *
 * Responsabilidade:
 * - Centralizar o resultado do CalculationEngine.
 * - Calcular indicadores adicionais.
 * - Preparar uma resposta única para Overlay, Histórico e Dashboard.
 *
 * Esta classe NÃO substitui o CalculationEngine.
 * Ela funciona como uma camada superior sobre o motor existente.
 */
data class RideAnalysis(
    val ride: RideInfo,
    val result: RideResult,
    val plataforma: String,

    val distanciaTotalKm: Double,
    val tempoTotalMin: Int,

    val percentualLucro: Double?,
    val custoTotalEstimado: Double?,

    val decisao: DecisaoCorrida,
    val confiancaDados: Int
) {

    val valorTotal: Double?
        get() = ride.valorTotal

    val valorPorKm: Double?
        get() = result.valorPorKmCalculado

    val valorPorHora: Double?
        get() = result.valorPorHoraEfetivo

    val lucroLiquido: Double?
        get() = result.lucroLiquidoEstimado

    val motivo: String
        get() = result.motivo

    companion object {

        fun criar(
            ride: RideInfo,
            engine: CalculationEngine,
            plataforma: String = "Outro"
        ): RideAnalysis {

            val result = engine.calcular(ride)

            val distanciaTotalKm =
                (ride.distanciaPickupKm ?: 0.0) +
                (ride.distanciaCorridaKm ?: 0.0)

            val tempoTotalMin =
                ride.tempoEfetivoMin ?: 0

            val percentualLucro =
                if (
                    result.lucroLiquidoEstimado != null &&
                    ride.valorTotal != null &&
                    ride.valorTotal > 0
                ) {
                    (result.lucroLiquidoEstimado / ride.valorTotal) * 100.0
                } else {
                    null
                }

            val custoTotalEstimado =
                if (
                    ride.valorTotal != null &&
                    result.lucroLiquidoEstimado != null
                ) {
                    ride.valorTotal - result.lucroLiquidoEstimado
                } else {
                    null
                }

            val decisao =
                when {
                    !result.valeAPena -> DecisaoCorrida.RECUSAR
                    result.nivel == NivelCorrida.BOM -> DecisaoCorrida.ACEITAR
                    else -> DecisaoCorrida.AVALIAR
                }

            val confiancaDados = calcularConfianca(ride)

            return RideAnalysis(
                ride = ride,
                result = result,
                plataforma = plataforma,
                distanciaTotalKm = distanciaTotalKm,
                tempoTotalMin = tempoTotalMin,
                percentualLucro = percentualLucro,
                custoTotalEstimado = custoTotalEstimado,
                decisao = decisao,
                confiancaDados = confiancaDados
            )
        }

        private fun calcularConfianca(ride: RideInfo): Int {

            var pontos = 0

            if (ride.valorTotal != null) pontos += 30
            if (ride.distanciaCorridaKm != null) pontos += 20
            if (ride.tempoCorridaMin != null) pontos += 20
            if (ride.distanciaPickupKm != null) pontos += 10
            if (ride.tempoPickupMin != null) pontos += 10
            if (ride.avaliacaoPassageiro != null) pontos += 5
            if (ride.surgeMultiplicador != null) pontos += 5

            return pontos.coerceIn(0, 100)
        }
    }
}

enum class DecisaoCorrida {
    ACEITAR,
    AVALIAR,
    RECUSAR
}
