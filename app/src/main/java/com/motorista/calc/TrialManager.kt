package com.motorista.calc

import android.content.Context

object TrialManager {

    private const val DIAS_TESTE = 10

    fun garantirInicializado(context: Context) {
        // Nenhuma ação necessária.
    }

    fun expirou(context: Context): Boolean {
        if (LicenseManager.estaBloqueadoExplicitamente(context)) return true
        if (LicenseManager.estaLiberadoPorLicenca(context)) return false
        return diasRestantes(context) <= 0
    }

    fun diasRestantes(context: Context): Int {
        val dataInstalacao = try {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (e: Exception) {
            return DIAS_TESTE
        }
        val diasDecorridos = ((System.currentTimeMillis() - dataInstalacao) / (24L * 60 * 60 * 1000)).toInt()
        return (DIAS_TESTE - diasDecorridos).coerceAtLeast(0)
    }
}
