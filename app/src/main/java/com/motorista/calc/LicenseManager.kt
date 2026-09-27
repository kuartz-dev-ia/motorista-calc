package com.motorista.calc

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source

object LicenseManager {
    private const val COLECAO = "licencas"
    private const val PREF_ULTIMO_STATUS = "licenca_ultimo_status_liberado"
    private const val PREF_ULTIMA_VERIFICACAO = "licenca_ultima_verificacao_millis"
    private const val PREF_DOCUMENTO_EXISTE = "licenca_documento_existe"
    private const val PREF_ULTIMO_ERRO = "licenca_ultimo_erro"
    private const val DIAS_TOLERANCIA_OFFLINE = 3

    @Volatile
    private var listenerAtivo: ListenerRegistration? = null

    fun obterIdDispositivo(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "id-desconhecido"
        } catch (t: Throwable) {
            "id-desconhecido"
        }
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
                    try {
                        if (erro != null) {
                            registrarErro(context, "Escuta em tempo real: ${erro.message}")
                            return@addSnapshotListener
                        }
                        if (doc == null) return@addSnapshotListener
                        salvarResultado(context, doc.exists(), doc.getBoolean("liberado") ?: false)
                        registrarErro(context, null)
                    } catch (t: Throwable) {
                        Log.e("LicenseManager", "Erro processando atualização da licença: ${t.message}")
                    }
                }
        } catch (t: Throwable) {
            Log.e("LicenseManager", "Erro ao iniciar escuta do Firebase: ${t.message}")
            registrarErro(context, "Erro ao iniciar escuta: ${t.message}")
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
                    try {
                        salvarResultado(context, doc.exists(), doc.getBoolean("liberado") ?: false)
                        registrarErro(context, null)
                    } catch (t: Throwable) { }
                }
                .addOnFailureListener { erro ->
                    registrarErro(context, "Consulta pontual: ${erro.message}")
                }
        } catch (t: Throwable) {
            registrarErro(context, "Erro na verificação: ${t.message}")
        }
    }

    private fun registrarErro(context: Context, mensagem: String?) {
        try {
            if (mensagem == null) {
                prefs(context).edit().remove(PREF_ULTIMO_ERRO).apply()
            } else {
                val hora = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                prefs(context).edit().putString(PREF_ULTIMO_ERRO, "[$hora] $mensagem").apply()
            }
        } catch (t: Throwable) { }
    }

    fun obterUltimoErro(context: Context): String? {
        return try { prefs(context).getString(PREF_ULTIMO_ERRO, null) } catch (t: Throwable) { null }
    }

    fun obterDiagnostico(context: Context): String {
        val p = prefs(context)
        val id = obterIdDispositivo(context)
        val documentoExiste = try { p.getBoolean(PREF_DOCUMENTO_EXISTE, false) } catch (t: Throwable) { false }
        val ultimoStatus = try { p.getBoolean(PREF_ULTIMO_STATUS, false) } catch (t: Throwable) { false }
        val ultimaVerificacao = try { p.getLong(PREF_ULTIMA_VERIFICACAO, 0L) } catch (t: Throwable) { 0L }
        val dataFormatada = if (ultimaVerificacao > 0) {
            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(ultimaVerificacao))
        } else "nunca"
        val erro = obterUltimoErro(context)

        return buildString {
            append("ID do aparelho: $id\n\n")
            append("Documento existe no Firebase: ${if (documentoExiste) "SIM" else "NÃO"}\n")
            append("Campo 'liberado': ${if (ultimoStatus) "true" else "false"}\n")
            append("Última verificação bem-sucedida: $dataFormatada\n\n")
            if (erro != null) {
                append("ÚLTIMO ERRO:\n$erro")
            } else {
                append("Nenhum erro registrado.")
            }
        }
    }

    private fun salvarResultado(context: Context, documentoExiste: Boolean, liberado: Boolean) {
        try {
            val editor = prefs(context).edit().putBoolean(PREF_DOCUMENTO_EXISTE, documentoExiste)
            if (documentoExiste) {
                editor.putBoolean(PREF_ULTIMO_STATUS, liberado)
                editor.putLong(PREF_ULTIMA_VERIFICACAO, System.currentTimeMillis())
            }
            editor.apply()
        } catch (t: Throwable) { }
    }

    fun estaBloqueadoExplicitamente(context: Context): Boolean {
        return try {
            val p = prefs(context)
            val documentoExiste = p.getBoolean(PREF_DOCUMENTO_EXISTE, false)
            if (!documentoExiste) return false
            !p.getBoolean(PREF_ULTIMO_STATUS, false)
        } catch (t: Throwable) {
            false
        }
    }

    fun estaLiberadoPorLicenca(context: Context): Boolean {
        return try {
            val p = prefs(context)
            val ultimoStatus = p.getBoolean(PREF_ULTIMO_STATUS, false)
            if (!ultimoStatus) return false

            val ultimaVerificacao = p.getLong(PREF_ULTIMA_VERIFICACAO, 0L)
            if (ultimaVerificacao == 0L) return false

            val diasDesde = (System.currentTimeMillis() - ultimaVerificacao) / (24L * 60 * 60 * 1000)
            diasDesde <= DIAS_TOLERANCIA_OFFLINE
        } catch (t: Throwable) {
            false
        }
    }
}
