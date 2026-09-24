package com.motorista.calc

import android.app.Application

/** Classe de Application do app — roda uma única vez, assim que o processo
 * é iniciado, antes de qualquer tela abrir. Usada aqui só pra iniciar a
 * escuta em tempo real da licença no Firebase (ver LicenseManager), que
 * assim fica ativa durante toda a vida do processo, não só quando uma tela
 * específica está aberta. */
class MotoristaCalcApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LicenseManager.iniciarEscutaEmTempoReal(this)
    }
}
