package com.motorista.calc

import java.text.Normalizer
import java.util.Locale

object EventoTextParser {

    private fun normalizar(texto: String): String {
        return Normalizer
            .normalize(texto, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase(Locale.getDefault())
    }

    fun extrairEndereco(texto: String): String? {

        val linhas = texto
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (linhas.isEmpty()) {
            return null
        }

        val palavrasEndereco = listOf(
            "rua",
            "avenida",
            "av.",
            "av ",
            "estrada",
            "rodovia",
            "travessa",
            "praça",
            "praca",
            "largo",
            "boulevard",
            "alameda",
            "logradouro"
        )

        for (linha in linhas) {

            val normalizada = normalizar(linha)

            if (
                palavrasEndereco.any {
                    normalizada.contains(it)
                }
            ) {
                return linha
            }
        }

        val cepRegex =
            Regex("""\b\d{5}-?\d{3}\b""")

        for (linha in linhas) {
            if (cepRegex.containsMatchIn(linha)) {
                return linha
            }
        }

        return null
    }

    fun extrairNome(
        texto: String,
        endereco: String?
    ): String? {

        val linhas = texto
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (linhas.isEmpty()) {
            return null
        }

        val enderecoNormalizado =
            endereco?.let {
                normalizar(it)
            }

        val palavrasIgnoradas = listOf(
            "ingresso",
            "ingressos",
            "entrada",
            "data",
            "horario",
            "horário",
            "local",
            "endereco",
            "endereço",
            "cep",
            "www.",
            "http://",
            "https://",
            "instagram",
            "facebook"
        )

        for (linha in linhas) {

            val normalizada =
                normalizar(linha)

            if (
                enderecoNormalizado != null &&
                normalizada == enderecoNormalizado
            ) {
                continue
            }

            if (
                palavrasIgnoradas.any {
                    normalizada.contains(it)
                }
            ) {
                continue
            }

            if (
                Regex("""\b\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\b""")
                    .containsMatchIn(linha)
            ) {
                continue
            }

            if (
                Regex("""\b\d{1,2}:\d{2}\b""")
                    .containsMatchIn(linha)
            ) {
                continue
            }

            if (linha.length >= 3) {
                return linha
            }
        }

        return null
    }

    fun extrairData(texto: String): String? {

        val padroes = listOf(

            Regex(
                """\b\d{1,2}[/-]\d{1,2}[/-]\d{4}\b"""
            ),

            Regex(
                """\b\d{1,2}[/-]\d{1,2}[/-]\d{2}\b"""
            ),

            Regex(
                """\b\d{1,2}\s+de\s+[a-zA-ZçÇãõáéíóú]+\s+de\s+\d{4}\b""",
                RegexOption.IGNORE_CASE
            ),

            Regex(
                """\b\d{1,2}\s+de\s+[a-zA-ZçÇãõáéíóú]+\b""",
                RegexOption.IGNORE_CASE
            )
        )

        for (regex in padroes) {

            val resultado =
                regex.find(texto)

            if (resultado != null) {
                return resultado.value
            }
        }

        return null
    }

    fun extrairHorarioInicio(texto: String): String? {

        val linhas = texto.lines()

        val regexHorario =
            Regex("""\b([01]?\d|2[0-3])[:hH]([0-5]\d)\b""")

        for (linha in linhas) {

            val normalizada =
                normalizar(linha)

            if (
                normalizada.contains("inicio") ||
                normalizada.contains("comeca") ||
                normalizada.contains("comeco") ||
                normalizada.contains("abertura") ||
                normalizada.contains("horario")
            ) {

                val resultado =
                    regexHorario.find(linha)

                if (resultado != null) {

                    val hora =
                        resultado.groupValues[1]
                            .padStart(2, '0')

                    val minuto =
                        resultado.groupValues[2]

                    return "$hora:$minuto"
                }
            }
        }

        val resultado =
            regexHorario.find(texto)

        if (resultado != null) {

            val hora =
                resultado.groupValues[1]
                    .padStart(2, '0')

            val minuto =
                resultado.groupValues[2]

            return "$hora:$minuto"
        }

        return null
    }
}
