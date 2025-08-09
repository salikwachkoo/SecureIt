package com.mohammadsalik.secureit.core.security

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import java.util.*

object EncryptionManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "SecureVaultKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private val secureRandom = SecureRandom()

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                createKey()
            }
        } catch (e: Exception) {
            throw SecurityException("Failed to initialize encryption", e)
        }
    }

    private fun createKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encryptString(plaintext: String): String {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = generateIV()
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), spec)

            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = iv + encrypted
            return Base64.getEncoder().encodeToString(combined)
        } catch (e: Exception) {
            throw SecurityException("Failed to encrypt string", e)
        }
    }

    fun decryptString(encryptedText: String): String {
        try {
            val combined = Base64.getDecoder().decode(encryptedText)
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decrypted = cipher.doFinal(encrypted)
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            throw SecurityException("Failed to decrypt string", e)
        }
    }

    fun encryptFile(inputFile: File, outputFile: File) {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = generateIV()
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), spec)

            FileInputStream(inputFile).use { input ->
                FileOutputStream(outputFile).use { output ->
                    // Write IV first
                    output.write(iv)
                    
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        val encrypted = cipher.update(buffer, 0, bytesRead)
                        if (encrypted != null) {
                            output.write(encrypted)
                        }
                    }
                    
                    val finalBlock = cipher.doFinal()
                    output.write(finalBlock)
                }
            }
        } catch (e: Exception) {
            throw SecurityException("Failed to encrypt file", e)
        }
    }

    fun decryptFile(inputFile: File, outputFile: File) {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            FileInputStream(inputFile).use { input ->
                // Read IV first
                val iv = ByteArray(GCM_IV_LENGTH)
                input.read(iv)
                
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        val decrypted = cipher.update(buffer, 0, bytesRead)
                        if (decrypted != null) {
                            output.write(decrypted)
                        }
                    }
                    
                    val finalBlock = cipher.doFinal()
                    output.write(finalBlock)
                }
            }
        } catch (e: Exception) {
            throw SecurityException("Failed to decrypt file", e)
        }
    }

    fun encryptUri(context: Context, inputUri: Uri, outputFile: File) {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = generateIV()
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), spec)

            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    // Write IV first
                    output.write(iv)
                    
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        val encrypted = cipher.update(buffer, 0, bytesRead)
                        if (encrypted != null) {
                            output.write(encrypted)
                        }
                    }
                    
                    val finalBlock = cipher.doFinal()
                    output.write(finalBlock)
                }
            }
        } catch (e: Exception) {
            throw SecurityException("Failed to encrypt URI", e)
        }
    }

    fun generateSecureSalt(): String {
        val salt = ByteArray(32)
        secureRandom.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    fun generateSecureIV(): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)
        return iv
    }

    private fun generateIV(): ByteArray {
        return generateSecureIV()
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
            chars.addAll("!@#$%^&*()_+-=[]{}|;:,.<>?".toList())
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
            false
        }
    }
}