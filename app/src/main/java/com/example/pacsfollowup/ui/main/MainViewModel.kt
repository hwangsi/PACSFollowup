package com.example.pacsfollowup.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pacsfollowup.data.model.PatientRecord
import com.example.pacsfollowup.data.security.EncryptedPrefs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "pacs_prefs_secure"
        private const val KEY_RECORDS = "recent_records"
        private const val MAX_RECORDS = 20
    }

    private val prefs = EncryptedPrefs.create(application, PREFS_NAME)
    private val gson = Gson()

    private val _records = MutableStateFlow<List<PatientRecord>>(emptyList())
    val records: StateFlow<List<PatientRecord>> = _records.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** 검색어에 따라 실시간 필터링된 목록 */
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

    init {
        loadRecords()
    }

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
        val updated = _records.value.toMutableList().apply { remove(record) }
        save(updated)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun save(list: List<PatientRecord>) {
        _records.value = list
        prefs.edit().putString(KEY_RECORDS, gson.toJson(list)).apply()
    }
}
