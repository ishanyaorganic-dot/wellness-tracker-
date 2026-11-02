package com.dailywellness.tracker.security

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinManager(context: Context) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    fun isConfigured() = prefs.contains("pin_hash") && prefs.contains("pin_salt")
    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derive(pin, salt)
        prefs.edit().putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pin_hash", Base64.encodeToString(hash, Base64.NO_WRAP)).apply()
    }
    fun verify(pin: String): Boolean {
        val saltText = prefs.getString("pin_salt", null) ?: return false
        val hashText = prefs.getString("pin_hash", null) ?: return false
        return java.security.MessageDigest.isEqual(
            Base64.decode(hashText, Base64.NO_WRAP),
            derive(pin, Base64.decode(saltText, Base64.NO_WRAP))
        )
    }
    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 150_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }
}