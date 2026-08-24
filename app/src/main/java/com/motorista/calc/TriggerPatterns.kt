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

    // Nomes de categoria de corrida — mais um sinal de que é uma tela de solicitação real.
    val CATEGORIAS = listOf(
        "uberx", "uber x", "black", "comfort", "confort", "conforto",
        "pop", "business", "99pop", "99top", "99comfort", "99moto"
    )

    /**
     * Considera tela de corrida real quando:
     * - tem um valor em R$
     * - tem PELO MENOS DUAS "pernas" (formato "X min (Y km)") — uma pra embarque,
     *   outra pra desembarque. Telas de bônus/radar não têm duas pernas assim.
     * - E tem uma palavra-gatilho OU um nome de categoria (Uber X, Black, Comfort...)
     */
    fun pareceTelaDeCorrida(textoTela: String): Boolean {
        val textoLower = textoTela.lowercase()
        val temValor = VALOR_REGEX.containsMatchIn(textoTela)
        val quantidadePernas = LEG_REGEX.findAll(textoTela).count()
        val temDuasPernas = quantidadePernas >= 2
        val temGatilho = PALAVRAS_GATILHO.any { textoLower.contains(it) }
        val temCategoria = CATEGORIAS.any { textoLower.contains(it) }
        return temValor && temDuasPernas && (temGatilho || temCategoria)
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
