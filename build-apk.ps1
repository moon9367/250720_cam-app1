Write-Host "APK 빌드를 시작합니다..." -ForegroundColor Green

# Android Studio의 내장 Java 경로 설정
$javaPaths = @(
    "C:\Program Files\Android\Android Studio\jbr",
    "$env:LOCALAPPDATA\Android\Sdk\jbr",
    "$env:PROGRAMFILES\Android\Android Studio\jbr"
)

$javaFound = $false
foreach ($path in $javaPaths) {
    if (Test-Path "$path\bin\java.exe") {
        $env:JAVA_HOME = $path
        $env:PATH = "$path\bin;$env:PATH"
        Write-Host "Java 경로 설정: $path" -ForegroundColor Yellow
        $javaFound = $true
        break
    }
}

if (-not $javaFound) {
    Write-Host "Java를 찾을 수 없습니다. Android Studio를 통해 빌드하거나 Java를 설치해주세요." -ForegroundColor Red
    Read-Host "계속하려면 아무 키나 누르세요"
    exit 1
}

# Java 버전 확인
try {
    $javaVersion = java -version 2>&1
    Write-Host "Java 버전 확인 완료" -ForegroundColor Green
} catch {
    Write-Host "Java 실행 중 오류가 발생했습니다." -ForegroundColor Red
    Read-Host "계속하려면 아무 키나 누르세요"
    exit 1
}

Write-Host "APK 빌드를 시작합니다..." -ForegroundColor Green

# APK 빌드 실행
try {
    .\gradlew assembleRelease
    if ($LASTEXITCODE -eq 0) {
        Write-Host "APK 빌드가 완료되었습니다!" -ForegroundColor Green
        Write-Host "APK 파일 위치: app\build\outputs\apk\release\app-release.apk" -ForegroundColor Cyan
    } else {
        Write-Host "APK 빌드 중 오류가 발생했습니다." -ForegroundColor Red
    }
} catch {
    Write-Host "빌드 중 오류가 발생했습니다: $_" -ForegroundColor Red
}

Read-Host "계속하려면 아무 키나 누르세요" 