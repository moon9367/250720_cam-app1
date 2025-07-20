package com.example.camerarecorder.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.camerarecorder.R
import com.example.camerarecorder.data.AppSettings
import com.example.camerarecorder.util.FileManager
import com.example.camerarecorder.util.Logger
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.*

class CameraRecordingService : Service() {
    
    private val binder = LocalBinder()
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var recordingFile: File? = null
    private var recordingStartTime: LocalDateTime? = null
    
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var wakeLock: PowerManager.WakeLock
    
    private var isRecording = false
    private var settings: AppSettings? = null
    
    // 1시간 타이머
    private var hourlyTimer: Job? = null
    
    inner class LocalBinder : Binder() {
        fun getService(): CameraRecordingService = this@CameraRecordingService
    }
    
    override fun onCreate() {
        super.onCreate()
        Logger.i("카메라 녹화 서비스 생성")
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // WakeLock 획득
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CameraRecorder::RecordingWakeLock"
        )
        wakeLock.acquire()
        
        // 설정 로드
        settings = AppSettings.load(this)
        
        // 디렉토리 생성
        FileManager.createDirectories()
        
        // 저장 공간 확인 및 정리
        checkAndCleanStorage()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording()
            ACTION_RESTART_RECORDING -> restartRecording()
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        Logger.i("카메라 녹화 서비스 종료")
        
        stopRecording()
        cameraExecutor.shutdown()
        
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        
        super.onDestroy()
    }
    
    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun createNotification(): Notification {
        val channelId = "camera_recording"
        val channelName = "카메라 녹화"
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Android 8.0 이상에서는 채널 생성 필요
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = Intent(this, CameraRecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val pendingIntent = PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("카메라 녹화 중")
            .setContentText(if (isRecording) "녹화 진행 중..." else "대기 중")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, "중지", pendingIntent)
            .build()
    }
    
    private fun startRecording() {
        if (isRecording) {
            Logger.w("이미 녹화 중입니다")
            return
        }
        
        Logger.i("카메라 녹화 시작")
        
        startForegroundService()
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                startCameraRecording()
            } catch (e: Exception) {
                Logger.e("카메라 초기화 실패", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun startCameraRecording() {
        try {
            val cameraProvider = cameraProvider ?: return
            
            // 카메라 선택 (후면 카메라)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            // 녹화 품질 설정
            val qualitySelector = QualitySelector.from(
                Quality.HIGHEST,
                FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
            )
            
            // 녹화 설정
            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()
            
            videoCapture = VideoCapture.withOutput(recorder)
            
            // 카메라 바인딩 - ProcessLifecycleOwner 사용
            camera = cameraProvider.bindToLifecycle(
                ProcessLifecycleOwner.get(),
                cameraSelector,
                videoCapture
            )
            
            // 녹화 시작
            startVideoRecording()
            
        } catch (e: Exception) {
            Logger.e("카메라 녹화 시작 실패", e)
        }
    }
    
    private fun startVideoRecording() {
        try {
            val videoCapture = videoCapture ?: return
            
            // 녹화 파일 생성
            val fileName = FileManager.generateRecordingFileName(
                settings?.filenamePattern ?: "CAM01_날짜_시간"
            )
            recordingFile = FileManager.getRecordingFile(fileName)
            
            val fileOptions = FileOutputOptions.Builder(recordingFile!!).build()
            
            recordingStartTime = LocalDateTime.now()
            
            recording = videoCapture.output
                .prepareRecording(this, fileOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            Logger.i("녹화 시작: ${recordingFile?.name}")
                            isRecording = true
                            startHourlyTimer()
                            updateNotification()
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (recordEvent.hasError()) {
                                Logger.e("녹화 오류: ${recordEvent.error}")
                                isRecording = false
                                stopHourlyTimer()
                            } else {
                                Logger.i("녹화 완료: ${recordingFile?.name}")
                                onRecordingCompleted()
                            }
                        }
                    }
                }
                
        } catch (e: Exception) {
            Logger.e("비디오 녹화 시작 실패", e)
        }
    }
    
    private fun startHourlyTimer() {
        if (!(settings?.hourlySplit ?: true)) return
        
        hourlyTimer = CoroutineScope(Dispatchers.Main).launch {
            delay(60 * 60 * 1000) // 1시간 대기
            if (isRecording) {
                Logger.i("1시간 경과, 녹화 분할")
                stopAndRestartRecording()
            }
        }
    }
    
    private fun stopHourlyTimer() {
        hourlyTimer?.cancel()
        hourlyTimer = null
    }
    
    private fun stopAndRestartRecording() {
        recording?.stop()
        recording = null
        
        // 잠시 대기 후 새로운 녹화 시작
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000) // 1초 대기
            startVideoRecording()
        }
    }
    
    private fun onRecordingCompleted() {
        isRecording = false
        stopHourlyTimer()
        
        recordingFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                Logger.i("녹화 파일 생성 완료: ${file.name} (${FileManager.formatFileSize(file.length())})")
                
                // NAS 전송 서비스 시작
                startNasTransferService(file)
                
                // 저장 공간 확인
                checkAndCleanStorage()
            }
        }
        
        // 새로운 녹화 시작
        if (settings?.autoStart == true) {
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000) // 1초 대기
                startVideoRecording()
            }
        }
    }
    
    private fun stopRecording() {
        Logger.i("카메라 녹화 중지")
        
        isRecording = false
        stopHourlyTimer()
        
        recording?.stop()
        recording = null
        
        cameraProvider?.unbindAll()
        camera = null
        videoCapture = null
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun restartRecording() {
        Logger.i("카메라 녹화 재시작")
        stopRecording()
        startRecording()
    }
    
    private fun startNasTransferService(file: File) {
        val intent = Intent(this, NasTransferService::class.java).apply {
            action = NasTransferService.ACTION_TRANSFER_FILE
            putExtra(NasTransferService.EXTRA_FILE_PATH, file.absolutePath)
        }
        startService(intent)
    }
    
    private fun checkAndCleanStorage() {
        settings?.let { settings ->
            if (settings.deleteOldFiles && FileManager.isStorageLow(settings.minFreeSpaceMB)) {
                Logger.w("저장 공간 부족, 오래된 파일 삭제 시작")
                FileManager.deleteOldFiles()
            }
        }
    }
    
    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    fun isRecording(): Boolean = isRecording
    
    fun getRecordingStatus(): String = if (isRecording) "녹화 중" else "중지됨"
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START_RECORDING = "start_recording"
        const val ACTION_STOP_RECORDING = "stop_recording"
        const val ACTION_RESTART_RECORDING = "restart_recording"
    }
} 