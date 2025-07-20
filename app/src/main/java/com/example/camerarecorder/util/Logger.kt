package com.example.camerarecorder.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Logger {
    private const val TAG = "CameraRecorder"
    private const val MAX_LOG_SIZE = 10 * 1024 * 1024 // 10MB
    private const val MAX_LOG_FILES = 5
    
    fun d(message: String) {
        Log.d(TAG, message)
        writeToFile("DEBUG", message)
    }
    
    fun i(message: String) {
        Log.i(TAG, message)
        writeToFile("INFO", message)
    }
    
    fun w(message: String) {
        Log.w(TAG, message)
        writeToFile("WARN", message)
    }
    
    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
        writeToFile("ERROR", message + (throwable?.let { "\n${it.stackTraceToString()}" } ?: ""))
    }
    
    private fun writeToFile(level: String, message: String) {
        try {
            val logDir = File(FileManager.getLogsDirectory())
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val logFile = File(logDir, "camera_recorder.log")
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val logEntry = "[$timestamp] $level: $message\n"
            
            FileWriter(logFile, true).use { writer ->
                writer.write(logEntry)
            }
            
            // 로그 파일 크기 관리
            if (logFile.length() > MAX_LOG_SIZE) {
                rotateLogFiles(logDir)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }
    
    private fun rotateLogFiles(logDir: File) {
        try {
            // 기존 백업 파일들을 한 칸씩 뒤로 이동
            for (i in MAX_LOG_FILES - 1 downTo 1) {
                val oldFile = File(logDir, "camera_recorder.log.$i")
                val newFile = File(logDir, "camera_recorder.log.${i + 1}")
                if (oldFile.exists()) {
                    oldFile.renameTo(newFile)
                }
            }
            
            // 현재 로그 파일을 백업
            val currentLog = File(logDir, "camera_recorder.log")
            val backupLog = File(logDir, "camera_recorder.log.1")
            if (currentLog.exists()) {
                currentLog.renameTo(backupLog)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log files", e)
        }
    }
    
    fun getLogs(context: Context): String {
        return try {
            val logFile = File(FileManager.getLogsDirectory(), "camera_recorder.log")
            if (logFile.exists()) {
                logFile.readText()
            } else {
                "로그 파일이 없습니다."
            }
        } catch (e: Exception) {
            "로그를 읽을 수 없습니다: ${e.message}"
        }
    }
    
    fun clearLogs(context: Context) {
        try {
            val logFile = File(FileManager.getLogsDirectory(), "camera_recorder.log")
            if (logFile.exists()) {
                logFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }
    
    fun exportLogs(context: Context): File? {
        return try {
            val logFile = File(FileManager.getLogsDirectory(), "camera_recorder.log")
            if (logFile.exists()) {
                val exportFile = File(FileManager.getExportsDirectory(), "camera_recorder_logs_${System.currentTimeMillis()}.txt")
                logFile.copyTo(exportFile, overwrite = true)
                exportFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export logs", e)
            null
        }
    }
} 