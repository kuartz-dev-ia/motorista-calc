package com.motorista.calc

import android.content.Context
import android.content.pm.PackageManager

/** Controla o período de teste do app usando a data de instalação do APK
 * (informação do próprio Android, que não é apagada ao "limpar dados" do
 * app — só some se a pessoa desinstalar e instalar de novo). */
object TrialManager {
    const val DIAS_DE_TESTE = 10

    private fun dataDeInstalacaoMillis(context: Context): Long {
        return try {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.firstInstallTime
        } catch (e: PackageManager.NameNotFoundException) {
            System.currentTimeMillis()
        }
    }

    fun garantirInicializado(context: Context) {
        // Não precisa mais salvar nada manualmente — a data de instalação
        // já é mantida pelo próprio sistema Android.
    }

    fun diasUsados(context: Context): Int {
        val inicio = dataDeInstalacaoMillis(context)
        val diffMillis = (System.currentTimeMillis() - inicio).coerceAtLeast(0)
        return (diffMillis / (24L * 60 * 60 * 1000)).toInt()
    }

    fun diasRestantes(context: Context): Int = (DIAS_DE_TESTE - diasUsados(context)).coerceAtLeast(0)

    fun expirou(context: Context): Boolean = diasUsados(context) >= DIAS_DE_TESTE
}
