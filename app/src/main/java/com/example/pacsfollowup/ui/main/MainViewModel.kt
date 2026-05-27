package com.example.pacsfollowup.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pacsfollowup.data.export.CsvExporter
import com.example.pacsfollowup.data.model.PatientRecord
import com.example.pacsfollowup.data.security.EncryptedPrefs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME  = "pacs_prefs_secure"
        private const val KEY_RECORDS = "recent_records"
        private const val MAX_RECORDS = 20
    }

    private val prefs   = EncryptedPrefs.create(application, PREFS_NAME)
    private val gson    = Gson()
    private val exporter = CsvExporter(application)

    // ── Records ────────────────────────────────────────────────────────────────

    private val _records = MutableStateFlow<List<PatientRecord>>(emptyList())
    val records: StateFlow<List<PatientRecord>> = _records.asStateFlow()

    // ── Search ─────────────────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Real-time filtered list based on [_searchQuery]. */
    val filteredRecords: StateFlow<List<PatientRecord>> =
        combine(_records, _searchQuery) { records, query ->
            if (query.isBlank()) records
            else records.filter { r ->
                r.patientId.contains(query, ignoreCase = true) ||
                r.examName.contains(query, ignoreCase = true) ||
                r.findings.contains(query, ignoreCase = true) ||
                r.date.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── View mode ──────────────────────────────────────────────────────────────

    private val _isTableView = MutableStateFlow(false)
    val isTableView: StateFlow<Boolean> = _isTableView.asStateFlow()

    fun toggleViewMode() { _isTableView.value = !_isTableView.value }

    // ── Export ─────────────────────────────────────────────────────────────────

    sealed class ExportState {
        object Idle                               : ExportState()
        object Deriving                           : ExportState()   // PBKDF2 in progress
        object Exporting                          : ExportState()   // file I/O in progress
        data class Success(
            val uri: Uri,
            val filename: String
        )                                         : ExportState()
        data class Error(val message: String)     : ExportState()
    }

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    /**
     * Exports the current record list to a CSV file with patient IDs encrypted
     * using [password]. Runs entirely on IO thread.
     */
    fun exportCsv(password: String) {
        if (_records.value.isEmpty()) {
            _exportState.value = ExportState.Error("No records to export.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _exportState.value = ExportState.Deriving
            exporter.export(_records.value, password)
                .onSuccess { result ->
                    _exportState.value = ExportState.Success(result.uri, result.displayName)
                }
                .onFailure { e ->
                    _exportState.value = ExportState.Error(e.message ?: "Export failed")
                }
        }
    }

    fun clearExportState() { _exportState.value = ExportState.Idle }

    // ── CRUD ───────────────────────────────────────────────────────────────────

    init { loadRecords() }

    private fun loadRecords() {
        val json = prefs.getString(KEY_RECORDS, null) ?: return
        val type = object : TypeToken<List<PatientRecord>>() {}.type
        _records.value = gson.fromJson(json, type) ?: emptyList()
    }

    fun addRecord(record: PatientRecord) {
        val updated = _records.value.toMutableList().apply {
            add(0, record)
            if (size > MAX_RECORDS) removeAt(size - 1)
        }
        save(updated)
    }

    fun deleteRecord(record: PatientRecord) {
        save(_records.value.toMutableList().apply { remove(record) })
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    private fun save(list: List<PatientRecord>) {
        _records.value = list
        prefs.edit().putString(KEY_RECORDS, gson.toJson(list)).apply()
    }
}
