package com.example.pacsfollowup.ui.review

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.pacsfollowup.data.model.PatientRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    capturedBitmap: Bitmap?,
    viewModel: ReviewViewModel,
    onSaved: (PatientRecord) -> Unit,
    onBack: () -> Unit
) {
    val patientId by viewModel.patientId.collectAsState()
    val date by viewModel.date.collectAsState()
    val examName by viewModel.examName.collectAsState()
    val findings by viewModel.findings.collectAsState()
    val isProcessingOcr by viewModel.isProcessingOcr.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(capturedBitmap) {
        capturedBitmap?.let { viewModel.processImage(it) }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }

    // Spreadsheet ID 다이얼로그
    var showSheetDialog by remember { mutableStateOf(false) }
    var sheetIdInput by remember { mutableStateOf("") }

    // 비식별화 확인 다이얼로그
    var showConsentDialog by remember { mutableStateOf(false) }

    fun attemptSave() {
        if (viewModel.spreadsheetId.isEmpty()) {
            sheetIdInput = ""
            showSheetDialog = true
            return
        }
        showConsentDialog = true
    }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = { Text("환자정보 보호 확인") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Google Sheets로 전송하기 전에 다음을 확인해주세요:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "• 환자명·주민번호 등 직접 식별자가 포함되어 있지 않은지\n" +
                        "• 환자 ID가 기관의 비식별화/가명화 규정을 준수했는지\n" +
                        "• IRB 또는 업무 규정상 외부 클라우드 전송이 허용되는지",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "확인 후에도 책임은 입력자에게 있습니다.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showConsentDialog = false
                    viewModel.saveRecord(onSaved)
                }) { Text("확인 후 저장") }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) { Text("취소") }
            }
        )
    }

    if (showSheetDialog) {
        // URL 전체 붙여넣기 시 ID 자동 추출
        val extractedId = extractSpreadsheetId(sheetIdInput.trim())
        val isValidInput = extractedId.isNotBlank()

        AlertDialog(
            onDismissRequest = { showSheetDialog = false },
            title = { Text("Google Spreadsheet ID") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "URL 전체 또는 ID만 붙여넣기 하세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    OutlinedTextField(
                        value = sheetIdInput,
                        onValueChange = { sheetIdInput = it },
                        label = { Text("Sheets URL 또는 ID") },
                        placeholder = { Text("https://docs.google.com/spreadsheets/d/...") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 추출된 ID 미리보기
                    if (sheetIdInput.isNotBlank()) {
                        Text(
                            text = if (isValidInput) "✅ ID: $extractedId"
                                   else "⚠️ 올바른 Sheets URL 또는 ID를 입력하세요",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isValidInput) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setSpreadsheetId(extractedId)
                        showSheetDialog = false
                        attemptSave()
                    },
                    enabled = isValidInput
                ) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showSheetDialog = false }) { Text("취소") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("판독 소견 입력") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        sheetIdInput = viewModel.spreadsheetId
                        showSheetDialog = true
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Sheets 설정")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 촬영 이미지 미리보기
            capturedBitmap?.let { bitmap ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "촬영 이미지",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // OCR 처리 중 표시
            if (isProcessingOcr) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("OCR 분석 중...", style = MaterialTheme.typography.bodySmall)
                }
            }

            // 기본 정보 섹션
            Text("기본 정보", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = patientId,
                onValueChange = viewModel::updatePatientId,
                label = { Text("환자 ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )

            OutlinedTextField(
                value = date,
                onValueChange = viewModel::updateDate,
                label = { Text("날짜") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
            )

            OutlinedTextField(
                value = examName,
                onValueChange = viewModel::updateExamName,
                label = { Text("영상검사명") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.MedicalServices, contentDescription = null) }
            )

            HorizontalDivider()

            // 소견 입력 섹션
            Text("판독 소견", style = MaterialTheme.typography.titleSmall)

            Text(
                "⚠️ 음성 입력 시 환자명·환자번호 등 식별정보를 말하지 마세요. " +
                "녹음은 Google Cloud로 전송됩니다.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (isRecording) viewModel.stopRecording()
                        else viewModel.startRecording()
                    },
                    enabled = !isTranscribing,
                    colors = if (isRecording) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isRecording) "녹음 중지" else "음성 녹음")
                }

                when {
                    isRecording -> Text(
                        "● REC",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                    isTranscribing -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Text("변환 중...", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            OutlinedTextField(
                value = findings,
                onValueChange = viewModel::updateFindings,
                label = { Text("소견") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                maxLines = 8,
                placeholder = { Text("음성 녹음 후 자동 입력되거나 직접 입력하세요") }
            )

            Spacer(Modifier.height(4.dp))

            // 저장 버튼
            Button(
                onClick = { attemptSave() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && !isProcessingOcr && !isRecording
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("저장 중...")
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Google Sheets에 저장")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * URL 전체 또는 ID만 입력해도 Spreadsheet ID 추출
 * 예) https://docs.google.com/spreadsheets/d/ABC123/edit#gid=0  →  ABC123
 *     ABC123  →  ABC123
 */
private fun extractSpreadsheetId(input: String): String {
    val urlPattern = Regex("""/spreadsheets/d/([a-zA-Z0-9\-_]+)""")
    val match = urlPattern.find(input)
    if (match != null) return match.groupValues[1]
    // URL이 아닌 경우 ID 직접 입력으로 간주 (영문·숫자·하이픈·언더스코어)
    return if (input.matches(Regex("[a-zA-Z0-9\\-_]+"))) input else ""
}
