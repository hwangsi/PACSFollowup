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

    var showSheetDialog by remember { mutableStateOf(false) }
    var sheetIdInput by remember { mutableStateOf("") }
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
            title = { Text("Patient Data Protection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Please confirm the following before sending to Google Sheets:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "• No direct identifiers (name, SSN, etc.) are included\n" +
                        "• Patient ID complies with your institution's de-identification policy\n" +
                        "• External cloud transfer is permitted by IRB or institutional policy",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Responsibility remains with the user even after confirmation.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showConsentDialog = false
                    viewModel.saveRecord(onSaved)
                }) { Text("Confirm & Save") }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSheetDialog) {
        val extractedId = extractSpreadsheetId(sheetIdInput.trim())
        val isValidInput = extractedId.isNotBlank()

        AlertDialog(
            onDismissRequest = { showSheetDialog = false },
            title = { Text("Google Spreadsheet ID") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Paste the full URL or just the ID",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    OutlinedTextField(
                        value = sheetIdInput,
                        onValueChange = { sheetIdInput = it },
                        label = { Text("Sheets URL or ID") },
                        placeholder = { Text("https://docs.google.com/spreadsheets/d/...") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (sheetIdInput.isNotBlank()) {
                        Text(
                            text = if (isValidInput) "✅ ID: $extractedId"
                                   else "⚠️ Enter a valid Sheets URL or ID",
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
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showSheetDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter Findings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        sheetIdInput = viewModel.spreadsheetId
                        showSheetDialog = true
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Sheets Settings")
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
            capturedBitmap?.let { bitmap ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Captured Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            if (isProcessingOcr) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Analyzing...", style = MaterialTheme.typography.bodySmall)
                }
            }

            Text("Patient Info", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = patientId,
                onValueChange = viewModel::updatePatientId,
                label = { Text("Patient ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )

            OutlinedTextField(
                value = date,
                onValueChange = viewModel::updateDate,
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
            )

            OutlinedTextField(
                value = examName,
                onValueChange = viewModel::updateExamName,
                label = { Text("Exam") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.MedicalServices, contentDescription = null) }
            )

            HorizontalDivider()

            Text("Findings", style = MaterialTheme.typography.titleSmall)

            Text(
                "⚠️ Do not say patient names or IDs during voice input. Audio is sent to Google Cloud.",
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
                    Text(if (isRecording) "Stop Recording" else "Record Voice")
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
                        Text("Transcribing...", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            OutlinedTextField(
                value = findings,
                onValueChange = viewModel::updateFindings,
                label = { Text("Findings") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                maxLines = 8,
                placeholder = { Text("Auto-filled after voice recording, or type manually") }
            )

            Spacer(Modifier.height(4.dp))

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
                    Text("Saving...")
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save to Google Sheets")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Extracts Spreadsheet ID from a full URL or returns the input if already an ID.
 * e.g. https://docs.google.com/spreadsheets/d/ABC123/edit  →  ABC123
 */
private fun extractSpreadsheetId(input: String): String {
    val urlPattern = Regex("""/spreadsheets/d/([a-zA-Z0-9\-_]+)""")
    val match = urlPattern.find(input)
    if (match != null) return match.groupValues[1]
    return if (input.matches(Regex("[a-zA-Z0-9\\-_]+"))) input else ""
}
