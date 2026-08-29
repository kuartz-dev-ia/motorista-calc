package com.motorista.calc

data class RideInfo(
    val valorTotal: Double?,
    val valorPorKmExibido: Double?,
    val surgeMultiplicador: Double?,
    val avaliacaoPassageiro: Double?,
    val viagemLonga: Boolean,
    val verificado: Boolean,
    val tempoPickupMin: Int?,
    val distanciaPickupKm: Double?,
    val tempoCorridaMin: Int?,
    val distanciaCorridaKm: Double?
) {
    val tempoEfetivoMin: Int?
        get() = if (tempoPickupMin != null || tempoCorridaMin != null)
            (tempoPickupMin ?: 0) + (tempoCorridaMin ?: 0)
        else null
}

enum class NivelCorrida { RUIM, MEDIO, BOM }

data class RideResult(
    val valorPorKmCalculado: Double?,
    val valorPorKmExibido: Double?,
    val valorPorHoraCorrida: Double?,
    val valorPorHoraEfetivo: Double?,
    val valorPorMinutoEfetivo: Double?,
    val custoCombustivelEstimado: Double?,
    val custoFixoEstimado: Double?,
    val lucroLiquidoEstimado: Double?,
    val valeAPena: Boolean,
    val nivel: NivelCorrida,
    val motivo: String
)

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

        val valorPorMinutoEfetivo = if (valor != null && tempoEfetivo != null && tempoEfetivo > 0) {
            valor / tempoEfetivo
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

        // Nível: RUIM se não vale a pena; entre BOM e MEDIO conforme a margem
        // acima dos mínimos configurados (quanto mais folga, mais "BOM").
        val nivel = if (!valeAPena) {
            NivelCorrida.RUIM
        } else {
            val margemKm = if (valorPorKmCalculado != null && minimoValorPorKm > 0) valorPorKmCalculado / minimoValorPorKm else null
            val margemHora = if (referenciaHora != null && minimoValorPorHora > 0) referenciaHora / minimoValorPorHora else null
            val margem = listOfNotNull(margemKm, margemHora).minOrNull() ?: 1.0
            if (margem >= 1.3) NivelCorrida.BOM else NivelCorrida.MEDIO
        }

        return RideResult(
            valorPorKmCalculado = valorPorKmCalculado,
            valorPorKmExibido = ride.valorPorKmExibido,
            valorPorHoraCorrida = valorPorHoraCorrida,
            valorPorHoraEfetivo = valorPorHoraEfetivo,
            valorPorMinutoEfetivo = valorPorMinutoEfetivo,
            custoCombustivelEstimado = custoCombustivel,
            custoFixoEstimado = custoFixoEstimado,
            lucroLiquidoEstimado = lucroLiquido,
            valeAPena = valeAPena,
            nivel = nivel,
            motivo = motivos.joinToString("; ")
        )
    }
}
