package com.motorista.calc

/** Extrai nome, endereço, data e horário de início a partir do texto lido
 * (via OCR) de um print/panfleto de evento. É uma extração por aproximação
 * (heurística) — funciona bem na maioria dos panfletos, mas sempre deixa os
 * campos editáveis pro usuário corrigir se precisar. */
object EventoTextParser {

    private val REGEX_DATA = Regex("""\b([0-3]?\d)[/.\-]([01]?\d)(?:[/.\-](\d{2,4}))?\b""")
    private val REGEX_HORARIO = Regex("""\b([01]?\d|2[0-3])[:h](\d{2})?\b""", RegexOption.IGNORE_CASE)
    private val PALAVRAS_ENDERECO = listOf(
        "rua", "av.", "avenida", "alameda", "travessa", "estrada", "rodovia",
        "nº", "numero", "número", "bairro", "praça", "arena", "estádio",
        "estadio", "ginásio", "ginasio", "parque", "clube"
    )
    private val REGEX_CIDADE_UF = Regex("""\b([A-ZÀ-Ú][a-zà-ú]+(?:\s[A-ZÀ-Ú][a-zà-ú]+)?)\s*[-/,]\s*([A-Z]{2})\b""")

    fun extrairData(texto: String): String? {
        val match = REGEX_DATA.find(texto) ?: return null
        val dia = match.groupValues[1].padStart(2, '0')
        val mes = match.groupValues[2].padStart(2, '0')
        val ano = match.groupValues[3]
        return if (ano.isNotBlank()) "$dia/$mes/${if (ano.length == 2) "20$ano" else ano}" else "$dia/$mes"
    }

    fun extrairHorarioInicio(texto: String): String? {
        val match = REGEX_HORARIO.find(texto) ?: return null
        val hora = match.groupValues[1].padStart(2, '0')
        val minuto = match.groupValues[2].ifBlank { "00" }
        return "$hora:$minuto"
    }

    fun extrairEndereco(texto: String): String? {
        val linhas = texto.lines().map { it.trim() }.filter { it.isNotBlank() }
        val porPalavraChave = linhas.firstOrNull { linha ->
            val l = linha.lowercase()
            PALAVRAS_ENDERECO.any { l.contains(it) }
        }
        if (porPalavraChave != null) return porPalavraChave
        return REGEX_CIDADE_UF.find(texto)?.value
    }

    fun extrairNome(texto: String, enderecoJaExtraido: String?): String? {
        val linhas = texto.lines().map { it.trim() }.filter { it.isNotBlank() }
        for (linha in linhas) {
            if (linha == enderecoJaExtraido) continue
            if (linha.length < 4) continue
            if (REGEX_HORARIO.matches(linha)) continue
            if (linha.all { !it.isLetter() }) continue
            return linha
        }
        return null
    }
}
