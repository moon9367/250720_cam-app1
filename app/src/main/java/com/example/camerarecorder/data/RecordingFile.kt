package com.example.camerarecorder.data

import java.io.File
import java.time.LocalDateTime

data class RecordingFile(
    val file: File,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val fileSize: Long = 0,
    val transferStatus: TransferStatus = TransferStatus.PENDING,
    val transferAttempts: Int = 0,
    val lastTransferAttempt: LocalDateTime? = null,
    val transferError: String? = null
) {
    enum class TransferStatus {
        PENDING, TRANSFERRING, COMPLETED, FAILED
    }
    
    val duration: Long
        get() = endTime?.let { 
            java.time.Duration.between(startTime, it).toSeconds() 
        } ?: 0
    
    val isCompleted: Boolean
        get() = endTime != null
    
    val filename: String
        get() = file.name
    
    val path: String
        get() = file.absolutePath
    
    fun updateTransferStatus(status: TransferStatus, error: String? = null): RecordingFile {
        return copy(
            transferStatus = status,
            transferError = error,
            transferAttempts = if (status == TransferStatus.FAILED) transferAttempts + 1 else transferAttempts,
            lastTransferAttempt = if (status != TransferStatus.PENDING) LocalDateTime.now() else lastTransferAttempt
        )
    }
    
    fun markAsCompleted(endTime: LocalDateTime): RecordingFile {
        return copy(
            endTime = endTime,
            fileSize = file.length()
        )
    }
    
    companion object {
        fun create(file: File, startTime: LocalDateTime): RecordingFile {
            return RecordingFile(
                file = file,
                startTime = startTime,
                fileSize = file.length()
            )
        }
    }
} 