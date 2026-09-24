package com.motorista.calc

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source

/** Controla a liberação remota do app via Firebase Firestore. Cada aparelho
 * tem um ID único; você libera ou bloqueia cada ID direto no site do
 * Firebase, sem precisar mexer no app.
 *
 * Usa duas fontes de verificação:
 * 1) Uma ESCUTA EM TEMPO REAL (addSnapshotListener), iniciada uma vez em
 *    MotoristaCalcApp — enquanto o app tiver processo vivo e internet,
 *    qualquer mudança no Firebase chega quase na hora, sem esperar reabrir
 *    uma tela.
 * 2) Uma consulta pontual (verificarEmSegundoPlano), chamada no onResume das
 *    telas principais, como reforço caso a escuta não tenha pego por algum
 *    motivo (app reaberto depois de ficar fechado, por exemplo).
 *
 * Quando o Firebase já confirmou explicitamente "liberado = false" pra esse
 * aparelho, o bloqueio é IMEDIATO e não dá direito a nenhum teste
 * gratuito — diferente de simplesmente "não estar liberado" (aparelho novo,
 * nunca cadastrado), que ainda usa o teste de 10 dias normalmente. */
object LicenseManager {
    private const val COLECAO = "licencas"
    private const val PREF_ULTIMO_STATUS = "licenca_ultimo_status_liberado"
    private const val PREF_ULTIMA_VERIFICACAO = "licenca_ultima_verificacao_millis"
    private const val PREF_DOCUMENTO_EXISTE = "licenca_documento_existe"
    private const val DIAS_TOLERANCIA_OFFLINE = 3

    @Volatile
    private var listenerAtivo: ListenerRegistration? = null

    fun obterIdDispositivo(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "id-desconhecido"
    }

    private fun prefs(context: Context) = context.getSharedPreferences(RideAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE)

    fun iniciarEscutaEmTempoReal(context: Context) {
        if (listenerAtivo != null) return
        try {
            val id = obterIdDispositivo(context)
            listenerAtivo = FirebaseFirestore.getInstance()
                .collection(COLECAO)
                .document(id)
                .addSnapshotListener { doc, erro ->
                    if (erro != null || doc == null) return@addSnapshotListener
                    salvarResultado(context, doc.exists(), doc.getBoolean("liberado") ?: false)
                }
        } catch (e: Exception) {
            Log.e("LicenseManager", "Erro ao iniciar escuta do Firebase: ${e.message}")
        }
    }

    fun verificarEmSegundoPlano(context: Context) {
        try {
            val id = obterIdDispositivo(context)
            FirebaseFirestore.getInstance()
                .collection(COLECAO)
                .document(id)
                .get(Source.SERVER)
                .addOnSuccessListener { doc ->
                    salvarResultado(context, doc.exists(), doc.getBoolean("liberado") ?: false)
                }
                .addOnFailureListener { }
        } catch (e: Exception) { }
    }

    private fun salvarResultado(context: Context, documentoExiste: Boolean, liberado: Boolean) {
        val editor = prefs(context).edit().putBoolean(PREF_DOCUMENTO_EXISTE, documentoExiste)
        if (documentoExiste) {
            editor.putBoolean(PREF_ULTIMO_STATUS, liberado)
            editor.putLong(PREF_ULTIMA_VERIFICACAO, System.currentTimeMillis())
        }
        editor.apply()
    }

    /** true quando o Firebase JÁ confirmou explicitamente que esse aparelho
     * está bloqueado (documento existe e liberado = false) — bloqueio na
     * hora, sem teste gratuito nem tolerância offline. */
    fun estaBloqueadoExplicitamente(context: Context): Boolean {
        val p = prefs(context)
        val documentoExiste = p.getBoolean(PREF_DOCUMENTO_EXISTE, false)
        if (!documentoExiste) return false
        return !p.getBoolean(PREF_ULTIMO_STATUS, false)
    }

    /** true se liberado por licença remota confirmada, dentro da margem de
     * tolerância offline (só vale enquanto o último status confirmado foi
     * "liberado"). */
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
