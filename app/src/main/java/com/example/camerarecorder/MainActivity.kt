package com.example.camerarecorder

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.camerarecorder.data.AppSettings
import com.example.camerarecorder.databinding.ActivityMainBinding
import com.example.camerarecorder.service.CameraRecordingService
import com.example.camerarecorder.service.NasTransferService
import com.example.camerarecorder.util.FileManager
import com.example.camerarecorder.util.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var cameraService: CameraRecordingService? = null
    private var isServiceBound = false
    
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startCameraService()
        } else {
            Toast.makeText(this, "필요한 권한이 허용되지 않았습니다", Toast.LENGTH_LONG).show()
        }
    }
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CameraRecordingService.LocalBinder
            cameraService = binder.getService()
            isServiceBound = true
            updateUI()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            cameraService = null
            isServiceBound = false
            updateUI()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        Logger.i("메인 액티비티 생성")
        
        setupUI()
        checkPermissions()
        bindCameraService()
        
        // 주기적으로 UI 업데이트
        startPeriodicUpdate()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unbindCameraService()
        stopPeriodicUpdate()
    }
    
    private fun setupUI() {
        binding.apply {
            btnStartRecording.setOnClickListener {
                if (checkPermissions()) {
                    startCameraService()
                }
            }
            
            btnStopRecording.setOnClickListener {
                stopCameraService()
            }
            
            btnSettings.setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
            
            btnLogs.setOnClickListener {
                showLogs()
            }
            
            btnTransferAll.setOnClickListener {
                transferAllPendingFiles()
            }
            
            btnTestConnection.setOnClickListener {
                testNasConnection()
            }
        }
    }
    
    private fun checkPermissions(): Boolean {
        val permissionsToRequest = mutableListOf<String>()
        
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
            return false
        }
        
        return true
    }
    
    private fun startCameraService() {
        val intent = Intent(this, CameraRecordingService::class.java).apply {
            action = CameraRecordingService.ACTION_START_RECORDING
        }
        startForegroundService(intent)
        
        Toast.makeText(this, "카메라 녹화를 시작합니다", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopCameraService() {
        val intent = Intent(this, CameraRecordingService::class.java).apply {
            action = CameraRecordingService.ACTION_STOP_RECORDING
        }
        startService(intent)
        
        Toast.makeText(this, "카메라 녹화를 중지합니다", Toast.LENGTH_SHORT).show()
    }
    
    private fun bindCameraService() {
        val intent = Intent(this, CameraRecordingService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun unbindCameraService() {
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }
    
    private fun updateUI() {
        binding.apply {
            val isRecording = cameraService?.isRecording() ?: false
            
            btnStartRecording.isEnabled = !isRecording
            btnStopRecording.isEnabled = isRecording
            
            tvRecordingStatus.text = if (isRecording) "녹화 중" else "중지됨"
            tvRecordingStatus.setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    if (isRecording) android.R.color.holo_red_dark else android.R.color.darker_gray
                )
            )
            
            // 저장소 정보 업데이트
            updateStorageInfo()
            
            // NAS 상태 업데이트
            updateNasStatus()
        }
    }
    
    private fun updateStorageInfo() {
        lifecycleScope.launch {
            val totalSpace = FileManager.getTotalStorageSpace()
            val usedSpace = FileManager.getUsedStorageSpace()
            val availableSpace = FileManager.getAvailableStorageSpace()
            
            val settings = AppSettings.load(this@MainActivity)
            val isLow = FileManager.isStorageLow(settings.minFreeSpaceMB)
            
            binding.tvStorageStatus.text = buildString {
                append("총: ${FileManager.formatFileSize(totalSpace)}\n")
                append("사용: ${FileManager.formatFileSize(usedSpace)}\n")
                append("여유: ${FileManager.formatFileSize(availableSpace)}")
                if (isLow) {
                    append(" (부족)")
                }
            }
            
            binding.tvStorageStatus.setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    if (isLow) android.R.color.holo_red_dark else android.R.color.holo_green_dark
                )
            )
        }
    }
    
    private fun updateNasStatus() {
        lifecycleScope.launch {
            val settings = AppSettings.load(this@MainActivity)
            
            if (settings.nasAddress.isEmpty()) {
                binding.tvNasStatus.text = "NAS 미설정"
                binding.tvNasStatus.setTextColor(
                    ContextCompat.getColor(this@MainActivity, android.R.color.darker_gray)
                )
            } else {
                binding.tvNasStatus.text = "NAS: ${settings.nasAddress}"
                binding.tvNasStatus.setTextColor(
                    ContextCompat.getColor(this@MainActivity, android.R.color.holo_blue_dark)
                )
            }
        }
    }
    
    private fun showLogs() {
        val logs = Logger.getLogs(this)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("로그")
            .setMessage(logs)
            .setPositiveButton("지우기") { _, _ ->
                Logger.clearLogs(this)
                Toast.makeText(this, "로그가 지워졌습니다", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("닫기", null)
            .create()
        
        dialog.show()
    }
    
    private fun transferAllPendingFiles() {
        val intent = Intent(this, NasTransferService::class.java).apply {
            action = NasTransferService.ACTION_TRANSFER_ALL_PENDING
        }
        startService(intent)
        
        Toast.makeText(this, "대기 중인 파일 전송을 시작합니다", Toast.LENGTH_SHORT).show()
    }
    
    private fun testNasConnection() {
        val intent = Intent(this, NasTransferService::class.java).apply {
            action = NasTransferService.ACTION_TEST_CONNECTION
        }
        startService(intent)
        
        Toast.makeText(this, "NAS 연결을 테스트합니다", Toast.LENGTH_SHORT).show()
    }
    
    private fun startPeriodicUpdate() {
        lifecycleScope.launch {
            while (true) {
                updateUI()
                delay(5000) // 5초마다 업데이트
            }
        }
    }
    
    private fun stopPeriodicUpdate() {
        // 코루틴은 lifecycleScope를 사용하므로 자동으로 취소됩니다
    }
} 