package com.example.data.local

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
  private const val ITERATIONS = 10_000
  private const val KEY_LENGTH_BITS = 256
  private const val SALT_BYTES = 16
  private const val ALGORITHM = "PBKDF2WithHmacSHA256"

  fun hash(password: String): String {
    val salt = ByteArray(SALT_BYTES)
    SecureRandom().nextBytes(salt)
    val derived = deriveKey(password, salt)
    val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
    val hashB64 = Base64.encodeToString(derived, Base64.NO_WRAP)
    return "$saltB64:$hashB64"
  }

  fun verify(password: String, storedHash: String): Boolean {
    val parts = storedHash.split(":")
    if (parts.size != 2) return false
    val salt = runCatching { Base64.decode(parts[0], Base64.NO_WRAP) }.getOrNull() ?: return false
    val expected = runCatching { Base64.decode(parts[1], Base64.NO_WRAP) }.getOrNull() ?: return false
    val actual = deriveKey(password, salt)
    return MessageDigest.isEqual(expected, actual)
  }

  private fun deriveKey(password: String, salt: ByteArray): ByteArray {
    val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
    return try {
      SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
    } finally {
      spec.clearPassword()
    }
  }
}
