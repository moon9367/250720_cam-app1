package com.example.camerarecorder.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.camerarecorder.R
import com.example.camerarecorder.data.AppSettings
import com.example.camerarecorder.network.NasManager
import com.example.camerarecorder.util.FileManager
import com.example.camerarecorder.util.Logger
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.*

class NasTransferService : LifecycleService() {
    
    private val nasManager = NasManager()
    private var settings: AppSettings? = null
    private var isTransferring = false
    
    override fun onCreate() {
        super.onCreate()
        Logger.i("NAS 전송 서비스 생성")
        settings = AppSettings.load(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        when (intent?.action) {
            ACTION_TRANSFER_FILE -> {
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
                if (filePath != null) {
                    transferFile(File(filePath))
                }
            }
            ACTION_TRANSFER_ALL_PENDING -> {
                transferAllPendingFiles()
            }
            ACTION_TEST_CONNECTION -> {
                testConnection()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
    
    override fun onDestroy() {
        Logger.i("NAS 전송 서비스 종료")
        nasManager.disconnect()
        super.onDestroy()
    }
    
    private fun transferFile(file: File) {
        if (isTransferring) {
            Logger.w("이미 전송 중입니다")
            return
        }
        
        if (!file.exists()) {
            Logger.e("전송할 파일이 존재하지 않습니다: ${file.absolutePath}")
            return
        }
        
        val settings = settings ?: return
        
        if (settings.nasAddress.isEmpty() || settings.nasUsername.isEmpty()) {
            Logger.w("NAS 설정이 완료되지 않았습니다")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                isTransferring = true
                startForegroundService()
                
                Logger.i("NAS 전송 시작: ${file.name}")
                
                // NAS 연결
                val connected = nasManager.connect(settings)
                if (!connected) {
                    Logger.e("NAS 연결 실패")
                    updateNotification("NAS 연결 실패")
                    return@launch
                }
                
                updateNotification("전송 중: ${file.name}")
                
                // 원격 경로 생성
                val dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                val remotePath = "/${dateStr}/${file.name}"
                
                // 파일 전송
                val success = nasManager.uploadFile(file, remotePath)
                
                if (success) {
                    Logger.i("파일 전송 완료: ${file.name}")
                    updateNotification("전송 완료: ${file.name}")
                    
                    // 전송 완료 후 로컬 파일 삭제 (선택사항)
                    if (settings.transferMode == AppSettings.TransferMode.REALTIME) {
                        if (file.delete()) {
                            Logger.i("로컬 파일 삭제: ${file.name}")
                        }
                    }
                } else {
                    Logger.e("파일 전송 실패: ${file.name}")
                    updateNotification("전송 실패: ${file.name}")
                }
                
            } catch (e: Exception) {
                Logger.e("파일 전송 중 오류 발생: ${file.name}", e)
                updateNotification("전송 오류: ${file.name}")
            } finally {
                isTransferring = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }
    
    private fun transferAllPendingFiles() {
        val pendingFiles = FileManager.getPendingTransferFiles()
        if (pendingFiles.isEmpty()) {
            Logger.i("전송 대기 중인 파일이 없습니다")
            return
        }
        
        Logger.i("대기 중인 파일 전송 시작: ${pendingFiles.size}개")
        
        CoroutineScope(Dispatchers.IO).launch {
            val settings = settings ?: return@launch
            
            // NAS 연결
            val connected = nasManager.connect(settings)
            if (!connected) {
                Logger.e("NAS 연결 실패, 전송을 건너뜁니다")
                return@launch
            }
            
            var successCount = 0
            var failCount = 0
            
            for (file in pendingFiles) {
                try {
                    updateNotification("전송 중: ${file.name} (${successCount + failCount + 1}/${pendingFiles.size})")
                    
                    val dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                    val remotePath = "/${dateStr}/${file.name}"
                    
                    val success = nasManager.uploadFile(file, remotePath)
                    
                    if (success) {
                        successCount++
                        Logger.i("파일 전송 완료: ${file.name}")
                        
                        // 전송 완료 후 로컬 파일 삭제
                        if (file.delete()) {
                            Logger.i("로컬 파일 삭제: ${file.name}")
                        }
                    } else {
                        failCount++
                        Logger.e("파일 전송 실패: ${file.name}")
                    }
                    
                    // 전송 간격 조절
                    delay(1000)
                    
                } catch (e: Exception) {
                    failCount++
                    Logger.e("파일 전송 중 오류 발생: ${file.name}", e)
                }
            }
            
            Logger.i("전송 완료: 성공 $successCount개, 실패 $failCount개")
            updateNotification("전송 완료: 성공 $successCount개, 실패 $failCount개")
            
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
    
    private fun testConnection() {
        val settings = settings ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                startForegroundService()
                updateNotification("NAS 연결 테스트 중...")
                
                val connected = nasManager.testConnection(settings)
                
                if (connected) {
                    Logger.i("NAS 연결 테스트 성공")
                    updateNotification("NAS 연결 테스트 성공")
                } else {
                    Logger.e("NAS 연결 테스트 실패")
                    updateNotification("NAS 연결 테스트 실패")
                }
                
            } catch (e: Exception) {
                Logger.e("NAS 연결 테스트 중 오류 발생", e)
                updateNotification("NAS 연결 테스트 오류")
            } finally {
                delay(3000) // 3초 후 알림 제거
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }
    
    private fun startForegroundService() {
        val notification = createNotification("NAS 전송 준비 중...")
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun createNotification(message: String): Notification {
        val channelId = "nas_transfer"
        val channelName = "NAS 전송"
        
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
        
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("NAS 전송")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    fun isTransferring(): Boolean = isTransferring
    
    fun getNasStatus(): String = nasManager.getConnectionStatus()
    
    companion object {
        private const val NOTIFICATION_ID = 1002
        const val ACTION_TRANSFER_FILE = "transfer_file"
        const val ACTION_TRANSFER_ALL_PENDING = "transfer_all_pending"
        const val ACTION_TEST_CONNECTION = "test_connection"
        const val EXTRA_FILE_PATH = "file_path"
    }
} 