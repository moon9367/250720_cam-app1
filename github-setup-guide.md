# GitHub Actions를 통한 APK 빌드 가이드

## 1. GitHub 계정 설정

### 1.1 GitHub 계정 생성
1. [GitHub.com](https://github.com) 방문
2. "Sign up" 클릭
3. 이메일, 비밀번호, 사용자명 입력
4. 이메일 인증 완료

### 1.2 Git 설치
1. [Git for Windows](https://git-scm.com/download/win) 다운로드
2. 설치 시 기본 설정 유지
3. 설치 완료 후 터미널에서 `git --version` 확인

## 2. GitHub 저장소 생성

### 2.1 새 저장소 만들기
1. GitHub.com 로그인
2. 오른쪽 상단 "+" 버튼 → "New repository" 클릭
3. 저장소 정보 입력:
   - **Repository name**: `camerarecorder-app` (또는 원하는 이름)
   - **Description**: `Camera Recorder Android App`
   - **Visibility**: Public 또는 Private 선택
   - **Initialize this repository with**: 체크하지 않음
4. "Create repository" 클릭

### 2.2 저장소 URL 복사
- 생성된 저장소 페이지에서 녹색 "Code" 버튼 클릭
- HTTPS 탭에서 URL 복사 (예: `https://github.com/username/camerarecorder-app.git`)

## 3. 프로젝트 업로드

### 3.1 자동 업로드 (권장)
1. 프로젝트 폴더에서 `upload-to-github.bat` 실행
2. 복사한 저장소 URL 입력
3. 자동으로 업로드 완료

### 3.2 수동 업로드
```bash
# 프로젝트 폴더에서 실행
git init
git add .
git commit -m "Initial commit: Camera Recorder App"
git branch -M main
git remote add origin https://github.com/username/camerarecorder-app.git
git push -u origin main
```

## 4. GitHub Actions 확인

### 4.1 Actions 탭 확인
1. GitHub 저장소 페이지에서 "Actions" 탭 클릭
2. "Build Android APK" 워크플로우가 자동으로 실행됨
3. 빌드 상태 확인 (노란색: 진행 중, 초록색: 성공, 빨간색: 실패)

### 4.2 빌드 로그 확인
- Actions 탭에서 워크플로우 클릭
- "build" 작업 클릭
- 실시간 빌드 로그 확인

## 5. APK 다운로드

### 5.1 Artifacts에서 다운로드
1. Actions 탭에서 완료된 워크플로우 클릭
2. "app-release" 아티팩트 클릭
3. "app-release.apk" 다운로드

### 5.2 Releases에서 다운로드 (자동)
1. 저장소 페이지에서 "Releases" 클릭
2. 최신 릴리즈에서 APK 다운로드
3. 릴리즈 노트 확인

## 6. 자동 빌드 트리거

### 6.1 코드 변경 시 자동 빌드
- main/master 브랜치에 코드 푸시 시 자동 빌드
- Pull Request 생성 시 자동 빌드

### 6.2 수동 빌드
1. Actions 탭에서 "Build Android APK" 워크플로우 클릭
2. "Run workflow" 버튼 클릭
3. 브랜치 선택 후 "Run workflow" 클릭

## 7. 문제 해결

### 7.1 빌드 실패 시
1. Actions 탭에서 실패한 워크플로우 클릭
2. 빌드 로그 확인
3. 일반적인 문제들:
   - Gradle 의존성 문제
   - 코드 컴파일 오류
   - 권한 문제

### 7.2 권한 문제
- 저장소 설정 → Actions → General
- "Actions permissions" 확인
- "Allow all actions and reusable workflows" 선택

## 8. 고급 설정

### 8.1 환경 변수 설정
- 저장소 설정 → Secrets and variables → Actions
- 필요한 API 키나 설정값 추가

### 8.2 브랜치별 빌드
- `develop` 브랜치: 디버그 APK
- `main` 브랜치: 릴리즈 APK

## 9. 장점

### 9.1 무료
- GitHub Actions: 월 2,000분 무료
- 일반적인 안드로이드 앱 빌드에 충분

### 9.2 자동화
- 코드 변경 시 자동 빌드
- 릴리즈 자동 생성
- 버전 관리 자동화

### 9.3 접근성
- 웹에서 언제든지 APK 다운로드
- 팀원들과 쉽게 공유
- 빌드 히스토리 관리

## 10. 주의사항

### 10.1 보안
- 민감한 정보는 GitHub Secrets 사용
- API 키나 비밀번호는 코드에 직접 포함하지 않음

### 10.2 용량
- APK 파일 크기 제한 확인
- 불필요한 파일은 .gitignore에 추가

### 10.3 빌드 시간
- 첫 빌드: 5-10분
- 이후 빌드: 2-5분 (캐시 활용) 