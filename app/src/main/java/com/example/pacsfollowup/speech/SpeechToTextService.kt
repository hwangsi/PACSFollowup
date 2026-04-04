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
                        put("languageCode", "en-US")
                        put("enableAutomaticPunctuation", true)
                        put("model", "medical_dictation")
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
