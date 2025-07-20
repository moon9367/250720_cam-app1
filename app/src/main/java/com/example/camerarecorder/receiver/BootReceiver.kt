package com.example.camerarecorder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.camerarecorder.data.AppSettings
import com.example.camerarecorder.service.CameraRecordingService
import com.example.camerarecorder.util.Logger

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_QUICKBOOT_POWERON -> {
                Logger.i("부팅 완료 수신: ${intent.action}")
                
                // 설정 확인
                val settings = AppSettings.load(context)
                if (settings.autoStart) {
                    Logger.i("자동 시작 설정이 활성화되어 있습니다")
                    
                    // 잠시 대기 후 서비스 시작 (시스템 부팅 완료 대기)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        startCameraService(context)
                    }, 30000) // 30초 대기
                } else {
                    Logger.i("자동 시작 설정이 비활성화되어 있습니다")
                }
            }
        }
    }
    
    private fun startCameraService(context: Context) {
        try {
            Logger.i("카메라 녹화 서비스 자동 시작")
            
            val intent = Intent(context, CameraRecordingService::class.java).apply {
                action = CameraRecordingService.ACTION_START_RECORDING
            }
            
            context.startForegroundService(intent)
            
        } catch (e: Exception) {
            Logger.e("카메라 서비스 자동 시작 실패", e)
        }
    }
} 