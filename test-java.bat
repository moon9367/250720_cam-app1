@echo off
setlocal enabledelayedexpansion
echo Java 환경 테스트
echo ================
echo.

echo 1. 현재 디렉토리: %CD%
echo.

echo 2. 시스템 PATH에서 Java 찾기...
java -version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ 시스템 PATH에서 Java 발견
    java -version
    goto :end
)

echo ✗ 시스템 PATH에서 Java를 찾을 수 없음
echo.

echo 3. Android Studio 경로에서 Java 찾기...

REM Android Studio 경로들 확인
if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    echo ✓ 발견: C:\Program Files\Android\Android Studio\jbr\bin\java.exe
    set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
    set PATH=%JAVA_HOME%\bin;%PATH%
    echo JAVA_HOME 설정: %JAVA_HOME%
    goto :test_java
)

if exist "%LOCALAPPDATA%\Android\Sdk\jbr\bin\java.exe" (
    echo ✓ 발견: %LOCALAPPDATA%\Android\Sdk\jbr\bin\java.exe
    set JAVA_HOME=%LOCALAPPDATA%\Android\Sdk\jbr
    set PATH=%JAVA_HOME%\bin;%PATH%
    echo JAVA_HOME 설정: %JAVA_HOME%
    goto :test_java
)

if exist "C:\Program Files (x86)\Android\Android Studio\jbr\bin\java.exe" (
    echo ✓ 발견: C:\Program Files (x86)\Android\Android Studio\jbr\bin\java.exe
    set JAVA_HOME=C:\Program Files (x86)\Android\Android Studio\jbr
    set PATH=%JAVA_HOME%\bin;%PATH%
    echo JAVA_HOME 설정: %JAVA_HOME%
    goto :test_java
)

echo ✗ Android Studio 경로에서도 Java를 찾을 수 없음
goto :end

:test_java
echo.
echo 4. 설정된 Java 테스트...
java -version
if %errorlevel% equ 0 (
    echo ✓ Java 설정 성공!
) else (
    echo ✗ Java 설정 실패
)

:end
echo.
echo 테스트 완료
pause 