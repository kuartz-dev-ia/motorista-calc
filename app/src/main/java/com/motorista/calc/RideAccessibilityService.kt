package com.motorista.calc

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

class RideAccessibilityService : AccessibilityService() {

    private val executor = Executors.newSingleThreadExecutor()

    private val recognizer by lazy {
        try {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao criar cliente de OCR: ${e.message}")
            null
        }
    }

    private var ultimoTextoProcessado: String = ""
    private var capturandoNoMomento = false

    private val handler = Handler(Looper.getMainLooper())
    private val pollingIntervalMs = 1500L

    private val pollRunnable = object : Runnable {
        override fun run() {
            try {
                val monitoramentoAtivo = getSharedPreferences(
                    PREFS_NAME,
                    MODE_PRIVATE
                ).getBoolean(
                    PREF_MONITORAMENTO_ATIVO,
                    true
                )

                val testeExpirado =
                    TrialManager.expirou(this@RideAccessibilityService)

                atualizarNotificacaoJornada()

                if (monitoramentoAtivo && !testeExpirado) {
                    verificarLembretePausa()
                    verificarLembreteMeta()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        tentarCapturarEOcr()
                    }
                }

            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Erro no ciclo de captura: ${e.message}"
                )

                capturandoNoMomento = false

            } finally {
                handler.postDelayed(
                    this,
                    pollingIntervalMs
                )
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        Log.d(
            TAG,
            "Serviço de acessibilidade conectado (modo print + OCR)"
        )

        criarCanalPausa()
        criarCanalMeta()
        criarCanalJornada()

        try {
            handler.post(pollRunnable)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Erro ao iniciar polling: ${e.message}"
            )
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // O reconhecimento roda via print + OCR em polling.
        // Este método é mantido porque faz parte do AccessibilityService.
    }

    private fun criarCanalPausa() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val canal = NotificationChannel(
                CANAL_PAUSA_ID,
                "Lembrete de pausa",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            (
                getSystemService(
                    NOTIFICATION_SERVICE
                ) as NotificationManager
            ).createNotificationChannel(canal)
        }
    }

    private fun criarCanalMeta() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val canal = NotificationChannel(
                CANAL_META_ID,
                "Lembrete de meta",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {

                enableVibration(true)

                vibrationPattern = longArrayOf(
                    0,
                    300,
                    200,
                    300,
                    200,
                    300
                )

                val somUri =
                    android.media.RingtoneManager.getDefaultUri(
                        android.media.RingtoneManager.TYPE_NOTIFICATION
                    )

                val atributos =
                    android.media.AudioAttributes.Builder()
                        .setUsage(
                            android.media.AudioAttributes.USAGE_ALARM
                        )
                        .setContentType(
                            android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION
                        )
                        .build()

                setSound(
                    somUri,
                    atributos
                )
            }

            (
                getSystemService(
                    NOTIFICATION_SERVICE
                ) as NotificationManager
            ).createNotificationChannel(canal)
        }
    }

    private fun criarCanalJornada() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val canal = NotificationChannel(
                CANAL_JORNADA_ID,
                "Jornada ao vivo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {

                setSound(
                    null,
                    null
                )

                enableVibration(false)
            }

            (
                getSystemService(
                    NOTIFICATION_SERVICE
                ) as NotificationManager
            ).createNotificationChannel(canal)
        }
    }

    private fun atualizarNotificacaoJornada() {

        val gerenciador =
            getSystemService(
                NOTIFICATION_SERVICE
            ) as NotificationManager

        val jornada =
            JornadaStorage.jornadaAtiva(this)

        if (jornada == null) {

            gerenciador.cancel(
                NOTIFICACAO_JORNADA_ID
            )

            return
        }

        val prefs =
            getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
            )

        val ultimaAtualizacao =
            prefs.getLong(
                PREF_ULTIMA_NOTIFICACAO_JORNADA,
                0L
            )

        if (
            System.currentTimeMillis() -
            ultimaAtualizacao < 20_000L
        ) {
            return
        }

        val stats =
            JornadaStorage.calcularStats(
                this,
                jornada
            )

        val horas =
            stats.tempoTrabalhadoMin / 60

        val minutos =
            stats.tempoTrabalhadoMin % 60

        try {

            val notificacao =
                NotificationCompat.Builder(
                    this,
                    CANAL_JORNADA_ID
                )
                    .setContentTitle(
                        "🚗 Jornada em andamento — %02d:%02d"
                            .format(
                                horas,
                                minutos
                            )
                    )
                    .setContentText(
                        "Ganho: R$ %.2f  •  R$/h: R$ %.2f  •  %.0f%% da meta"
                            .format(
                                stats.ganhoBruto,
                                stats.valorPorHora,
                                stats.percentualMeta
                            )
                    )
                    .setSmallIcon(
                        android.R.drawable.ic_menu_directions
                    )
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(
                        NotificationCompat.PRIORITY_LOW
                    )
                    .build()

            gerenciador.notify(
                NOTIFICACAO_JORNADA_ID,
                notificacao
            )

            prefs.edit()
                .putLong(
                    PREF_ULTIMA_NOTIFICACAO_JORNADA,
                    System.currentTimeMillis()
                )
                .apply()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Erro ao notificar jornada: ${e.message}"
            )
        }
    }

    private fun verificarLembretePausa() {

        val prefs =
            getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
            )

        val inicioSessao =
            prefs.getLong(
                PREF_INICIO_SESSAO,
                0L
            )

        if (inicioSessao <= 0L) {
            return
        }

        val limiteHoras =
            prefs.getFloat(
                PREF_LIMITE_PAUSA_HORAS,
                3.0f
            )

        val horasDecorridas =
            (
                System.currentTimeMillis() -
                inicioSessao
            ) / 3_600_000.0

        if (horasDecorridas >= limiteHoras) {

            enviarNotificacaoPausa(
                horasDecorridas
            )

            prefs.edit()
                .putLong(
                    PREF_INICIO_SESSAO,
                    System.currentTimeMillis()
                )
                .apply()
        }
    }

    private fun enviarNotificacaoPausa(
        horas: Double
    ) {

        try {

            val notificacao =
                NotificationCompat.Builder(
                    this,
                    CANAL_PAUSA_ID
                )
                    .setContentTitle(
                        "☕ Que tal uma pausa?"
                    )
                    .setContentText(
                        "Você já está rodando há %.1f horas seguidas."
                            .format(horas)
                    )
                    .setSmallIcon(
                        android.R.drawable.ic_popup_reminder
                    )
                    .setAutoCancel(true)
                    .build()

            (
                getSystemService(
                    NOTIFICATION_SERVICE
                ) as NotificationManager
            ).notify(
                NOTIFICACAO_PAUSA_ID,
                notificacao
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Erro ao notificar pausa: ${e.message}"
            )
        }
    }

    private fun verificarLembreteMeta() {

        val jornada =
            JornadaStorage.jornadaAtiva(this)
                ?: return

        val prefs =
            getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
            )

        val ultimoLembrete =
            prefs.getLong(
                PREF_ULTIMO_LEMBRETE_META,
                jornada.dataInicioMillis
            )

        if (
            System.currentTimeMillis() -
            ultimoLembrete <
            INTERVALO_LEMBRETE_META_MS
        ) {
            return
        }

        val stats =
            JornadaStorage.calcularStats(
                this,
                jornada
            )

        val horasDecorridas =
            stats.tempoTrabalhadoMin / 60.0

        val metaPorHora =
            if (jornada.cargaHorariaHoras > 0) {
                jornada.metaDiaria /
                    jornada.cargaHorariaHoras
            } else {
                0.0
            }

        val metaAcumuladaAgora =
            metaPorHora *
            horasDecorridas

        val faltaRitmo =
            (
                metaAcumuladaAgora -
                stats.ganhoBruto
            ).coerceAtLeast(0.0)

        val faltaMetaDia =
            (
                jornada.metaDiaria -
                stats.ganhoBruto
            ).coerceAtLeast(0.0)

        enviarNotificacaoMeta(
            faltaRitmo,
            faltaMetaDia,
            jornada.metaDiaria
        )

        prefs.edit()
            .putLong(
                PREF_ULTIMO_LEMBRETE_META,
                System.currentTimeMillis()
            )
            .apply()
    }

    private fun enviarNotificacaoMeta(
        faltaRitmo: Double,
        faltaMetaDia: Double,
        metaDiaria: Double
    ) {

        try {

            val texto =
                if (
                    faltaRitmo <= 0.01 &&
                    faltaMetaDia <= 0.01
                ) {

                    "🎉 Você está em dia com as metas! Continue assim."

                } else {

                    "Faltam R$ %.2f pra ficar em dia com o ritmo da hora. " +
                        "Faltam R$ %.2f pra bater a meta do dia (R$ %.0f)."
                            .format(
                                faltaRitmo,
                                faltaMetaDia,
                                metaDiaria
                            )
                }

            val notificacao =
                NotificationCompat.Builder(
                    this,
                    CANAL_META_ID
                )
                    .setContentTitle(
                        "🎯 Status da meta"
                    )
                    .setContentText(texto)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(texto)
                    )
                    .setSmallIcon(
                        android.R.drawable.ic_dialog_info
                    )
                    .setPriority(
                        NotificationCompat.PRIORITY_HIGH
                    )
                    .setCategory(
                        NotificationCompat.CATEGORY_ALARM
                    )
                    .setAutoCancel(true)
                    .build()

            (
                getSystemService(
                    NOT
