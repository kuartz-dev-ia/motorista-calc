package com.motorista.calc

/** Detecta a tela que mostra o resultado de UMA corrida específica que acabou
 * de ser concluída — reconhece a frase da Uber ("última viagem") e as
 * variações da 99 ("valor da última viagem" / "valor da última corrida") —
 * e extrai o valor daquela viagem (que aparece acima da frase, no canto
 * esquerdo), além da categoria e horário, se existirem por perto. */
object ResumoDetector {

    private val REGEX_VALOR = Regex("""R\$\s?(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2})""")
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

    private fun normalizarLinha(linha: String) = linha.trim()

    fun pareceUltimaViagem(texto: String): Boolean {
        val textoLower = texto.lowercase()
        return FRASES_GATILHO.any { textoLower.contains(it) }
    }

    /** Procura o valor em R$ mais próximo ACIMA da linha com a frase-gatilho
     * (é onde ele normalmente aparece, tanto no Uber quanto na 99). A busca
     * não tem mais um limite curto de linhas — varre desde a frase até o
     * início do texto, pegando a ocorrência mais próxima. Se não achar acima,
     * tenta a mesma linha ou abaixo, como reforço. */
    fun extrairValorUltimaViagem(texto: String): Double? {
        val linhas = texto.lines().map { normalizarLinha(it) }
        val indiceFrase = linhas.indexOfFirst { linha ->
            val l = linha.lowercase()
            FRASES_GATILHO.any { l.contains(it) }
        }
        if (indiceFrase == -1) return null

        for (i in indiceFrase - 1 downTo 0) {
            val match = REGEX_VALOR.find(linhas[i])
            if (match != null) return converterValor(match.groupValues[1])
        }
        for (i in indiceFrase until linhas.size) {
            val match = REGEX_VALOR.find(linhas[i])
            if (match != null) return converterValor(match.groupValues[1])
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
