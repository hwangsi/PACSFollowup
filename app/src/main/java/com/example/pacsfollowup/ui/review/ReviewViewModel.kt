package com.example.pacsfollowup.ui.review

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pacsfollowup.BuildConfig
import com.example.pacsfollowup.data.model.PatientRecord
import com.example.pacsfollowup.data.repository.SheetsRepository
import com.example.pacsfollowup.ocr.OcrProcessor
import com.example.pacsfollowup.speech.SpeechRecorder
import com.example.pacsfollowup.speech.SpeechToTextService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val ocrProcessor = OcrProcessor()
    private val speechRecorder = SpeechRecorder()
    private val sttService = SpeechToTextService()
    private val sheetsRepository = SheetsRepository(application)

    val spreadsheetId: String get() = sheetsRepository.spreadsheetId
    fun setSpreadsheetId(id: String) { sheetsRepository.spreadsheetId = id }

    private val _patientId = MutableStateFlow("")
    val patientId: StateFlow<String> = _patientId.asStateFlow()

    private val _date = MutableStateFlow("")
    val date: StateFlow<String> = _date.asStateFlow()

    private val _examName = MutableStateFlow("")
    val examName: StateFlow<String> = _examName.asStateFlow()

    private val _findings = MutableStateFlow("")
    val findings: StateFlow<String> = _findings.asStateFlow()

    private val _isProcessingOcr = MutableStateFlow(false)
    val isProcessingOcr: StateFlow<Boolean> = _isProcessingOcr.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _isProcessingOcr.value = true
            try {
                val result = ocrProcessor.processImage(bitmap)
                _patientId.value = result.patientId
                _date.value = result.date
                _examName.value = result.examName
            } catch (e: Exception) {
                _errorMessage.value = "OCR 오류: ${e.message}"
            } finally {
                _isProcessingOcr.value = false
            }
        }
    }

    fun startRecording() {
        if (_isRecording.value) return
        _findings.value = ""  // 기존 소견 초기화
        speechRecorder.start(viewModelScope)
        _isRecording.value = true
    }

    fun stopRecording() {
        if (!_isRecording.value) return
        viewModelScope.launch {
            _isRecording.value = false
            val audio = speechRecorder.stop()
            if (audio.isEmpty()) return@launch

            // 최소 1초 미만(32000 bytes = 16kHz × 2byte × 1ch × 1sec)이면 안내
            val minBytes = SpeechRecorder.SAMPLE_RATE * 2 * 1
            if (audio.size < minBytes) {
                _errorMessage.value = "녹음 시간이 너무 짧습니다. 1초 이상 말씀해주세요."
                return@launch
            }

            _isTranscribing.value = true
            val apiKey = BuildConfig.SPEECH_API_KEY
            if (apiKey.isEmpty()) {
                _errorMessage.value = "SPEECH_API_KEY가 설정되지 않았습니다\ngradle.properties에 키를 추가해주세요"
                _isTranscribing.value = false
                return@launch
            }
            sttService.transcribe(audio, apiKey)
                .onSuccess { transcript ->
                    val trimmed = transcript.trim()
                    if (trimmed.isNotEmpty()) {
                        _findings.value = if (_findings.value.isEmpty()) trimmed
                                          else "${_findings.value} $trimmed"
                    } else {
                        _errorMessage.value = "음성이 인식되지 않았습니다. 다시 시도해주세요."
                    }
                }
                .onFailure { _errorMessage.value = "음성 인식 오류: ${it.message}" }
            _isTranscribing.value = false
        }
    }

    fun updatePatientId(v: String) { _patientId.value = v }
    fun updateDate(v: String) { _date.value = v }
    fun updateExamName(v: String) { _examName.value = v }
    fun updateFindings(v: String) { _findings.value = v }
    fun setError(message: String) { _errorMessage.value = message }
    fun clearError() { _errorMessage.value = null }

    fun saveRecord(onSuccess: (PatientRecord) -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            val record = PatientRecord(
                patientId = _patientId.value,
                date = _date.value,
                examName = _examName.value,
                findings = _findings.value
            )
            sheetsRepository.appendRecord(record)
                .onSuccess { onSuccess(record) }
                .onFailure { _errorMessage.value = "저장 실패: ${it.message}" }
            _isSaving.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        ocrProcessor.close()
    }
}
