# 카메라 녹화기 (Camera Recorder)

갤럭시 스마트폰을 위한 자동 카메라 녹화 및 NAS 전송 앱입니다.

## 주요 기능

### 📹 카메라 녹화
- **자동 녹화**: 앱 실행 시 후면 카메라로 자동 녹화 시작
- **1시간 단위 분할**: 영상을 1시간 단위로 자동 분할 저장
- **최고 해상도**: 카메라가 지원하는 최대 해상도로 녹화
- **포그라운드 서비스**: 백그라운드에서도 녹화 계속 진행

### 💾 NAS 전송
- **SMB 프로토콜**: NAS 공유 폴더에 자동 전송
- **실시간/주기적 전송**: 사용자 설정에 따른 전송 방식 선택
- **연결 복구**: NAS 연결이 복구되면 자동으로 전송 재개
- **전송 상태 관리**: 전송 성공/실패 로그 및 재시도 기능

### 🔧 자동화 기능
- **자동 시작**: 부팅 완료 시 자동으로 녹화 시작
- **저장 공간 관리**: 저장 공간 부족 시 오래된 파일 자동 삭제
- **백그라운드 유지**: WakeLock을 통한 안정적인 백그라운드 동작

### 📱 사용자 인터페이스
- **간단한 메인 화면**: 녹화 상태, 저장소 정보, NAS 상태 표시
- **설정 화면**: NAS 연결 정보, 녹화 옵션, 전송 방식 설정
- **로그 확인**: 실시간 로그 확인 및 내보내기 기능

## 설치 및 사용법

### 1. 권한 설정
앱 실행 시 다음 권한을 허용해야 합니다:
- 카메라 권한
- 오디오 녹화 권한
- 저장소 읽기/쓰기 권한

### 2. NAS 설정
1. 설정 화면에서 NAS 정보 입력:
   - NAS 주소 (IP 주소)
   - 포트 (기본: 445)
   - 사용자명
   - 비밀번호
   - 공유 폴더명
2. "연결 테스트" 버튼으로 연결 확인

### 3. 녹화 시작
- 메인 화면에서 "녹화 시작" 버튼 클릭
- 앱이 백그라운드로 이동해도 녹화 계속 진행
- 알림을 통해 녹화 상태 확인 가능

### 4. 자동 시작 설정
- 설정에서 "자동 시작" 옵션 활성화
- 삼성 루틴 앱을 통해 부팅 시 자동 실행 설정

## 기술 스펙

### 개발 환경
- **언어**: Kotlin
- **최소 SDK**: API 24 (Android 7.0)
- **타겟 SDK**: API 34 (Android 14)
- **카메라 API**: CameraX
- **네트워크**: SMBJ 라이브러리

### 주요 라이브러리
- `androidx.camera:camera-video`: 비디오 녹화
- `com.hierynomus:smbj`: SMB 프로토콜 지원
- `androidx.work:work-runtime-ktx`: 백그라운드 작업
- `kotlinx-coroutines`: 비동기 처리

### 파일 구조
```
app/src/main/java/com/example/camerarecorder/
├── data/
│   ├── AppSettings.kt          # 설정 관리
│   └── RecordingFile.kt        # 녹화 파일 정보
├── network/
│   └── NasManager.kt           # NAS 연결 관리
├── service/
│   ├── CameraRecordingService.kt  # 카메라 녹화 서비스
│   └── NasTransferService.kt      # NAS 전송 서비스
├── receiver/
│   └── BootReceiver.kt         # 부팅 완료 수신자
├── util/
│   ├── Logger.kt               # 로그 관리
│   └── FileManager.kt          # 파일 관리
├── MainActivity.kt             # 메인 액티비티
└── SettingsActivity.kt         # 설정 액티비티
```

## 빌드 및 배포

### 1. 프로젝트 빌드
```bash
./gradlew assembleRelease
```

### 2. APK 생성
빌드된 APK는 `app/build/outputs/apk/release/` 디렉토리에 생성됩니다.

### 3. 설치
```bash
adb install app-release.apk
```

## 주의사항

### 배터리 최적화
- 앱이 배터리 최적화에서 제외되도록 설정
- 설정 > 앱 > 카메라 녹화기 > 배터리 > 배터리 최적화 해제

### 저장소 관리
- 녹화 파일은 외부 저장소의 `CameraRecorder/recordings/` 폴더에 저장
- 로그 파일은 `CameraRecorder/logs/` 폴더에 저장
- 정기적으로 저장소 상태 확인 필요

### 네트워크 설정
- NAS와 같은 네트워크에 연결되어 있어야 함
- 방화벽에서 SMB 포트(445) 허용 필요
- Wi-Fi 절전 모드 비활성화 권장

## 문제 해결

### 녹화가 시작되지 않는 경우
1. 카메라 권한 확인
2. 저장소 권한 확인
3. 다른 앱이 카메라를 사용 중인지 확인

### NAS 전송이 실패하는 경우
1. NAS 주소 및 포트 확인
2. 사용자명/비밀번호 확인
3. 공유 폴더 접근 권한 확인
4. 네트워크 연결 상태 확인

### 앱이 자동으로 시작되지 않는 경우
1. 자동 시작 설정 확인
2. 배터리 최적화 설정 확인
3. 삼성 루틴 앱 설정 확인

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 지원

문제가 발생하거나 개선 사항이 있으시면 이슈를 등록해 주세요. 