package com.motorista.calc

/**
 * Central de padrões (regex) usados para reconhecer informações relevantes
 * no texto que o AccessibilityService extrai da tela do app de corrida.
 */
object TriggerPatterns {

    val PALAVRAS_GATILHO = listOf(
        "aceitar",
        "selecionar",
        "recusar",
        "solicitações",
        "viagem longa",
        "exclusivo"
    )

    /**
     * Considera tela de corrida só quando: tem uma palavra-gatilho, tem um valor em R$,
     * E tem pelo menos um trecho no formato "X min (Y km)" — isso evita falsos positivos
     * em telas como "Radar de viagens" (bônus por região), que têm valores em R$ mas não
     * têm o formato de trecho/perna de uma corrida real.
     */
    fun pareceTelaDeCorrida(textoTela: String): Boolean {
        val textoLower = textoTela.lowercase()
        val temGatilho = PALAVRAS_GATILHO.any { textoLower.contains(it) }
        val temValor = VALOR_REGEX.containsMatchIn(textoTela)
        val temPerna = LEG_REGEX.containsMatchIn(textoTela)
        return temGatilho && temValor && temPerna
    }

    private val VALOR_REGEX = Regex("""R\$\s?([0-9]{1,4}(?:[.,][0-9]{2})?)""")
    private val VALOR_POR_KM_REGEX = Regex("""R\$\s?([0-9]+(?:[.,][0-9]{2})?)\s*/\s*km""", RegexOption.IGNORE_CASE)
    private val SURGE_REGEX = Regex("""([0-9][.,][0-9])\s*x\b""")

    private val LEG_REGEX = Regex(
        """(?:(\d+)\s*h\s*e\s*)?(\d+)\s*min(?:utos)?\s*\(([0-9]+(?:[.,][0-9]+)?)\s*(km|m)\)""",
        RegexOption.IGNORE_CASE
    )

    private val AVALIACAO_REGEX = Regex(
        """([0-9][.,][0-9]{1,2})\s*(?:\((\d+)\)|·\s*(\d+)\s*corridas)"""
    )

    private fun normalizarNumero(texto: String): Double? =
        texto.replace(",", ".").toDoubleOrNull()

    fun extrairValorTotal(texto: String): Double? {
        for (m in VALOR_REGEX.findAll(texto)) {
            val fimMatch = m.range.last + 1
            val janela = texto.substring(fimMatch, minOf(fimMatch + 6, texto.length))
            if (!janela.contains("/km", ignoreCase = true) && !janela.trimStart().startsWith("/")) {
                return normalizarNumero(m.groupValues[1])
            }
        }
        return null
    }

    fun extrairValorPorKmExibido(texto: String): Double? {
        val m = VALOR_POR_KM_REGEX.find(texto) ?: return null
        return normalizarNumero(m.groupValues[1])
    }

    fun extrairSurge(texto: String): Double? {
        val m = SURGE_REGEX.find(texto) ?: return null
        return normalizarNumero(m.groupValues[1])
    }

    fun extrairAvaliacao(texto: String): Double? {
        val m = AVALIACAO_REGEX.find(texto) ?: return null
        return normalizarNumero(m.groupValues[1])
    }

    data class Perna(val tempoMin: Int, val distanciaKm: Double)

    fun extrairPernas(texto: String): List<Perna> {
        return LEG_REGEX.findAll(texto).map { m ->
            val horas = m.groupValues[1].toIntOrNull() ?: 0
            val minutos = m.groupValues[2].toIntOrNull() ?: 0
            val distanciaBruta = normalizarNumero(m.groupValues[3]) ?: 0.0
            val unidade = m.groupValues[4].lowercase()
            val distanciaKm = if (unidade == "m") distanciaBruta / 1000.0 else distanciaBruta
            Perna(tempoMin = horas * 60 + minutos, distanciaKm = distanciaKm)
        }.toList()
    }

    fun ehViagemLonga(texto: String): Boolean = texto.contains("Viagem longa", ignoreCase = true)
    fun ehVerificado(texto: String): Boolean = texto.contains("Verificado", ignoreCase = true)
}
