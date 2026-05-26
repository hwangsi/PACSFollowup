package com.example.pacsfollowup.speech

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SpeechToTextService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun transcribe(audioData: ByteArray, apiKey: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val audioBase64 = Base64.encodeToString(audioData, Base64.NO_WRAP)

                val requestBody = JSONObject().apply {
                    put("config", JSONObject().apply {
                        put("encoding", "LINEAR16")
                        put("sampleRateHertz", SpeechRecorder.SAMPLE_RATE)
                        put("languageCode", "ko-KR")
                        put("enableAutomaticPunctuation", true)
                        // model 미지정 시 ko-KR 기본 모델 사용 (latest_long은 짧은 발화 미지원)
                        put("speechContexts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("phrases", JSONArray().apply {
                                    // 영상의학 자주 쓰는 용어 힌트
                                    listOf(
                                        "전립선", "신장", "방광", "요관", "부신",
                                        "고환", "난소", "자궁", "골반", "복부",
                                        "CT", "MRI", "초음파", "PET", "X-ray",
                                        "결절", "종괴", "낭종", "석회화", "혈종",
                                        "추적 관찰", "이상 없음", "정상 범위",
                                        "크기 증가", "크기 감소", "변화 없음"
                                    ).forEach { put(it) }
                                })
                                put("boost", 15)
                            })
                        })
                    })
                    put("audio", JSONObject().apply {
                        put("content", audioBase64)
                    })
                }.toString()

                val request = Request.Builder()
                    .url("https://speech.googleapis.com/v1/speech:recognize?key=$apiKey")
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    error("STT API Error ${response.code}: $responseBody")
                }

                val results = JSONObject(responseBody).optJSONArray("results")
                    ?: return@runCatching ""

                buildString {
                    for (i in 0 until results.length()) {
                        val alternatives = results.getJSONObject(i).optJSONArray("alternatives")
                        if (alternatives != null && alternatives.length() > 0) {
                            append(alternatives.getJSONObject(0).getString("transcript"))
                            append(" ")
                        }
                    }
                }.trim()
            }
        }
}
