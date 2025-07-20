@echo off
echo ========================================
echo Docker를 통한 APK 빌드
echo ========================================
echo.

REM Docker 설치 확인
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Docker가 설치되어 있지 않습니다.
    echo https://www.docker.com/products/docker-desktop 에서 다운로드하세요.
    pause
    exit /b 1
)

echo Docker 발견: 
docker --version
echo.

echo APK 빌드를 시작합니다...
echo.

REM Android 빌드용 Docker 이미지 사용
docker run --rm -v "%CD%:/app" -w /app openjdk:17-jdk-slim bash -c "
    apt-get update && apt-get install -y gradle
    chmod +x gradlew
    ./gradlew assembleRelease
"

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo APK 빌드 성공!
    echo ========================================
    echo.
    echo APK 파일 위치: app\build\outputs\apk\release\app-release.apk
) else (
    echo.
    echo ========================================
    echo APK 빌드 실패!
    echo ========================================
)

echo.
pause 