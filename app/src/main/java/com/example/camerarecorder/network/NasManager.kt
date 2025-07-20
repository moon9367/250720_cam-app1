package com.example.camerarecorder.network

import com.example.camerarecorder.data.AppSettings
import com.example.camerarecorder.util.Logger
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NasManager {
    private var client: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null
    
    suspend fun connect(settings: AppSettings): Boolean = withContext(Dispatchers.IO) {
        try {
            Logger.i("NAS 연결 시도: ${settings.nasAddress}:${settings.nasPort}")
            
            client = SMBClient()
            connection = client?.connect(settings.nasAddress, settings.nasPort)
            
            if (connection == null) {
                Logger.e("NAS 연결 실패: 연결을 생성할 수 없습니다")
                return@withContext false
            }
            
            val authContext = AuthenticationContext(settings.nasUsername, settings.nasPassword.toCharArray(), null)
            session = connection?.authenticate(authContext)
            
            if (session == null) {
                Logger.e("NAS 인증 실패: 사용자명 또는 비밀번호가 잘못되었습니다")
                return@withContext false
            }
            
            share = session?.connectShare(settings.nasShare) as? DiskShare
            
            if (share == null) {
                Logger.e("NAS 공유 폴더 연결 실패: ${settings.nasShare}")
                return@withContext false
            }
            
            Logger.i("NAS 연결 성공")
            return@withContext true
            
        } catch (e: Exception) {
            Logger.e("NAS 연결 중 오류 발생", e)
            disconnect()
            return@withContext false
        }
    }
    
    suspend fun uploadFile(localFile: File, remotePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (share == null) {
                Logger.e("NAS가 연결되지 않았습니다")
                return@withContext false
            }
            
            if (!localFile.exists()) {
                Logger.e("로컬 파일이 존재하지 않습니다: ${localFile.absolutePath}")
                return@withContext false
            }
            
            Logger.i("파일 업로드 시작: ${localFile.name} -> $remotePath")
            
            // 원격 디렉토리 생성
            createRemoteDirectory(remotePath.substringBeforeLast("/"))
            
            // 파일 업로드
            val remoteFile = share?.openFile(
                remotePath,
                setOf(AccessMask.GENERIC_WRITE),
                setOf(),
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OVERWRITE_IF,
                setOf()
            )
            
            if (remoteFile == null) {
                Logger.e("원격 파일을 생성할 수 없습니다: $remotePath")
                return@withContext false
            }
            
            remoteFile.use { file ->
                val outputStream = file.outputStream
                val inputStream = localFile.inputStream()
                
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
            }
            
            Logger.i("파일 업로드 완료: ${localFile.name}")
            return@withContext true
            
        } catch (e: Exception) {
            Logger.e("파일 업로드 중 오류 발생: ${localFile.name}", e)
            return@withContext false
        }
    }
    
    private fun createRemoteDirectory(path: String) {
        try {
            if (path.isEmpty() || path == "/") return
            
            val pathParts = path.split("/").filter { it.isNotEmpty() }
            var currentPath = ""
            
            for (part in pathParts) {
                currentPath += "/$part"
                try {
                    share?.mkdir(currentPath)
                } catch (e: Exception) {
                    // 디렉토리가 이미 존재하는 경우 무시
                    Logger.d("디렉토리가 이미 존재합니다: $currentPath")
                }
            }
        } catch (e: Exception) {
            Logger.e("원격 디렉토리 생성 중 오류 발생: $path", e)
        }
    }
    
    suspend fun testConnection(settings: AppSettings): Boolean = withContext(Dispatchers.IO) {
        try {
            val connected = connect(settings)
            if (connected) {
                disconnect()
            }
            return@withContext connected
        } catch (e: Exception) {
            Logger.e("NAS 연결 테스트 중 오류 발생", e)
            return@withContext false
        }
    }
    
    fun isConnected(): Boolean {
        return share != null && share?.isConnected == true
    }
    
    fun disconnect() {
        try {
            share?.close()
            session?.close()
            connection?.close()
            client?.close()
            
            share = null
            session = null
            connection = null
            client = null
            
            Logger.i("NAS 연결 해제")
        } catch (e: Exception) {
            Logger.e("NAS 연결 해제 중 오류 발생", e)
        }
    }
    
    fun getConnectionStatus(): String {
        return if (isConnected()) "연결됨" else "연결 안됨"
    }
} 