package com.example.pacsfollowup.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.pacsfollowup.data.model.PatientRecord
import com.example.pacsfollowup.data.security.EncryptedPrefs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        _records.value = updated
        prefs.edit().putString(KEY_RECORDS, gson.toJson(updated)).apply()
    }
}
