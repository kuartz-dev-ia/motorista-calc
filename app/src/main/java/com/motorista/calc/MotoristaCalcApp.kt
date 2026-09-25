package com.motorista.calc

import android.app.Application
import android.util.Log

/** Classe de Application do app — roda uma única vez, antes de qualquer
 * tela abrir. Só inicia a escuta remota da licença aqui.
 *
 * IMPORTANTE: envolvida num try/catch(Throwable) — se o Firebase falhar por
 * qualquer motivo (rede, configuração, versão), o app continua abrindo
 * normalmente (só sem a checagem de licença em tempo real, que ainda assim
 * roda de novo, com segurança, no onResume das telas principais). Sem essa
 * proteção, qualquer falha aqui derruba o aplicativo inteiro antes mesmo da
 * primeira tela aparecer — sem mensagem nenhuma, exatamente como estava
 * acontecendo. */
class MotoristaCalcApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            LicenseManager.iniciarEscutaEmTempoReal(this)
        } catch (t: Throwable) {
            Log.e("MotoristaCalcApp", "Erro ao iniciar licença remota: ${t.message}")
        }
    }
}
