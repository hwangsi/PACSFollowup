package com.example.pacsfollowup.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pacsfollowup.data.model.PatientRecord
import kotlinx.coroutines.delay

// ── Column widths for table view ──────────────────────────────────────────────
private object Cols {
    val DATE     = 100.dp
    val PT_ID    = 130.dp
    val EXAM     = 120.dp
    val FINDINGS = 220.dp
    val SAVED_AT = 150.dp
    val DEL      =  44.dp
}

// ═══════════════════════════════════════════════════════════════════════════════
//  MainScreen
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToCamera: () -> Unit
) {
    val filteredRecords  by viewModel.filteredRecords.collectAsState()
    val searchQuery      by viewModel.searchQuery.collectAsState()
    val isTableView      by viewModel.isTableView.collectAsState()
    val exportState      by viewModel.exportState.collectAsState()

    var searchActive      by remember { mutableStateOf(false) }
    var recordToDelete    by remember { mutableStateOf<PatientRecord?>(null) }
    var showExportDialog  by remember { mutableStateOf(false) }
    val focusRequester    = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Search: auto-focus / clear ────────────────────────────────────────────
    LaunchedEffect(searchActive) {
        if (searchActive) { delay(100); focusRequester.requestFocus() }
        else viewModel.setSearchQuery("")
    }

    // ── Export state: show snackbar ───────────────────────────────────────────
    LaunchedEffect(exportState) {
        when (val s = exportState) {
            is MainViewModel.ExportState.Success -> {
                snackbarHostState.showSnackbar(
                    "✅ Saved: ${s.filename}  (Downloads folder)",
                    duration = SnackbarDuration.Long
                )
                viewModel.clearExportState()
            }
            is MainViewModel.ExportState.Error -> {
                snackbarHostState.showSnackbar("❌ ${s.message}", duration = SnackbarDuration.Long)
                viewModel.clearExportState()
            }
            else -> Unit
        }
    }

    // ── Delete confirmation ───────────────────────────────────────────────────
    recordToDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Delete Record") },
            text  = {
                Text(
                    "Delete the record for Patient ID " +
                    "\"${rec.patientId.ifEmpty { "N/A" }}\"?\nThis cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteRecord(rec); recordToDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // ── Export password dialog ────────────────────────────────────────────────
    val isExporting = exportState is MainViewModel.ExportState.Deriving ||
                      exportState is MainViewModel.ExportState.Exporting
    if (showExportDialog) {
        ExportPasswordDialog(
            isExporting = isExporting,
            onConfirm   = { pw -> showExportDialog = false; viewModel.exportCsv(pw) },
            onDismiss   = { showExportDialog = false }
        )
    }

    // ── Main scaffold ─────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("PACS Follow-up") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    // Export CSV
                    IconButton(onClick = { showExportDialog = true }) {
                        if (isExporting)
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        else
                            Icon(Icons.Default.FileDownload, contentDescription = "Export CSV")
                    }
                    // Card / Table toggle
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (isTableView) Icons.Default.ViewStream
                                          else Icons.Default.TableChart,
                            contentDescription = if (isTableView) "Card View" else "Table View"
                        )
                    }
                    // Search toggle
                    IconButton(onClick = { searchActive = !searchActive }) {
                        Icon(
                            imageVector = if (searchActive) Icons.Default.Clear
                                          else Icons.Default.Search,
                            contentDescription = if (searchActive) "Close Search" else "Search"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCamera,
                icon    = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                text    = { Text("Capture PACS") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Search bar ────────────────────────────────────────────────────
            AnimatedVisibility(visible = searchActive,
                               enter = expandVertically(), exit = shrinkVertically()) {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .focusRequester(focusRequester),
                    placeholder   = { Text("Search by ID, exam, date, findings…") },
                    leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon  = {
                        if (searchQuery.isNotEmpty())
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                    },
                    singleLine = true
                )
            }

            if (searchActive && searchQuery.isNotBlank()) {
                Text(
                    text  = "${filteredRecords.size} result(s) found",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            // ── Content ───────────────────────────────────────────────────────
            when {
                filteredRecords.isEmpty() ->
                    EmptyState(isSearching = searchActive && searchQuery.isNotBlank())
                isTableView ->
                    TableView(filteredRecords, onDeleteClick = { recordToDelete = it })
                else ->
                    CardView(filteredRecords, onDeleteClick  = { recordToDelete = it })
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Export password dialog
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ExportPasswordDialog(
    isExporting: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pw  by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }

    val match    = pw == pw2
    val canExport = pw.length >= 4 && match && !isExporting

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        title = { Text("Export CSV") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Set a password to encrypt Patient IDs in the exported file.\n" +
                    "The same password is required to decrypt them.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value         = pw,
                    onValueChange = { pw = it },
                    label         = { Text("Export password (min. 4 chars)") },
                    singleLine    = true,
                    enabled       = !isExporting,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = pw2,
                    onValueChange = { pw2 = it },
                    label         = { Text("Confirm password") },
                    singleLine    = true,
                    enabled       = !isExporting,
                    isError       = pw2.isNotEmpty() && !match,
                    supportingText = {
                        if (pw2.isNotEmpty() && !match)
                            Text("Passwords do not match",
                                 color = MaterialTheme.colorScheme.error)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier      = Modifier.fillMaxWidth()
                )
                if (isExporting) {
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Encrypting & saving…", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text(
                    "• File is saved to the Downloads folder (visible via USB)\n" +
                    "• Patient IDs: AES-256-GCM encrypted (PBKDF2/SHA-256, 100k iter)\n" +
                    "• Date, Exam, Findings are stored in plain text",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pw) }, enabled = canExport) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isExporting) { Text("Cancel") }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Card view
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CardView(
    records: List<PatientRecord>,
    onDeleteClick: (PatientRecord) -> Unit
) {
    LazyColumn(
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(records, key = { it.savedAt + it.patientId }) { record ->
            SwipeToDeleteWrapper(onDelete = { onDeleteClick(record) }) {
                RecordCard(record = record, onDeleteClick = { onDeleteClick(record) })
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Table view
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TableView(
    records: List<PatientRecord>,
    onDeleteClick: (PatientRecord) -> Unit
) {
    // All header + data rows share this scroll state → synchronized horizontal scroll
    val hScroll = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {

        // Sticky header row
        Surface(
            color          = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(hScroll)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TH("Date",       Cols.DATE)
                ColSep()
                TH("Patient ID", Cols.PT_ID)
                ColSep()
                TH("Exam",       Cols.EXAM)
                ColSep()
                TH("Findings",   Cols.FINDINGS)
                ColSep()
                TH("Saved At",   Cols.SAVED_AT)
                ColSep()
                TH("Del",        Cols.DEL)
            }
        }
        HorizontalDivider(thickness = 1.dp)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(records, key = { it.savedAt + it.patientId }) { record ->
                TableRow(record = record, hScroll = hScroll, onDeleteClick = { onDeleteClick(record) })
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color     = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@Composable
private fun TableRow(
    record: PatientRecord,
    hScroll: ScrollState,
    onDeleteClick: () -> Unit
) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(revealed) { if (revealed) { delay(3_000); revealed = false } }

    val displayId = when {
        record.patientId.isEmpty() -> "—"
        revealed                   -> record.patientId
        else                       -> maskId(record.patientId)
    }

    Row(
        modifier = Modifier
            .horizontalScroll(hScroll)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TD(record.date.ifEmpty { "—" },       Cols.DATE)
        ColSep()

        // Patient ID — tap to reveal
        Box(
            modifier = Modifier
                .width(Cols.PT_ID)
                .clickable(
                    enabled             = record.patientId.isNotEmpty(),
                    interactionSource   = remember { MutableInteractionSource() },
                    indication          = null
                ) { revealed = !revealed },
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text       = displayId,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = if (revealed) FontWeight.Bold else FontWeight.Normal,
                color      = if (revealed) MaterialTheme.colorScheme.primary
                             else MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }
        ColSep()

        TD(record.examName.ifEmpty { "—" },   Cols.EXAM)
        ColSep()

        // Findings — 2 lines allowed
        Text(
            text     = record.findings.ifEmpty { "—" },
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(Cols.FINDINGS)
        )
        ColSep()

        TD(record.savedAt.ifEmpty { "—" },    Cols.SAVED_AT)
        ColSep()

        // Delete
        IconButton(onClick = onDeleteClick, modifier = Modifier.size(Cols.DEL)) {
            Icon(
                imageVector        = Icons.Default.Delete,
                contentDescription = "Delete",
                tint               = MaterialTheme.colorScheme.outline,
                modifier           = Modifier.size(18.dp)
            )
        }
    }
}

// ── Table header / data cell helpers ──────────────────────────────────────────

@Composable
private fun TH(text: String, width: Dp) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines   = 1,
        overflow   = TextOverflow.Ellipsis,
        modifier   = Modifier.width(width)
    )
}

@Composable
private fun TD(text: String, width: Dp) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(width)
    )
}

@Composable
private fun ColSep() {
    Spacer(modifier = Modifier.width(4.dp))
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(18.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
    Spacer(modifier = Modifier.width(4.dp))
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Swipe-to-delete wrapper
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteWrapper(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            if (v == SwipeToDismissBoxValue.EndToStart) onDelete()
            false
        }
    )
    SwipeToDismissBox(
        state                       = state,
        enableDismissFromStartToEnd = false,
        backgroundContent           = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete, "Delete",
                    tint     = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { content() }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Record card  (card view)
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordCard(record: PatientRecord, onDeleteClick: () -> Unit) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(revealed) { if (revealed) { delay(3_000); revealed = false } }

    val displayId = when {
        record.patientId.isEmpty() -> "No Patient ID"
        revealed                   -> record.patientId
        else                       -> maskId(record.patientId)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick  = { if (record.patientId.isNotEmpty()) revealed = !revealed }
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = displayId,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = if (revealed) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.onSurface,
                    modifier   = Modifier.weight(1f)
                )
                Text(record.date,
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.outline)
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Delete",
                         tint     = MaterialTheme.colorScheme.outline,
                         modifier = Modifier.size(18.dp))
                }
            }
            if (record.examName.isNotEmpty())
                AssistChip(onClick = {},
                           label   = { Text(record.examName,
                                            style = MaterialTheme.typography.labelSmall) })
            if (record.findings.isNotEmpty())
                Text(record.findings,
                     style    = MaterialTheme.typography.bodyMedium,
                     maxLines = 2,
                     color    = MaterialTheme.colorScheme.onSurfaceVariant)
            if (record.savedAt.isNotEmpty())
                Text("Saved: ${record.savedAt}",
                     style = MaterialTheme.typography.labelSmall,
                     color = MaterialTheme.colorScheme.outline)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Empty state
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyState(isSearching: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.MedicalServices, null,
                 modifier = Modifier.size(72.dp),
                 tint     = MaterialTheme.colorScheme.outline)
            Text(
                text  = if (isSearching) "No matching records" else "No records found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
            if (!isSearching)
                Text(
                    "Tap the button below to capture a PACS screen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
        }
    }
}

// ── Shared helper ──────────────────────────────────────────────────────────────

private fun maskId(id: String): String =
    if (id.length > 3) id.take(3) + "****" else "****"
