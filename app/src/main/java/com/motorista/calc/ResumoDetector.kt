package com.motorista.calc

/** Detecta a tela que mostra o resultado de UMA corrida específica que acabou
 * de ser concluída — reconhece a frase da Uber ("última viagem") e a da 99
 * ("valor da última corrida"/"valor da última viagem"), inclusive quando:
 * - o OCR lê "última" errado, como "útima" (falta o L)
 * - a frase vem quebrada em duas linhas (comum na 99: "Valor da útima" numa
 *   linha e "Corrida >" na linha seguinte)
 * Também bloqueia um alarme falso identificado: a tela inicial do Uber tem um
 * atalho de navegação chamado "Última viagem" (mostra o último destino), que
 * usa as mesmas palavras mas não é uma corrida concluída — sempre aparece
 * junto com "Você está offline", então isso é usado como sinal de exclusão. */
object ResumoDetector {

    private val REGEX_VALOR_COM_CIFRAO = Regex("""R\$\s?(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2})""")
    private val REGEX_VALOR_SEM_CIFRAO = Regex("""\b(\d{1,3}(?:\.\d{3})*,\d{2}|\d{1,3},\d{2})\b""")
    private val REGEX_HORARIO = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""")
    private val CATEGORIAS = listOf(
        "uberx", "uber x", "comfort", "black", "99pop", "pop", "moto", "motoTáxi",
        "bag", "van", "exclusivo", "vip", "flash", "top", "grand", "select", "green"
    )

    // "(?:u|ú)[l]?tima" casa com "última", "ultima" E "útima" (o "[l]?"
    // torna o L opcional, cobrindo o erro comum do OCR de "comer" essa letra).
    private val REGEX_FRASE = Regex(
        """(?:valor\s+da\s+)?(?:u|ú)[l]?tima\s+(?:corrida|viagem)""",
        RegexOption.IGNORE_CASE
    )

    private const val VALOR_MINIMO_PLAUSIVEL = 0.5
    private const val VALOR_MAXIMO_PLAUSIVEL = 500.0

    fun pareceUltimaViagem(texto: String): Boolean {
        val normalizado = texto.replace("\n", " ").lowercase()
        // Exclui o atalho de navegação "Última viagem" da tela inicial do
        // Uber, que só aparece quando o motorista está offline.
        if (normalizado.contains("você está offline") || normalizado.contains("voce esta offline")) {
            return false
        }
        return REGEX_FRASE.containsMatchIn(normalizado)
    }

    /** Acha em qual linha a frase-gatilho aparece — testando uma "janela" de
     * até 3 linhas seguidas, pra pegar tanto frases numa linha só (Uber)
     * quanto quebradas em duas linhas (99: "Valor da útima" / "Corrida >"). */
    private fun encontrarIndiceFrase(linhas: List<String>): Int? {
        for (i in linhas.indices) {
            val janela = listOfNotNull(linhas.getOrNull(i), linhas.getOrNull(i + 1), linhas.getOrNull(i + 2))
                .joinToString(" ")
            if (REGEX_FRASE.containsMatchIn(janela)) return i
        }
        return null
    }

    /** Procura o valor mais próximo ACIMA da linha com a frase-gatilho.
     * Primeiro tenta só valores com "R$" na frente (mais confiável); se não
     * achar nenhum, tenta valores "soltos" (sem R$) na faixa plausível de
     * uma corrida. */
    fun extrairValorUltimaViagem(texto: String): Double? {
        val linhas = texto.lines().map { it.trim() }
        val indiceFrase = encontrarIndiceFrase(linhas) ?: return null

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
