# ──────────────────────────────────────────
# PatientRecord - Gson 직렬화 보존
# ──────────────────────────────────────────
-keep class com.example.pacsfollowup.data.model.** { *; }

# ──────────────────────────────────────────
# Gson
# ──────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ──────────────────────────────────────────
# Google API Client (Sheets API, HTTP)
# ──────────────────────────────────────────
-keep class com.google.api.** { *; }
-keep class com.google.apis.** { *; }
-keep class com.google.auth.** { *; }
-dontwarn com.google.api.**
-dontwarn com.google.apis.**

# ──────────────────────────────────────────
# OkHttp (자체 consumer rules 있으나 명시)
# ──────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**

# ──────────────────────────────────────────
# 스택 트레이스 가독성 유지
# ──────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
