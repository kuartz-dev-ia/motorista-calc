package com.motorista.calc

/** Detecta telas de "resumo de ganhos" do Uber/99 (ex: "R$ 152,10 hoje · 3
 * viagens concluídas") e extrai o valor total e a quantidade de corridas.
 * Fica separado do TriggerPatterns.kt (que cuida da tela de OFERTA de
 * corrida) pra não mexer nos regex já ajustados lá. */
object ResumoDetector {

    private val REGEX_VALOR = Regex("""R\$\s?(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2})""")
    private val REGEX_VIAGENS = Regex("""(\d+)\s*(viagens|corridas)""", RegexOption.IGNORE_CASE)
    private val PALAVRAS_RESUMO = listOf("hoje", "ganhos", "faturamento", "resumo", "total do dia")

    fun pareceTelaDeResumoGanhos(texto: String): Boolean {
        val textoLower = texto.lowercase()
        val temValor = REGEX_VALOR.containsMatchIn(texto)
        val temViagens = REGEX_VIAGENS.containsMatchIn(texto)
        val temPalavraResumo = PALAVRAS_RESUMO.any { textoLower.contains(it) }
        return temValor && temViagens && temPalavraResumo
    }

    fun extrairValorTotal(texto: String): Double? {
        val match = REGEX_VALOR.find(texto) ?: return null
        val bruto = match.groupValues[1].replace(".", "").replace(",", ".")
        return bruto.toDoubleOrNull()
    }

    fun extrairQtdViagens(texto: String): Int? {
        val match = REGEX_VIAGENS.find(texto) ?: return null
        return match.groupValues[1].toIntOrNull()
    }
}
