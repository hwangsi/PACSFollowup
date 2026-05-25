package com.example.pacsfollowup.data.repository

import android.content.Context
import com.example.pacsfollowup.data.model.PatientRecord
import com.example.pacsfollowup.data.security.EncryptedPrefs
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

    suspend fun appendRecord(record: PatientRecord): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = buildService()
            val savedAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val row: List<Any> = listOf(
                record.date,
                record.patientId,
                record.examName,
                record.findings,
                savedAt
            )
            val body = ValueRange().setValues(listOf(row))
            service.spreadsheets().values()
                .append(spreadsheetId, "시트1!A:E", body)
                .setValueInputOption("USER_ENTERED")
                .execute()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
