package com.example.pacsfollowup.data.model

import java.io.Serializable

data class PatientRecord(
    val patientId: String,
    val date: String,
    val examName: String,
    val findings: String,
    val savedAt: String = ""
) : Serializable
