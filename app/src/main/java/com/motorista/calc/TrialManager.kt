package com.motorista.calc

import android.content.Context

/** Não existe mais teste gratuito automático. O único jeito de usar o app é
 * o administrador liberar o ID do aparelho no Firebase (liberado = true).
 * Enquanto isso não acontecer — aparelho novo, nunca cadastrado, ou
 * explicitamente bloqueado — o app trata como "expirado"/bloqueado. */
object TrialManager {

    fun garantirInicializado(context: Context) {
        // Nenhuma ação necessária.
    }

    /** true = não pode usar (precisa de liberação no Firebase). */
    fun expirou(context: Context): Boolean {
        return !LicenseManager.estaLiberadoPorLicenca(context)
    }
}
