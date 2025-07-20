@echo off
echo ========================================
echo APK 빌드 도구
echo ========================================
echo.

REM 현재 디렉토리 확인
echo 현재 작업 디렉토리: %CD%
echo.

REM Android Studio의 다양한 Java 경로 시도
set JAVA_FOUND=0

echo Java 경로를 찾는 중...

REM 경로 1: Android Studio 기본 설치 경로
if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
    set PATH=%JAVA_HOME%\bin;%PATH%
    echo [1] Android Studio 기본 경로에서 Java 발견: %JAVA_HOME%
    set JAVA_FOUND=1
    goto :check_java
)

REM 경로 2: 사용자별 Android Studio 경로
if exist "%LOCALAPPDATA%\Android\Sdk\jbr\bin\java.exe" (
    set JAVA_HOME=%LOCALAPPDATA%\Android\Sdk\jbr
    set PATH=%JAVA_HOME%\bin;%PATH%
    echo [2] 사용자별 Android Studio 경로에서 Java 발견: %JAVA_HOME%
    set JAVA_FOUND=1
    goto :check_java
)

REM 경로 3: Program Files (x86) 경로
if exist "C:\Program Files (x86)\Android\Android Studio\jbr\bin\java.exe" (
    set JAVA_HOME=C:\Program Files (x86)\Android\Android Studio\jbr
    set PATH=%JAVA_HOME%\bin;%PATH%
    echo [3] Program Files (x86) 경로에서 Java 발견: %JAVA_HOME%
    set JAVA_FOUND=1
    goto :check_java
)

REM 경로 4: 시스템 PATH에서 Java 찾기
java -version >nul 2>&1
if %errorlevel% equ 0 (
    echo [4] 시스템 PATH에서 Java 발견
    set JAVA_FOUND=1
    goto :check_java
)

:java_not_found
echo.
echo ========================================
echo 오류: Java를 찾을 수 없습니다!
echo ========================================
echo.
echo 다음 중 하나를 확인해주세요:
echo 1. Android Studio가 설치되어 있는지 확인
echo 2. Java가 시스템에 설치되어 있는지 확인
echo 3. JAVA_HOME 환경변수가 설정되어 있는지 확인
echo.
echo Android Studio를 통해 직접 빌드하는 것을 권장합니다:
echo Build → Build Bundle(s) / APK(s) → Build APK(s)
echo.
pause
exit /b 1

:check_java
echo.
echo Java 버전 확인 중...
java -version
if %errorlevel% neq 0 (
    echo Java 실행 중 오류가 발생했습니다.
    goto :java_not_found
)

echo.
echo ========================================
echo Java 환경 설정 완료!
echo ========================================
echo.

REM Gradle 래퍼 확인
if not exist "gradlew.bat" (
    echo 오류: gradlew.bat 파일을 찾을 수 없습니다.
    echo 현재 디렉토리가 Android 프로젝트 루트인지 확인해주세요.
    pause
    exit /b 1
)

echo Gradle 래퍼 발견: gradlew.bat
echo.

echo ========================================
echo APK 빌드를 시작합니다...
echo ========================================
echo.

REM APK 빌드 실행
gradlew assembleRelease

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo APK 빌드 성공!
    echo ========================================
    echo.
    echo APK 파일 위치: app\build\outputs\apk\release\app-release.apk
    echo.
    if exist "app\build\outputs\apk\release\app-release.apk" (
        echo 파일 크기 확인:
        dir "app\build\outputs\apk\release\app-release.apk"
    )
) else (
    echo.
    echo ========================================
    echo APK 빌드 실패!
    echo ========================================
    echo.
    echo 오류 코드: %errorlevel%
    echo 자세한 오류 내용을 확인하려면 위의 빌드 로그를 참조하세요.
)

echo.
echo ========================================
echo 작업 완료
echo ========================================
echo.
pause 