package com.example.camerarecorder.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

data class AppSettings(
    val nasAddress: String = "",
    val nasPort: Int = 445,
    val nasUsername: String = "",
    val nasPassword: String = "",
    val nasShare: String = "",
    val transferMode: TransferMode = TransferMode.REALTIME,
    val filenamePattern: String = "CAM01_날짜_시간",
    val autoStart: Boolean = true,
    val deleteOldFiles: Boolean = true,
    val minFreeSpaceMB: Int = 1000,
    val hourlySplit: Boolean = true
) {
    enum class TransferMode {
        REALTIME, PERIODIC
    }
    
    companion object {
        private const val PREFS_NAME = "camera_recorder_settings"
        private const val KEY_NAS_ADDRESS = "nas_address"
        private const val KEY_NAS_PORT = "nas_port"
        private const val KEY_NAS_USERNAME = "nas_username"
        private const val KEY_NAS_PASSWORD = "nas_password"
        private const val KEY_NAS_SHARE = "nas_share"
        private const val KEY_TRANSFER_MODE = "transfer_mode"
        private const val KEY_FILENAME_PATTERN = "filename_pattern"
        private const val KEY_AUTO_START = "auto_start"
        private const val KEY_DELETE_OLD_FILES = "delete_old_files"
        private const val KEY_MIN_FREE_SPACE = "min_free_space"
        private const val KEY_HOURLY_SPLIT = "hourly_split"
        
        fun load(context: Context): AppSettings {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return AppSettings(
                nasAddress = prefs.getString(KEY_NAS_ADDRESS, "") ?: "",
                nasPort = prefs.getInt(KEY_NAS_PORT, 445),
                nasUsername = prefs.getString(KEY_NAS_USERNAME, "") ?: "",
                nasPassword = prefs.getString(KEY_NAS_PASSWORD, "") ?: "",
                nasShare = prefs.getString(KEY_NAS_SHARE, "") ?: "",
                transferMode = TransferMode.values()[prefs.getInt(KEY_TRANSFER_MODE, 0)],
                filenamePattern = prefs.getString(KEY_FILENAME_PATTERN, "CAM01_날짜_시간") ?: "CAM01_날짜_시간",
                autoStart = prefs.getBoolean(KEY_AUTO_START, true),
                deleteOldFiles = prefs.getBoolean(KEY_DELETE_OLD_FILES, true),
                minFreeSpaceMB = prefs.getInt(KEY_MIN_FREE_SPACE, 1000),
                hourlySplit = prefs.getBoolean(KEY_HOURLY_SPLIT, true)
            )
        }
        
        fun save(context: Context, settings: AppSettings) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit {
                putString(KEY_NAS_ADDRESS, settings.nasAddress)
                putInt(KEY_NAS_PORT, settings.nasPort)
                putString(KEY_NAS_USERNAME, settings.nasUsername)
                putString(KEY_NAS_PASSWORD, settings.nasPassword)
                putString(KEY_NAS_SHARE, settings.nasShare)
                putInt(KEY_TRANSFER_MODE, settings.transferMode.ordinal)
                putString(KEY_FILENAME_PATTERN, settings.filenamePattern)
                putBoolean(KEY_AUTO_START, settings.autoStart)
                putBoolean(KEY_DELETE_OLD_FILES, settings.deleteOldFiles)
                putInt(KEY_MIN_FREE_SPACE, settings.minFreeSpaceMB)
                putBoolean(KEY_HOURLY_SPLIT, settings.hourlySplit)
            }
        }
    }
} 