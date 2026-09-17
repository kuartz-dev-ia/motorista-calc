package com.motorista.calc

/** Detecta a tela que mostra o resultado de UMA corrida específica que acabou
 * de ser concluída (identificada pela frase "última viagem" na tela), e
 * extrai o valor daquela viagem (que aparece logo acima da frase), além da
 * categoria (Comfort, Black, etc.) e o horário, se existirem por perto.
 * Isso soma corrida por corrida no histórico, em vez de pegar o total
 * acumulado do dia (que ficaria duplicado a cada nova viagem). */
object ResumoDetector {

    private val REGEX_VALOR = Regex("""R\$\s?(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2})""")
    private val REGEX_HORARIO = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""")
    private val CATEGORIAS = listOf(
        "uberx", "uber x", "comfort", "black", "99pop", "pop", "moto", "motoTáxi",
        "bag", "van", "exclusivo", "vip", "flash", "top", "grand", "select", "green"
    )

    private fun normalizarLinha(linha: String) = linha.trim()

    fun pareceUltimaViagem(texto: String): Boolean {
        val textoLower = texto.lowercase()
        return textoLower.contains("última viagem") || textoLower.contains("ultima viagem")
    }

    /** Procura o valor em R$ mais próximo ACIMA da linha "última viagem"
     * (que é onde ele aparece, segundo o padrão descrito). Se não achar acima,
     * tenta a mesma linha ou logo abaixo, como reforço. */
    fun extrairValorUltimaViagem(texto: String): Double? {
        val linhas = texto.lines().map { normalizarLinha(it) }
        val indiceFrase = linhas.indexOfFirst {
            val l = it.lowercase()
            l.contains("última viagem") || l.contains("ultima viagem")
        }
        if (indiceFrase == -1) return null

        // Procura pra cima primeiro (até 3 linhas acima).
        for (i in indiceFrase - 1 downTo maxOf(0, indiceFrase - 3)) {
            val match = REGEX_VALOR.find(linhas[i])
            if (match != null) return converterValor(match.groupValues[1])
        }
        // Reforço: mesma linha ou até 2 linhas abaixo.
        for (i in indiceFrase..minOf(linhas.size - 1, indiceFrase + 2)) {
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
