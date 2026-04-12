# PACS Followup App

PACS 화면을 촬영하여 추적 관찰이 필요한 환자 정보를 자동으로 추출하고 Google Sheets에 저장하는 Android 앱입니다.

## 주요 기능

- 📷 **카메라 촬영**: PACS 화면을 직접 촬영
- 🔍 **OCR 자동 인식**: 환자 ID, 날짜, 영상검사명 자동 추출 (Google ML Kit)
- 🎤 **음성 소견 입력**: 음성 녹음 후 자동 텍스트 변환 (Google Cloud Speech-to-Text)
- 📊 **Google Sheets 연동**: 판독 소견 자동 저장
- 🏥 **비뇨생식계 특화**: 전립선, 신장, 방광 등 검사명 자동 인식

## 기술 스택

- **언어**: Kotlin
- **UI**: Jetpack Compose
- **OCR**: Google ML Kit Text Recognition v2
- **STT**: Google Cloud Speech-to-Text API (medical_dictation 모델)
- **Storage**: Google Sheets API v4 (Service Account)
- **Camera**: CameraX

## 데이터 구조 (Google Sheets)

| A: 날짜 | B: 환자ID | C: 영상검사명 | D: 소견 | E: 입력시각 |
|--------|---------|------------|--------|-----------|

## 설정 방법

### 1. Google Cloud Console
- Cloud Speech-to-Text API 활성화
- Google Sheets API 활성화
- Service Account 생성 후 JSON 키 다운로드

### 2. 파일 설정
app/src/main/assets/service_account.json  ← 서비스 계정 키
gradle.properties 에 SPEECH_API_KEY 추가

### 3. Google Sheets
- 새 스프레드시트 생성
- 서비스 계정 이메일을 편집자로 공유
- 앱 설정(⚙️)에서 Spreadsheet ID 입력

## 보안 주의사항

- `service_account.json` 은 절대 GitHub에 업로드 금지
- `gradle.properties` 는 `.gitignore` 에 포함됨
- API 키는 로컬에서만 관리

## 개발자

- 황성일 (분당서울대학교병원 영상의학과)

2. git add README.md
3. git commit -m "Docs: README 작성"
4. git push origin main
