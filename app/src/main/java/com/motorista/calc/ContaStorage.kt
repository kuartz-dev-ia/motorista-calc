package com.motorista.calc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

data class Conta(
    val id: Long,
    val nome: String,
    val categoria: String,
    val valorMensal: Double,
    val dataVencimentoMillis: Long?,
    var pagoMesReferencia: String?,
    val lembreteDias: Int
)

object ContaStorage {
    private const val PREFS_NAME = "motorista_calc_contas"
    private const val CHAVE_LISTA = "lista_contas"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun listarTodos(context: Context): List<Conta> {
        val json = prefs(context).getString(CHAVE_LISTA, null) ?: return emptyList()
        val array = try { JSONArray(json) } catch (e: Exception) { return emptyList() }
        val lista = mutableListOf<Conta>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            lista.add(
                Conta(
                    id = obj.getLong("id"),
                    nome = obj.getString("nome"),
                    categoria = obj.optString("categoria", "Outro"),
                    valorMensal = obj.getDouble("valorMensal"),
                    dataVencimentoMillis = if (obj.has("dataVencimentoMillis") && !obj.isNull("dataVencimentoMillis")) obj.getLong("dataVencimentoMillis") else null,
                    pagoMesReferencia = if (obj.has("pagoMesReferencia") && !obj.isNull("pagoMesReferencia")) obj.getString("pagoMesReferencia") else null,
                    lembreteDias = obj.optInt("lembreteDias", 3)
                )
            )
        }
        return lista
    }

    private fun salvarTudo(context: Context, lista: List<Conta>) {
        val array = JSONArray()
        for (c in lista) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("nome", c.nome)
            obj.put("categoria", c.categoria)
            obj.put("valorMensal", c.valorMensal)
            c.dataVencimentoMillis?.let { obj.put("dataVencimentoMillis", it) }
            c.pagoMesReferencia?.let { obj.put("pagoMesReferencia", it) }
            obj.put("lembreteDias", c.lembreteDias)
            array.put(obj)
        }
        prefs(context).edit().putString(CHAVE_LISTA, array.toString()).apply()
    }

    fun adicionar(context: Context, nome: String, categoria: String, valorMensal: Double, dataVencimentoMillis: Long?, lembreteDias: Int) {
        val lista = listarTodos(context).toMutableList()
        lista.add(0, Conta(System.currentTimeMillis(), nome, categoria, valorMensal, dataVencimentoMillis, null, lembreteDias))
        salvarTudo(context, lista)
    }

    fun atualizar(context: Context, id: Long, nome: String, categoria: String, valorMensal: Double, dataVencimentoMillis: Long?, lembreteDias: Int) {
        val lista = listarTodos(context).toMutableList()
        val idx = lista.indexOfFirst { it.id == id }
        if (idx < 0) return
        val pagoAnterior = lista[idx].pagoMesReferencia
        lista[idx] = Conta(id, nome, categoria, valorMensal, dataVencimentoMillis, pagoAnterior, lembreteDias)
        salvarTudo(context, lista)
    }

    fun apagar(context: Context, id: Long) {
        salvarTudo(context, listarTodos(context).filter { it.id != id })
    }

    fun alternarPaga(context: Context, id: Long) {
        val lista = listarTodos(context).toMutableList()
        val idx = lista.indexOfFirst { it.id == id }
        if (idx < 0) return
        val atual = lista[idx]
        val dataVencimento = atual.dataVencimentoMillis ?: return
        val chaveMes = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(java.util.Date(dataVencimento))
        lista[idx] = atual.copy(pagoMesReferencia = if (atual.pagoMesReferencia == chaveMes) null else chaveMes)
        salvarTudo(context, lista)
    }

    fun totalMensal(context: Context): Double = listarTodos(context).sumOf { it.valorMensal }

    /** Retorna (texto do status, cor hexadecimal) pra exibir na lista. */
    fun statusDaConta(conta: Conta): Pair<String, String> {
        val dataVencimento = conta.dataVencimentoMillis ?: return Pair("Sem data de vencimento", "#8B96AC")

        val chaveMes = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(java.util.Date(dataVencimento))
        if (conta.pagoMesReferencia == chaveMes) {
            return Pair("Paga", "#1FE7A0")
        }

        val diasRestantes = ((dataVencimento - System.currentTimeMillis()) / (24L * 60 * 60 * 1000)).toInt()

        return when {
            diasRestantes < 0 -> Pair("Atrasada há ${-diasRestantes} dia(s)", "#F5576B")
            diasRestantes == 0 -> Pair("Vence hoje", "#F5A623")
            diasRestantes <= conta.lembreteDias -> Pair("Vence em $diasRestantes dia(s)", "#F5A623")
            else -> Pair("Vence em $diasRestantes dia(s)", "#8B96AC")
        }
    }
}
