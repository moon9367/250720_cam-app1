package com.example.camerarecorder.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object FileManager {
    private const val APP_DIR = "CameraRecorder"
    private const val RECORDINGS_DIR = "recordings"
    private const val LOGS_DIR = "logs"
    private const val EXPORTS_DIR = "exports"
    
    fun getAppDirectory(): String {
        val externalDir = Environment.getExternalStorageDirectory()
        return File(externalDir, APP_DIR).absolutePath
    }
    
    fun getRecordingsDirectory(): String {
        return File(getAppDirectory(), RECORDINGS_DIR).absolutePath
    }
    
    fun getLogsDirectory(): String {
        return File(getAppDirectory(), LOGS_DIR).absolutePath
    }
    
    fun getExportsDirectory(): String {
        return File(getAppDirectory(), EXPORTS_DIR).absolutePath
    }
    
    fun createDirectories() {
        val dirs = listOf(
            getAppDirectory(),
            getRecordingsDirectory(),
            getLogsDirectory(),
            getExportsDirectory()
        )
        
        dirs.forEach { dirPath ->
            val dir = File(dirPath)
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }
    
    fun generateRecordingFileName(pattern: String = "CAM01_날짜_시간"): String {
        val now = LocalDateTime.now()
        val dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val timeStr = now.format(DateTimeFormatter.ofPattern("HHmmss"))
        
        val fileName = pattern
            .replace("날짜", dateStr)
            .replace("시간", timeStr)
        
        return "$fileName.mp4"
    }
    
    fun getRecordingFile(fileName: String): File {
        return File(getRecordingsDirectory(), fileName)
    }
    
    fun getAvailableStorageSpace(): Long {
        val dir = File(getRecordingsDirectory())
        return dir.freeSpace
    }
    
    fun getUsedStorageSpace(): Long {
        val dir = File(getRecordingsDirectory())
        return dir.totalSpace - dir.freeSpace
    }
    
    fun getTotalStorageSpace(): Long {
        val dir = File(getRecordingsDirectory())
        return dir.totalSpace
    }
    
    fun isStorageLow(minFreeSpaceMB: Int): Boolean {
        val freeSpaceMB = getAvailableStorageSpace() / (1024 * 1024)
        return freeSpaceMB < minFreeSpaceMB
    }
    
    fun deleteOldFiles(keepDays: Int = 7) {
        try {
            val recordingsDir = File(getRecordingsDirectory())
            if (!recordingsDir.exists()) return
            
            val cutoffTime = System.currentTimeMillis() - (keepDays * 24 * 60 * 60 * 1000L)
            
            recordingsDir.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        Logger.i("오래된 파일 삭제: ${file.name}")
                    } else {
                        Logger.w("파일 삭제 실패: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("오래된 파일 삭제 중 오류 발생", e)
        }
    }
    
    fun getRecordingFiles(): List<File> {
        val recordingsDir = File(getRecordingsDirectory())
        if (!recordingsDir.exists()) return emptyList()
        
        return recordingsDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() == "mp4" }
            ?.sortedBy { it.lastModified() }
            ?: emptyList()
    }
    
    fun getPendingTransferFiles(): List<File> {
        // TODO: 실제로는 데이터베이스나 설정 파일에서 전송 상태를 확인해야 함
        return getRecordingFiles()
    }
    
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, secs)
            else -> String.format("%02d:%02d", minutes, secs)
        }
    }
} 