# PACS Followup App

> ⚠️ **PHI 경고 / Test environment only**
> 본 앱은 **개발·검증용 데모**입니다. 실제 환자 식별정보(PHI; 이름, 주민번호, 환자번호, 생년월일 등)를
> 입력·녹음·전송하지 마십시오. 실 임상 사용 전에는 소속 기관의 IRB·정보보호·보안 부서의 검토와
> 비식별화(De-identification) 절차가 반드시 선행되어야 합니다.
> 본 저장소의 어떤 코드도 의료기기 인허가를 받지 않았습니다.

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
- **Local persistence**: EncryptedSharedPreferences (AES-256 GCM)

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

## 보안 / 개인정보 처리

- `service_account.json` 은 절대 GitHub에 업로드 금지
- `gradle.properties` 는 `.gitignore` 에 포함됨
- API 키는 로컬에서만 관리
- 로컬 캐시(환자 ID·소견 최근 20건, Spreadsheet ID)는 **EncryptedSharedPreferences(AES-256)** 로 저장
- `android:allowBackup="false"` 로 ADB/Auto Backup 경로 차단
- **외부 전송 데이터** (Google STT, Google Sheets)는 사용자의 동의 다이얼로그 이후 전송됨
- 음성 입력 시 환자명·환자번호 등 직접 식별자를 발화하지 마세요 (앱 내 경고 표시)
- 실 환경 적용 전에 IRB / DPIA / 위탁계약(BAA/DPA) 검토 필수

## 개발자

- 황성일 (분당서울대학교병원 영상의학과)
