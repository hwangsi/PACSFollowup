# PACS Follow-up App

> ⚠️ **PHI Warning / Test environment only**
> This app is a **development/validation demo**. Do NOT input, record, or transmit actual patient identifiers (PHI: name, SSN, patient ID, date of birth, etc.).
> Before clinical use, a review by your institution's IRB, privacy, and security department and a de-identification procedure are mandatory.
> No code in this repository has received medical device approval.

An Android app that captures PACS screens, automatically extracts patient information via OCR, records dictated findings via voice, and saves everything to Google Sheets.

---

## Features

- 📷 **Camera Capture** — Photograph PACS screens directly
- 🔍 **OCR Auto-extraction** — Patient ID, date, and exam name extracted automatically (Google ML Kit)
- 🎤 **Voice Dictation** — Record findings in English; auto-converted to text (Google Cloud Speech-to-Text, `medical_dictation` model)
- 📊 **Google Sheets Integration** — Findings saved automatically via Service Account
- 🏥 **Radiology/Urology focused** — Prostate, kidney, bladder and other exam names recognized
- 🔒 **Patient ID masking** — IDs masked in the record list; tap to reveal for 3 seconds
- 🔎 **Search** — Real-time filtering by Patient ID, exam, date, or findings
- 🗑️ **Delete** — Swipe left or tap the trash icon to delete a record (with confirmation)

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| OCR | Google ML Kit Text Recognition v2 (Korean + Latin) |
| STT | Google Cloud Speech-to-Text API (`medical_dictation`, `en-US`) |
| Storage | Google Sheets API v4 (Service Account) |
| Camera | CameraX |
| Local Storage | EncryptedSharedPreferences (AES-256-GCM) |
| Encryption | Android Keystore AES-256-GCM (Patient ID) |

---

## Google Sheets Data Structure

| A: Date | B: Patient ID (encrypted) | C: Exam | D: Findings | E: Saved At |
|---------|--------------------------|---------|-------------|-------------|

---

## Setup

### 1. Google Cloud Console
- Enable **Cloud Speech-to-Text API**
- Enable **Google Sheets API**
- Create a **Service Account** and download the JSON key

### 2. File Configuration
```
app/src/main/assets/service_account.json   ← Service Account key (never commit)
gradle.properties                          ← Add SPEECH_API_KEY (never commit)
```

`gradle.properties` format:
```properties
SPEECH_API_KEY=your_api_key_here
```

### 3. Google Sheets
1. Create a new spreadsheet
2. Share it with the service account email (found in `client_email` field of the JSON) as **Editor**
3. In the app, tap ⚙️ and paste the full Sheets URL or just the Spreadsheet ID

> **Tip**: You can paste the full URL (`https://docs.google.com/spreadsheets/d/.../edit`) — the app extracts the ID automatically.

---

## Security

| Item | Measure |
|------|---------|
| `service_account.json` | Never commit — listed in `.gitignore` |
| `gradle.properties` | Never commit — listed in `.gitignore` |
| Local records cache (20 records, Spreadsheet ID) | **EncryptedSharedPreferences** (AES-256-GCM) |
| Patient ID column in Sheets | **Android Keystore AES-256-GCM** — key is hardware-bound to the device |
| ADB/Auto Backup | Blocked via `android:allowBackup="false"` |
| External data transmission | Consent dialog shown before every save |
| Voice input warning | In-app warning not to speak patient names or IDs |

> Before production use, review IRB / DPIA / data processing agreements (BAA/DPA).

---

## Developer

- Sung-il Hwang, Department of Radiology, Seoul National University Bundang Hospital
