package com.mohammadsalik.secureit.core.security

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.SecureRandom
import java.util.*

object EncryptionManager {
    private const val TAG = "EncryptionManager"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "SecureVaultKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128 // bits

    private val secureRandom = SecureRandom()

    init {
        ensureKeyExists()
    }

    @Synchronized
    private fun ensureKeyExists() {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                createKey()
            }
        } catch (e: Exception) {
            Log.e(TAG, "ensureKeyExists error", e)
            throw SecurityException("Failed to initialize encryption", e)
        }
    }

    @Synchronized
    private fun createKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    @Synchronized
    private fun regenerateKey() {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete old key", e)
        }
        createKey()
    }

    @Synchronized
    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encryptString(plaintext: String): String {
        return tryEncrypt(plaintext) ?: run {
            Log.w(TAG, "encryptString first attempt failed; regenerating key and retrying")
            regenerateKey()
            tryEncrypt(plaintext) ?: throw SecurityException("Failed to encrypt string after key regeneration")
        }
    }

    private fun tryEncrypt(plaintext: String): String? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            // Let keystore generate a random IV
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv ?: return null
            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = iv + encrypted
            Base64.getEncoder().encodeToString(combined)
        } catch (e: UnrecoverableKeyException) {
            Log.e(TAG, "UnrecoverableKeyException during encrypt", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "encrypt error", e)
            null
        }
    }

    fun decryptString(encryptedText: String): String {
        return tryDecrypt(encryptedText) ?: run {
            Log.w(TAG, "decryptString first attempt failed; regenerating key and retrying")
            regenerateKey()
            tryDecrypt(encryptedText) ?: throw SecurityException("Failed to decrypt string after key regeneration")
        }
    }

    private fun tryDecrypt(encryptedText: String): String? {
        return try {
            val combined = Base64.getDecoder().decode(encryptedText)
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val decrypted = cipher.doFinal(encrypted)
            String(decrypted, Charsets.UTF_8)
        } catch (e: UnrecoverableKeyException) {
            Log.e(TAG, "UnrecoverableKeyException during decrypt", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "decrypt error", e)
            null
        }
    }

    fun encryptFile(inputFile: File, outputFile: File) {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            // Let keystore generate a random IV
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv ?: throw SecurityException("Failed to obtain IV")
            FileInputStream(inputFile).use { input ->
                FileOutputStream(outputFile).use { output ->
                    output.write(iv)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        val encrypted = cipher.update(buffer, 0, bytesRead)
                        if (encrypted != null) output.write(encrypted)
                    }
                    output.write(cipher.doFinal())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "encryptFile error", e)
            throw SecurityException("Failed to encrypt file", e)
        }
    }

    fun decryptFile(inputFile: File, outputFile: File) {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            FileInputStream(inputFile).use { input ->
                val iv = ByteArray(GCM_IV_LENGTH)
                input.read(iv)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        val decrypted = cipher.update(buffer, 0, bytesRead)
                        if (decrypted != null) output.write(decrypted)
                    }
                    output.write(cipher.doFinal())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "decryptFile error", e)
            throw SecurityException("Failed to decrypt file", e)
        }
    }

    fun encryptUri(context: Context, inputUri: Uri, outputFile: File) {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            // Let keystore generate a random IV
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv ?: throw SecurityException("Failed to obtain IV")
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    output.write(iv)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        val encrypted = cipher.update(buffer, 0, bytesRead)
                        if (encrypted != null) output.write(encrypted)
                    }
                    output.write(cipher.doFinal())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "encryptUri error", e)
            throw SecurityException("Failed to encrypt URI", e)
        }
    }

    fun generateSecureSalt(): String {
        val salt = ByteArray(32)
        secureRandom.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    private fun generateIV(): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)
        return iv
    }

    fun hashPassword(password: String, salt: String): String {
        try {
            val combined = password + salt
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(combined.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(hash)
        } catch (e: Exception) {
            throw SecurityException("Failed to hash password", e)
        }
    }

    fun verifyPassword(password: String, salt: String, hash: String): Boolean {
        val computedHash = hashPassword(password, salt)
        return computedHash == hash
    }

    fun generateSecurePassword(length: Int = 16, includeSpecial: Boolean = true): String {
        val chars = mutableListOf<Char>()
        chars.addAll('A'..'Z')
        chars.addAll('a'..'z')
        chars.addAll('0'..'9')
        
        if (includeSpecial) {
            chars.addAll("!@#\$%^&*()_+-=[]{}|;:,.<>?".toList())
        }

        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    fun isEncryptionAvailable(): Boolean {
        return try {
            ensureKeyExists()
            true
        } catch (e: Exception) {
            Log.e(TAG, "isEncryptionAvailable error", e)
            false
        }
    }
}