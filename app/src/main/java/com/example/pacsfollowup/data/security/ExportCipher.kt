package com.example.pacsfollowup.data.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-based AES-256-GCM encryption for CSV export.
 *
 * Usage flow:
 *   val salt = ExportCipher.generateSalt()
 *   val key  = ExportCipher.deriveKey(password, salt)
 *   // per-record:
 *   val enc  = ExportCipher.encryptWithKey(patientId, key)
 *   // decrypt:
 *   val dec  = ExportCipher.decryptWithKey(enc, key)
 *
 * The salt (hex) is stored in the CSV comment header so the file is self-contained.
 * Security relies solely on the password; the salt is not secret but prevents
 * precomputation (rainbow table) attacks.
 */
object ExportCipher {

    private const val TRANSFORMATION   = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM    = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS       = 100_000
    private const val KEY_BITS         = 256
    private const val GCM_TAG_BITS     = 128
    private const val IV_SIZE          = 12   // bytes, GCM recommended
    const val SALT_SIZE                = 16   // bytes

    // ── Key management ────────────────────────────────────────────────────────

    fun generateSalt(): ByteArray = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }

    /**
     * PBKDF2 key derivation. This is intentionally slow (100k iterations).
     * Call once per export/import session, not per record.
     */
    fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val spec    = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val raw     = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(raw, "AES")
    }

    // ── Encrypt / Decrypt ─────────────────────────────────────────────────────

    /**
     * Encrypts [plain] with [key] using AES-256-GCM.
     * Returns Base64(randomIV[12] + ciphertext+tag).
     * Empty string input returns empty string output.
     */
    fun encryptWithKey(plain: String, key: SecretKeySpec): String {
        if (plain.isBlank()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv                                    // 12-byte random IV
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + ct, Base64.NO_WRAP)
    }

    /**
     * Decrypts a value produced by [encryptWithKey].
     * Returns null if decryption fails (wrong password, corrupt data, etc.).
     * Returns "" if [encoded] is blank.
     */
    fun decryptWithKey(encoded: String, key: SecretKeySpec): String? {
        if (encoded.isBlank()) return ""
        return try {
            val data = Base64.decode(encoded, Base64.NO_WRAP)
            if (data.size <= IV_SIZE) return null
            val iv = data.copyOfRange(0, IV_SIZE)
            val ct = data.copyOfRange(IV_SIZE, data.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ct), Charsets.UTF_8)
        } catch (_: Exception) {
            null   // wrong password → GCM tag verification fails
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun saltToHex(salt: ByteArray): String =
        salt.joinToString("") { "%02x".format(it) }

    fun hexToSalt(hex: String): ByteArray =
        ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
