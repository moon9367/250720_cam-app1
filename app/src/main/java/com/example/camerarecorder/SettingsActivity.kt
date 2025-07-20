package com.example.camerarecorder

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.camerarecorder.data.AppSettings
import com.example.camerarecorder.databinding.ActivitySettingsBinding
import com.example.camerarecorder.service.NasTransferService
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private var currentSettings: AppSettings = AppSettings()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        loadSettings()
    }
    
    private fun setupUI() {
        binding.apply {
            // 툴바 설정
            toolbar.setNavigationOnClickListener {
                finish()
            }
            
            // 저장 버튼
            btnSave.setOnClickListener {
                saveSettings()
            }
            
            // 연결 테스트 버튼
            btnTestConnection.setOnClickListener {
                testConnection()
            }
            
            // 전송 방식 라디오 버튼
            radioGroupTransferMode.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.radioRealtime -> currentSettings = currentSettings.copy(
                        transferMode = AppSettings.TransferMode.REALTIME
                    )
                    R.id.radioPeriodic -> currentSettings = currentSettings.copy(
                        transferMode = AppSettings.TransferMode.PERIODIC
                    )
                }
            }
            
            // 자동 시작 체크박스
            checkboxAutoStart.setOnCheckedChangeListener { _, isChecked ->
                currentSettings = currentSettings.copy(autoStart = isChecked)
            }
            
            // 오래된 파일 삭제 체크박스
            checkboxDeleteOldFiles.setOnCheckedChangeListener { _, isChecked ->
                currentSettings = currentSettings.copy(deleteOldFiles = isChecked)
            }
            
            // 1시간 단위 분할 체크박스
            checkboxHourlySplit.setOnCheckedChangeListener { _, isChecked ->
                currentSettings = currentSettings.copy(hourlySplit = isChecked)
            }
        }
    }
    
    private fun loadSettings() {
        currentSettings = AppSettings.load(this)
        
        binding.apply {
            // NAS 설정
            editTextNasAddress.setText(currentSettings.nasAddress)
            editTextNasPort.setText(currentSettings.nasPort.toString())
            editTextNasUsername.setText(currentSettings.nasUsername)
            editTextNasPassword.setText(currentSettings.nasPassword)
            editTextNasShare.setText(currentSettings.nasShare)
            
            // 전송 방식
            when (currentSettings.transferMode) {
                AppSettings.TransferMode.REALTIME -> radioRealtime.isChecked = true
                AppSettings.TransferMode.PERIODIC -> radioPeriodic.isChecked = true
            }
            
            // 파일명 규칙
            editTextFilenamePattern.setText(currentSettings.filenamePattern)
            
            // 기타 설정
            checkboxAutoStart.isChecked = currentSettings.autoStart
            checkboxDeleteOldFiles.isChecked = currentSettings.deleteOldFiles
            editTextMinFreeSpace.setText(currentSettings.minFreeSpaceMB.toString())
            checkboxHourlySplit.isChecked = currentSettings.hourlySplit
        }
    }
    
    private fun saveSettings() {
        try {
            // 입력값 검증
            val nasAddress = binding.editTextNasAddress.text.toString().trim()
            val nasPort = binding.editTextNasPort.text.toString().toIntOrNull() ?: 445
            val nasUsername = binding.editTextNasUsername.text.toString().trim()
            val nasPassword = binding.editTextNasPassword.text.toString()
            val nasShare = binding.editTextNasShare.text.toString().trim()
            val filenamePattern = binding.editTextFilenamePattern.text.toString().trim()
            val minFreeSpace = binding.editTextMinFreeSpace.text.toString().toIntOrNull() ?: 1000
            
            // 필수 필드 검증
            if (nasAddress.isEmpty()) {
                Toast.makeText(this, "NAS 주소를 입력해주세요", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (nasUsername.isEmpty()) {
                Toast.makeText(this, "사용자명을 입력해주세요", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (filenamePattern.isEmpty()) {
                Toast.makeText(this, "파일명 규칙을 입력해주세요", Toast.LENGTH_SHORT).show()
                return
            }
            
            // 설정 업데이트
            currentSettings = currentSettings.copy(
                nasAddress = nasAddress,
                nasPort = nasPort,
                nasUsername = nasUsername,
                nasPassword = nasPassword,
                nasShare = nasShare,
                filenamePattern = filenamePattern,
                minFreeSpaceMB = minFreeSpace
            )
            
            // 설정 저장
            AppSettings.save(this, currentSettings)
            
            Toast.makeText(this, "설정이 저장되었습니다", Toast.LENGTH_SHORT).show()
            finish()
            
        } catch (e: Exception) {
            Toast.makeText(this, "설정 저장 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun testConnection() {
        // 현재 입력된 값으로 임시 설정 생성
        val tempSettings = currentSettings.copy(
            nasAddress = binding.editTextNasAddress.text.toString().trim(),
            nasPort = binding.editTextNasPort.text.toString().toIntOrNull() ?: 445,
            nasUsername = binding.editTextNasUsername.text.toString().trim(),
            nasPassword = binding.editTextNasPassword.text.toString(),
            nasShare = binding.editTextNasShare.text.toString().trim()
        )
        
        if (tempSettings.nasAddress.isEmpty() || tempSettings.nasUsername.isEmpty()) {
            Toast.makeText(this, "NAS 주소와 사용자명을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 임시 설정을 SharedPreferences에 저장하여 서비스에서 사용
        AppSettings.save(this, tempSettings)
        
        // 연결 테스트 서비스 시작
        val intent = android.content.Intent(this, NasTransferService::class.java).apply {
            action = NasTransferService.ACTION_TEST_CONNECTION
        }
        startService(intent)
        
        Toast.makeText(this, "NAS 연결을 테스트합니다", Toast.LENGTH_SHORT).show()
    }
} 