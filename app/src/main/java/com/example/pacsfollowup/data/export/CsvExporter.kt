package com.example.pacsfollowup.data.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.pacsfollowup.data.model.PatientRecord
import com.example.pacsfollowup.data.security.ExportCipher
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Exports [PatientRecord] list to a password-protected CSV file.
 *
 * CSV format:
 * ```
 * # PACS_EXPORT_V1
 * # SALT:<32-char hex>
 * # ALGO:AES-256-GCM/PBKDF2WithHmacSHA256/100000
 * Date,PatientID_Encrypted,Exam,Findings,SavedAt
 * 2024-01-15,<base64(iv+ct)>,Prostate MRI,"findings text",2024-01-15 10:30:00
 * ```
 *
 * Patient ID decryption:
 *   1. Parse SALT from the `# SALT:` header line.
 *   2. key = PBKDF2(password, SALT, 100000, 256-bit)
 *   3. data = Base64.decode(field)  →  iv[0..11] + ciphertext[12..]
 *   4. AES-256-GCM decrypt(key, iv, ciphertext)
 *
 * File is saved to public Downloads folder (accessible via USB).
 */
class CsvExporter(private val context: Context) {

    data class ExportResult(val uri: Uri, val displayName: String)

    /**
     * Exports all [records] with Patient IDs encrypted using [password].
     * Returns the URI of the created file.
     */
    fun export(records: List<PatientRecord>, password: String): Result<ExportResult> =
        runCatching {
            val salt = ExportCipher.generateSalt()
            val key  = ExportCipher.deriveKey(password, salt)
            val csv  = buildCsv(records, salt, key)

            val timestamp   = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val displayName = "pacs_export_$timestamp.csv"

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(csv, displayName)
            } else {
                saveViaLegacyFile(csv, displayName)
            }
            ExportResult(uri, displayName)
        }

    // ── CSV builder ────────────────────────────────────────────────────────────

    private fun buildCsv(
        records: List<PatientRecord>,
        salt: ByteArray,
        key: javax.crypto.spec.SecretKeySpec
    ): String = buildString {
        appendLine("# PACS_EXPORT_V1")
        appendLine("# SALT:${ExportCipher.saltToHex(salt)}")
        appendLine("# ALGO:AES-256-GCM/PBKDF2WithHmacSHA256/100000")
        appendLine("Date,PatientID_Encrypted,Exam,Findings,SavedAt")
        records.forEach { r ->
            val encId = ExportCipher.encryptWithKey(r.patientId, key)
            appendLine(
                listOf(r.date, encId, r.examName, r.findings, r.savedAt)
                    .joinToString(",") { it.csvEscape() }
            )
        }
    }

    private fun String.csvEscape(): String =
        if (contains(',') || contains('"') || contains('\n') || contains('\r'))
            "\"${replace("\"", "\"\"")}\""
        else this

    // ── File I/O ───────────────────────────────────────────────────────────────

    /** Android 10+ path: MediaStore Downloads (no extra permission needed). */
    private fun saveViaMediaStore(csv: String, displayName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri      = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore insert returned null — external storage unavailable")

        resolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    /** Android 9 and below: direct file access in public Downloads. */
    @Suppress("DEPRECATION")
    private fun saveViaLegacyFile(csv: String, displayName: String): Uri {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        dir.mkdirs()
        val file = File(dir, displayName)
        file.writeText(csv, Charsets.UTF_8)
        return Uri.fromFile(file)
    }
}
