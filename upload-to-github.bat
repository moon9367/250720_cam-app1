@echo off
echo ========================================
echo GitHub 업로드 도구
echo ========================================
echo.

REM Git 설치 확인
git --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Git이 설치되어 있지 않습니다.
    echo https://git-scm.com/downloads 에서 다운로드하세요.
    pause
    exit /b 1
)

echo Git 발견:
git --version
echo.

REM .gitignore 파일 생성 (없는 경우)
if not exist ".gitignore" (
    echo .gitignore 파일을 생성합니다...
    echo # Built application files > .gitignore
    echo *.apk >> .gitignore
    echo *.aab >> .gitignore
    echo app/release/ >> .gitignore
    echo app/debug/ >> .gitignore
    echo .gradle/ >> .gitignore
    echo build/ >> .gitignore
    echo local.properties >> .gitignore
    echo .idea/ >> .gitignore
    echo *.iml >> .gitignore
    echo .DS_Store >> .gitignore
    echo Thumbs.db >> .gitignore
)

echo 다음 단계를 따라주세요:
echo.
echo 1. GitHub.com에서 새 저장소 생성
echo 2. 저장소 이름 입력 (예: camerarecorder-app)
echo 3. Public 또는 Private 선택
echo 4. "Create repository" 클릭
echo.
echo 저장소 URL을 입력하세요 (예: https://github.com/username/camerarecorder-app.git):
set /p REPO_URL="저장소 URL: "

echo.
echo Git 초기화 및 업로드를 시작합니다...

REM Git 초기화
git init
git add .
git commit -m "Initial commit: Camera Recorder App"

REM 원격 저장소 추가
git branch -M main
git remote add origin %REPO_URL%
git push -u origin main

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo GitHub 업로드 성공!
    echo ========================================
    echo.
    echo 저장소 URL: %REPO_URL%
    echo.
    echo 이제 GitHub Actions에서 APK가 자동으로 빌드됩니다.
    echo Actions 탭에서 빌드 상태를 확인할 수 있습니다.
) else (
    echo.
    echo ========================================
    echo GitHub 업로드 실패!
    echo ========================================
    echo.
    echo 다음을 확인해주세요:
    echo 1. 저장소 URL이 올바른지 확인
    echo 2. GitHub 계정 인증이 완료되었는지 확인
    echo 3. 인터넷 연결 상태 확인
)

echo.
pause 