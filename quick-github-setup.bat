@echo off
echo ========================================
echo GitHub Actions 빠른 설정
echo ========================================
echo.

REM Git 설치 확인
git --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Git이 설치되어 있지 않습니다.
    echo.
    echo 1. https://git-scm.com/downloads 에서 Git 다운로드
    echo 2. 설치 완료 후 이 스크립트를 다시 실행
    echo.
    pause
    exit /b 1
)

echo ✓ Git 설치 확인됨
echo.

REM .gitignore 파일 생성
if not exist ".gitignore" (
    echo .gitignore 파일을 생성합니다...
    (
        echo # Built application files
        echo *.apk
        echo *.aab
        echo app/release/
        echo app/debug/
        echo .gradle/
        echo build/
        echo local.properties
        echo .idea/
        echo *.iml
        echo .DS_Store
        echo Thumbs.db
    ) > .gitignore
    echo ✓ .gitignore 파일 생성됨
)

echo.
echo ========================================
echo GitHub 설정 단계
echo ========================================
echo.
echo 1. GitHub.com에서 새 저장소 생성
echo    - 저장소 이름: camerarecorder-app (또는 원하는 이름)
echo    - Public 또는 Private 선택
echo    - README 파일 생성하지 않음
echo.
echo 2. 저장소 생성 후 URL 복사
echo    - 녹색 "Code" 버튼 클릭
echo    - HTTPS 탭에서 URL 복사
echo.
echo 3. 아래에 URL을 입력하세요:
echo.
set /p REPO_URL="GitHub 저장소 URL: "

if "%REPO_URL%"=="" (
    echo URL이 입력되지 않았습니다.
    pause
    exit /b 1
)

echo.
echo ========================================
echo 프로젝트 업로드 시작
echo ========================================
echo.

REM Git 초기화
echo 1. Git 초기화...
git init
if %errorlevel% neq 0 (
    echo Git 초기화 실패
    pause
    exit /b 1
)

REM 파일 추가
echo 2. 파일 추가...
git add .
if %errorlevel% neq 0 (
    echo 파일 추가 실패
    pause
    exit /b 1
)

REM 커밋
echo 3. 커밋 생성...
git commit -m "Initial commit: Camera Recorder App"
if %errorlevel% neq 0 (
    echo 커밋 실패
    pause
    exit /b 1
)

REM 브랜치 이름 변경
echo 4. 브랜치 이름 변경...
git branch -M main
if %errorlevel% neq 0 (
    echo 브랜치 이름 변경 실패
    pause
    exit /b 1
)

REM 원격 저장소 추가
echo 5. 원격 저장소 추가...
git remote add origin %REPO_URL%
if %errorlevel% neq 0 (
    echo 원격 저장소 추가 실패
    pause
    exit /b 1
)

REM 푸시
echo 6. GitHub에 업로드...
git push -u origin main
if %errorlevel% neq 0 (
    echo.
    echo ========================================
    echo 업로드 실패!
    echo ========================================
    echo.
    echo 다음을 확인해주세요:
    echo 1. GitHub 계정 인증이 완료되었는지 확인
    echo 2. 저장소 URL이 올바른지 확인
    echo 3. 인터넷 연결 상태 확인
    echo.
    echo 수동으로 다음 명령을 실행해보세요:
    echo git push -u origin main
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo 업로드 성공!
echo ========================================
echo.
echo 저장소 URL: %REPO_URL%
echo.
echo 다음 단계:
echo 1. GitHub 저장소 페이지 방문
echo 2. "Actions" 탭 클릭
echo 3. "Build Android APK" 워크플로우 확인
echo 4. 빌드 완료 후 APK 다운로드
echo.
echo 빌드 시간: 약 5-10분 (첫 빌드)
echo.
pause 