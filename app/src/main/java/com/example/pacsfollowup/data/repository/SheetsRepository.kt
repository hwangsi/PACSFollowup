package com.example.pacsfollowup.data.repository

import android.content.Context
import android.util.Log
import com.example.pacsfollowup.data.model.PatientRecord
import com.example.pacsfollowup.data.security.EncryptedPrefs
import com.example.pacsfollowup.data.security.PatientIdCipher
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SheetsRepository(private val context: Context) {

    companion object {
        private const val TAG = "PACS_Sheets"
        private const val PREFS_NAME = "pacs_prefs_secure"
        private const val KEY_SPREADSHEET_ID = "spreadsheet_id"
        private const val APP_NAME = "PACSFollowup"
    }

    private val prefs = EncryptedPrefs.create(context, PREFS_NAME)

    var spreadsheetId: String
        get() = prefs.getString(KEY_SPREADSHEET_ID, "") ?: ""
        set(value) { prefs.edit().putString(KEY_SPREADSHEET_ID, value).apply() }

    private fun buildService(): Sheets {
        val transport = NetHttpTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()
        val credential = GoogleCredential.fromStream(
            context.assets.open("service_account.json"),
            transport,
            jsonFactory
        ).createScoped(listOf(SheetsScopes.SPREADSHEETS))

        return Sheets.Builder(transport, jsonFactory, credential)
            .setApplicationName(APP_NAME)
            .build()
    }

    /** 스프레드시트의 첫 번째 시트 이름을 가져옴 */
    private fun getFirstSheetName(service: Sheets): String {
        return try {
            val spreadsheet = service.spreadsheets().get(spreadsheetId).execute()
            val name = spreadsheet.sheets?.firstOrNull()?.properties?.title ?: "Sheet1"
            Log.d(TAG, "첫 번째 시트 이름: $name")
            name
        } catch (e: Exception) {
            Log.w(TAG, "시트 이름 조회 실패, 기본값 사용: ${e.message}")
            "Sheet1"
        }
    }

    suspend fun appendRecord(record: PatientRecord): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "저장 시작 - spreadsheetId: $spreadsheetId")
            val service = buildService()

            // 시트 이름 자동 감지 (시트1 / Sheet1 등 대응)
            val sheetName = getFirstSheetName(service)

            val savedAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val row: List<Any> = listOf(
                record.date,
                PatientIdCipher.encrypt(record.patientId),
                record.examName,
                record.findings,
                savedAt
            )
            val body = ValueRange().setValues(listOf(row))
            service.spreadsheets().values()
                .append(spreadsheetId, "$sheetName!A:E", body)
                .setValueInputOption("USER_ENTERED")
                .execute()

            Log.d(TAG, "저장 성공 - 시트: $sheetName")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "저장 실패: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }
}
