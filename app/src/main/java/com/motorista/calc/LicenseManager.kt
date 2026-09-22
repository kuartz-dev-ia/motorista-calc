package com.motorista.calc

import android.content.Context
import android.provider.Settings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

/** Controla a liberação remota do app via Firebase Firestore. Cada aparelho
 * tem um ID único (gerado pelo próprio Android); você libera ou bloqueia
 * cada ID direto no site do Firebase, sem precisar mexer no app. O app
 * guarda o último status confirmado localmente, pra continuar funcionando
 * alguns dias mesmo sem internet no momento — depois disso, exige conexão de
 * novo pra confirmar que ainda está liberado. */
object LicenseManager {
    private const val COLECAO = "licencas"
    private const val PREF_ULTIMO_STATUS = "licenca_ultimo_status_liberado"
    private const val PREF_ULTIMA_VERIFICACAO = "licenca_ultima_verificacao_millis"
    private const val DIAS_TOLERANCIA_OFFLINE = 3

    fun obterIdDispositivo(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "id-desconhecido"
    }

    private fun prefs(context: Context) = context.getSharedPreferences(RideAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE)

    /** Consulta o Firestore em segundo plano (não trava a tela) e atualiza o
     * cache local com o resultado. Chame isso sempre que uma tela principal
     * abrir (onResume). */
    fun verificarEmSegundoPlano(context: Context) {
        val id = obterIdDispositivo(context)
        try {
            FirebaseFirestore.getInstance()
                .collection(COLECAO)
                .document(id)
                .get(Source.SERVER)
                .addOnSuccessListener { doc ->
                    val liberado = doc.getBoolean("liberado") ?: false
                    prefs(context).edit()
                        .putBoolean(PREF_ULTIMO_STATUS, liberado)
                        .putLong(PREF_ULTIMA_VERIFICACAO, System.currentTimeMillis())
                        .apply()
                }
                .addOnFailureListener {
                    // Sem internet agora ou documento não existe ainda —
                    // mantém o último status conhecido, sem travar nada.
                }
        } catch (e: Exception) {
            // Firebase pode falhar ao inicializar em casos raros — ignora
            // silenciosamente, o teste gratuito local continua valendo.
        }
    }

    /** true se o app está liberado por licença remota — considerando o
     * último status confirmado, com uma margem de dias offline. */
    fun estaLiberadoPorLicenca(context: Context): Boolean {
        val p = prefs(context)
        val ultimoStatus = p.getBoolean(PREF_ULTIMO_STATUS, false)
        if (!ultimoStatus) return false

        val ultimaVerificacao = p.getLong(PREF_ULTIMA_VERIFICACAO, 0L)
        if (ultimaVerificacao == 0L) return false

        val diasDesde = (System.currentTimeMillis() - ultimaVerificacao) / (24L * 60 * 60 * 1000)
        return diasDesde <= DIAS_TOLERANCIA_OFFLINE
    }
}
