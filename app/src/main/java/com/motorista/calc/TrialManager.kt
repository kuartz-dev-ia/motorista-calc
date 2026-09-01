package com.motorista.calc

import android.content.Context

/** Controla o período de teste do app: grava a data da primeira abertura e
 * calcula quantos dias já se passaram, pra liberar/bloquear o uso. */
object TrialManager {
    private const val PREFS_NAME = "motorista_calc_trial"
    private const val CHAVE_PRIMEIRO_USO = "primeiro_uso_millis"
    const val DIAS_DE_TESTE = 10

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun garantirInicializado(context: Context) {
        val p = prefs(context)
        if (!p.contains(CHAVE_PRIMEIRO_USO)) {
            p.edit().putLong(CHAVE_PRIMEIRO_USO, System.currentTimeMillis()).apply()
        }
    }

    fun diasUsados(context: Context): Int {
        val inicio = prefs(context).getLong(CHAVE_PRIMEIRO_USO, System.currentTimeMillis())
        val diffMillis = System.currentTimeMillis() - inicio
        return (diffMillis / (24L * 60 * 60 * 1000)).toInt()
    }

    fun diasRestantes(context: Context): Int = (DIAS_DE_TESTE - diasUsados(context)).coerceAtLeast(0)

    fun expirou(context: Context): Boolean = diasUsados(context) >= DIAS_DE_TESTE
}
