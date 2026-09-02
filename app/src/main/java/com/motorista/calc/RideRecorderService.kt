package com.motorista.calc

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class RideRecorderService : Service() {

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var mediaRecorder: MediaRecorder? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoStopRunnable: Runnable? = null
    private var arquivoAtual: java.io.File? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        criarCanalNotificacao()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> iniciarGravacao()
            ACTION_STOP -> pararGravacao()
        }
        return START_NOT_STICKY
    }

    private fun criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CANAL_ID, "Gravação de corrida", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(canal)
        }
    }

    private fun montarNotificacao(): android.app.Notification {
        return NotificationCompat.Builder(this, CANAL_ID)
            .setContentTitle("🔴 Gravando corrida")
            .setContentText("Motorista Calc está gravando vídeo e áudio")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setOngoing(true)
            .build()
    }

    private fun iniciarGravacao() {
        if (emGravacao) return

        val temPermissoes = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!temPermissoes) {
            Log.e(TAG, "Sem permissão de câmera/microfone")
            stopSelf()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val tipoServico = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            startForeground(NOTIFICACAO_ID, montarNotificacao(), tipoServico)
        } else {
            startForeground(NOTIFICACAO_ID, montarNotificacao())
        }

        backgroundThread = HandlerThread("CameraGravacao").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)

        arquivoAtual = RecordingsStorage.novoArquivo(this)

        try {
            abrirCameraFrontal()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir câmera: ${e.message}")
            pararGravacao()
        }
    }

    @Suppress("MissingPermission")
    private fun abrirCameraFrontal() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val idFrontal = cameraManager.cameraIdList.firstOrNull {
            cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        } ?: cameraManager.cameraIdList.firstOrNull()

        if (idFrontal == null) {
            Log.e(TAG, "Nenhuma câmera encontrada")
            pararGravacao()
            return
        }

        val caracteristicas = cameraManager.getCameraCharacteristics(idFrontal)
        val sensorOrientation = caracteristicas.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90

        mediaRecorder = criarMediaRecorder(sensorOrientation)

        cameraManager.openCamera(idFrontal, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                criarSessaoDeCaptura()
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Log.e(TAG, "Erro na câmera: $error")
                camera.close()
                cameraDevice = null
                pararGravacao()
            }
        }, backgroundHandler)
    }

    private fun criarMediaRecorder(sensorOrientation: Int): MediaRecorder {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setOutputFile(arquivoAtual!!.absolutePath)
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setVideoSize(1280, 720)
        recorder.setVideoFrameRate(25)
        recorder.setVideoEncodingBitRate(4_000_000)
        recorder.setAudioEncodingBitRate(128_000)
        recorder.setAudioSamplingRate(44100)
        val hint = (360 - ((sensorOrientation + 270) % 360)) % 360
        recorder.setOrientationHint(hint)
        recorder.prepare()
        return recorder
    }

    private fun criarSessaoDeCaptura() {
        val camera = cameraDevice ?: return
        val recorder = mediaRecorder ?: return
        val surface = recorder.surface

        val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            addTarget(surface)
        }

        camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                try {
                    session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                    recorder.start()
                    emGravacao = true
                    agendarParadaAutomatica()
                    mainHandler.post {
                        Toast.makeText(this@RideRecorderService, "🔴 Gravação iniciada", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao iniciar gravação: ${e.message}")
                    pararGravacao()
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e(TAG, "Falha ao configurar sessão de captura")
                pararGravacao()
            }
        }, backgroundHandler)
    }

    private fun agendarParadaAutomatica() {
        autoStopRunnable = Runnable { pararGravacao() }
        mainHandler.postDelayed(autoStopRunnable!!, DURACAO_MAXIMA_MS)
    }

    private fun pararGravacao() {
        val estavaGravando = emGravacao

        autoStopRunnable?.let { mainHandler.removeCallbacks(it) }
        autoStopRunnable = null

        try { captureSession?.close() } catch (e: Exception) { }
        captureSession = null

        try {
            if (emGravacao) mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao parar o gravador: ${e.message}")
        }
        try { mediaRecorder?.release() } catch (e: Exception) { }
        mediaRecorder = null

        try { cameraDevice?.close() } catch (e: Exception) { }
        cameraDevice = null

        backgroundThread?.quitSafely()
        backgroundThread = null
        backgroundHandler = null

        emGravacao = false

        if (estavaGravando) {
            mainHandler.post {
                Toast.makeText(this, "⏹️ Gravação encerrada", Toast.LENGTH_SHORT).show()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (emGravacao) pararGravacao()
    }

    companion object {
        private const val TAG = "RideRecorder"
        private const val CANAL_ID = "gravacao_corrida"
        private const val NOTIFICACAO_ID = 991
        private const val DURACAO_MAXIMA_MS = 60L * 60 * 1000

        const val ACTION_START = "com.motorista.calc.action.START_RECORDING"
        const val ACTION_STOP = "com.motorista.calc.action.STOP_RECORDING"

        @Volatile
        var emGravacao: Boolean = false
            private set
    }
}
