package com.motorista.calc

/**
 * Central de padrões (regex) usados para reconhecer informações relevantes
 * no texto que o AccessibilityService extrai da tela do app de corrida.
 *
 * Regex ajustados a partir de prints REAIS de Uber Driver e 99 Motorista
 * (telas "Dinheiro", "Exclusivo"/Comfort/Black, navegação e lista de solicitações).
 *
 * Formatos observados nos prints:
 *  - Valor total:        "R$99,40"  (sem espaço, cards "Dinheiro")
 *                         "R$ 103,55" (com espaço, cards "Exclusivo/Comfort/Black")
 *  - Valor por km:        "R$1,72/km"  ou  "R$ 2,10/km aprox."
 *  - Multiplicador:       "⚡2,1x" / "1,6x" / "1,8x" (tarifa dinâmica)
 *  - Trecho (perna):      "74min (57,6km)" | "51 minutos (48.8 km)" | "1 h e 45 min (39.3 km)" | "2min (93m)"
 *  - Avaliação:           "4,20 · 5 corridas" | "★ 4,96 (900)" | "4,88 (251)"
 *  - Tags:                "Viagem longa (mais de 45...)", "Verificado", "Perfil Essencial/Premium", "Exclusivo"
 */
object TriggerPatterns {

    // --- Gatilho: a tela atual é uma tela de corrida? ---
    val PALAVRAS_GATILHO = listOf(
        "aceitar",
        "selecionar",
        "recusar",
        "solicitações",
        "viagem longa",
        "exclusivo"
    )

    fun pareceTelaDeCorrida(textoTela: String): Boolean {
        val textoLower = textoTela.lowercase()
        // precisa ter pelo menos uma palavra-gatilho E um valor em R$ na tela
        val temGatilho = PALAVRAS_GATILHO.any { textoLower.contains(it) }
        val temValor = VALOR_REGEX.containsMatchIn(textoTela)
        return temGatilho && temValor
    }

    // --- Valor em R$ (com ou sem espaço depois do "R$") ---
    private val VALOR_REGEX = Regex("""R\$\s?([0-9]{1,4}(?:[.,][0-9]{2})?)""")
    private val VALOR_POR_KM_REGEX = Regex("""R\$\s?([0-9]+(?:[.,][0-9]{2})?)\s*/\s*km""", RegexOption.IGNORE_CASE)

    // --- Multiplicador de tarifa dinâmica: "2,1x" "1,6x" ---
    private val SURGE_REGEX = Regex("""([0-9][.,][0-9])\s*x\b""")

    // --- Trecho: "(h e )? min (dist unidade)" cobre todos os formatos de duração vistos ---
    private val LEG_REGEX = Regex(
        """(?:(\d+)\s*h\s*e\s*)?(\d+)\s*min(?:utos)?\s*\(([0-9]+(?:[.,][0-9]+)?)\s*(km|m)\)""",
        RegexOption.IGNORE_CASE
    )

    // --- Avaliação: exige "(N)" ou "· N corridas" junto pra evitar falso positivo com outros números ---
    private val AVALIACAO_REGEX = Regex(
        """([0-9][.,][0-9]{1,2})\s*(?:\((\d+)\)|·\s*(\d+)\s*corridas)"""
    )

    private fun normalizarNumero(texto: String): Double? =
        texto.replace(",", ".").toDoubleOrNull()

    /** Valor total da corrida — primeiro "R$ X,XX" da tela que NÃO é seguido de "/km". */
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

    /** Valor por km já calculado e EXIBIDO pelo próprio app (Uber/99). Útil para conferência. */
    fun extrairValorPorKmExibido(texto: String): Double? {
        val m = VALOR_POR_KM_REGEX.find(texto) ?: return null
        return normalizarNumero(m.groupValues[1])
    }

    fun extrairSurge(texto: String): Double? {
        val m = SURG
