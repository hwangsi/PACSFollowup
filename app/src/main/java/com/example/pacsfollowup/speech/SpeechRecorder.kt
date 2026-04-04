package com.example.pacsfollowup.speech

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class SpeechRecorder {

    companion object {
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private val active = AtomicBoolean(false)
    private val audioBuffer = ByteArrayOutputStream()
    private var recordJob: Job? = null

    val isRecording: Boolean get() = active.get()

    fun start(scope: CoroutineScope) {
        audioBuffer.reset()
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            minBuffer * 4
        )
        active.set(true)
        audioRecord?.startRecording()

        recordJob = scope.launch(Dispatchers.IO) {
            val chunk = ByteArray(minBuffer)
            while (active.get()) {
                val read = audioRecord?.read(chunk, 0, chunk.size) ?: 0
                if (read > 0) {
                    synchronized(audioBuffer) { audioBuffer.write(chunk, 0, read) }
                }
            }
        }
    }

    suspend fun stop(): ByteArray {
        active.set(false)
        recordJob?.join()
        recordJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        return synchronized(audioBuffer) { audioBuffer.toByteArray() }
    }
}
