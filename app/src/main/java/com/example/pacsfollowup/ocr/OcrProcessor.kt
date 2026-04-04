package com.example.pacsfollowup.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrResult(
    val patientId: String,
    val date: String,
    val examName: String,
    val rawText: String
)

class OcrProcessor {

    private val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    suspend fun processImage(bitmap: Bitmap): OcrResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText = suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        val rawText = visionText.text
        return OcrResult(
            patientId = extractPatientId(rawText),
            date = extractDate(rawText),
            examName = extractExamName(rawText),
            rawText = rawText
        )
    }

    private fun extractPatientId(text: String): String {
        val patterns = listOf(
            Pattern.compile("환자\\s*번호[:\\s]+([A-Za-z0-9\\-]+)"),
            Pattern.compile("환자\\s*ID[:\\s]+([A-Za-z0-9\\-]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:PID|PT)[:\\s#]+([A-Za-z0-9\\-]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ID[:\\s]+([A-Za-z0-9\\-]{4,12})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b([0-9]{7,12})\\b")
        )
        for (pattern in patterns) {
            val m = pattern.matcher(text)
            if (m.find()) return m.group(1) ?: ""
        }
        return ""
    }

    private fun extractDate(text: String): String {
        val patterns = listOf(
            "(?<y>\\d{4})[.\\-/](?<m>\\d{1,2})[.\\-/](?<d>\\d{1,2})",
            "(?<y>\\d{4})년\\s*(?<m>\\d{1,2})월\\s*(?<d>\\d{1,2})일",
            "(?<y>\\d{2})[.\\-/](?<m>\\d{2})[.\\-/](?<d>\\d{2})"
        )
        for (patternStr in patterns) {
            val m = Pattern.compile(patternStr).matcher(text)
            if (m.find()) {
                return try {
                    "${m.group("y")}-${m.group("m")!!.padStart(2, '0')}-${m.group("d")!!.padStart(2, '0')}"
                } catch (e: Exception) {
                    m.group(0) ?: ""
                }
            }
        }
        return ""
    }

    private fun extractExamName(text: String): String {
        val upper = text.uppercase()

        // Modality 감지
        val modality = when {
            upper.contains("PET-CT") || upper.contains("PET CT") -> "PET-CT"
            upper.contains("PET") -> "PET"
            upper.contains("MRI") || upper.contains("MR ") -> "MRI"
            upper.contains("CT") -> "CT"
            upper.contains("X-RAY") || upper.contains("XRAY") || upper.contains("X RAY") -> "X-ray"
            upper.contains("ULTRASOUND") || upper.contains("초음파") || upper.contains(" US ") || upper.contains("USG") -> "US"
            else -> ""
        }

        // 비뇨생식계 검사명 우선 감지
        val urogenitalExams = mapOf(
            "PROSTATE" to "Prostate",
            "전립선" to "Prostate",
            "KIDNEY" to "Kidney",
            "신장" to "Kidney",
            "RENAL" to "Kidney",
            "BLADDER" to "Bladder",
            "방광" to "Bladder",
            "URETER" to "Ureter",
            "요관" to "Ureter",
            "URETHRA" to "Urethra",
            "요도" to "Urethra",
            "ADRENAL" to "Adrenal",
            "부신" to "Adrenal",
            "TESTIS" to "Testis",
            "TESTICLE" to "Testis",
            "고환" to "Testis",
            "OVARY" to "Ovary",
            "난소" to "Ovary",
            "UTERUS" to "Uterus",
            "자궁" to "Uterus",
            "CERVIX" to "Cervix",
            "자궁경부" to "Cervix",
            "PELVIS" to "Pelvis",
            "골반" to "Pelvis",
            "ABDOMEN" to "Abdomen",
            "복부" to "Abdomen",
            "KUB" to "KUB",
            "IVP" to "IVP",
            "RETROPERITONEUM" to "Retroperitoneum",
            "후복막" to "Retroperitoneum"
        )

        // 일반 신체부위
        val generalParts = mapOf(
            "CHEST" to "Chest",
            "흉부" to "Chest",
            "BRAIN" to "Brain",
            "뇌" to "Brain",
            "SPINE" to "Spine",
            "척추" to "Spine",
            "LIVER" to "Liver",
            "간" to "Liver",
            "BREAST" to "Breast",
            "유방" to "Breast"
        )

        // 비뇨생식계 먼저 검색
        val bodyPart = urogenitalExams.entries.firstOrNull {
            upper.contains(it.key.uppercase())
        }?.value
            ?: generalParts.entries.firstOrNull {
                upper.contains(it.key.uppercase())
            }?.value
            ?: ""

        // 조합: bodyPart + modality
        val combined = listOf(bodyPart, modality)
            .filter { it.isNotEmpty() }
            .joinToString(" ")

        // modality가 있으면 반드시 포함
        return when {
            combined.isNotEmpty() -> combined
            modality.isNotEmpty() -> modality  // bodyPart 못찾아도 modality는 표시
            else -> text.lines()
                .firstOrNull { it.isNotBlank() }
                ?.trim()?.take(30) ?: ""
        }
    }

    fun close() = recognizer.close()
}
