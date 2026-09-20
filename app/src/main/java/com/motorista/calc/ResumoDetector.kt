package com.motorista.calc

/** Detecta a tela que mostra o resultado de UMA corrida específica que acabou
 * de ser concluída — reconhece a frase da Uber ("última viagem") e as
 * variações da 99 ("valor da última viagem" / "valor da última corrida") —
 * e extrai o valor daquela viagem (que aparece acima da frase, no canto
 * esquerdo), além da categoria e horário, se existirem por perto. */
object ResumoDetector {

    private val REGEX_VALOR_COM_CIFRAO = Regex("""R\$\s?(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2})""")
    private val REGEX_VALOR_SEM_CIFRAO = Regex("""\b(\d{1,3}(?:\.\d{3})*,\d{2}|\d{1,3},\d{2})\b""")
    private val REGEX_HORARIO = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""")
    private val CATEGORIAS = listOf(
        "uberx", "uber x", "comfort", "black", "99pop", "pop", "moto", "motoTáxi",
        "bag", "van", "exclusivo", "vip", "flash", "top", "grand", "select", "green"
    )
    private val FRASES_GATILHO = listOf(
        "última viagem", "ultima viagem",
        "valor da última viagem", "valor da ultima viagem",
        "valor da última corrida", "valor da ultima corrida"
    )

    private const val VALOR_MINIMO_PLAUSIVEL = 0.5
    private const val VALOR_MAXIMO_PLAUSIVEL = 500.0

    private fun normalizarLinha(linha: String) = linha.trim()

    fun pareceUltimaViagem(texto: String): Boolean {
        val textoLower = texto.lowercase()
        return FRASES_GATILHO.any { textoLower.contains(it) }
    }

    /** Procura o valor mais próximo ACIMA da linha com a frase-gatilho (é onde
     * ele normalmente aparece, tanto no Uber quanto na 99). Primeiro tenta só
     * valores com "R$" na frente (mais confiável); se não achar nenhum em
     * toda a tela, tenta valores "soltos" (sem R$) na faixa plausível de uma
     * corrida — cobre o caso de painéis que mostram só o número. */
    fun extrairValorUltimaViagem(texto: String): Double? {
        val linhas = texto.lines().map { normalizarLinha(it) }
        val indiceFrase = linhas.indexOfFirst { linha ->
            val l = linha.lowercase()
            FRASES_GATILHO.any { l.contains(it) }
        }
        if (indiceFrase == -1) return null

        buscarComRegex(linhas, indiceFrase, REGEX_VALOR_COM_CIFRAO)?.let { return it }
        return buscarComRegex(linhas, indiceFrase, REGEX_VALOR_SEM_CIFRAO, validarFaixa = true)
    }

    private fun buscarComRegex(linhas: List<String>, indiceFrase: Int, regex: Regex, validarFaixa: Boolean = false): Double? {
        for (i in indiceFrase - 1 downTo 0) {
            val match = regex.find(linhas[i]) ?: continue
            val valor = converterValor(match.groupValues[1]) ?: continue
            if (!validarFaixa || valor in VALOR_MINIMO_PLAUSIVEL..VALOR_MAXIMO_PLAUSIVEL) return valor
        }
        for (i in indiceFrase until linhas.size) {
            val match = regex.find(linhas[i]) ?: continue
            val valor = converterValor(match.groupValues[1]) ?: continue
            if (!validarFaixa || valor in VALOR_MINIMO_PLAUSIVEL..VALOR_MAXIMO_PLAUSIVEL) return valor
        }
        return null
    }

    private fun converterValor(bruto: String): Double? {
        return bruto.replace(".", "").replace(",", ".").toDoubleOrNull()
    }

    fun extrairCategoria(texto: String): String? {
        val textoLower = texto.lowercase()
        val encontrada = CATEGORIAS.firstOrNull { textoLower.contains(it) } ?: return null
        return encontrada.replaceFirstChar { it.uppercase() }
    }

    fun extrairHorario(texto: String): String? {
        return REGEX_HORARIO.find(texto)?.value
    }
}
