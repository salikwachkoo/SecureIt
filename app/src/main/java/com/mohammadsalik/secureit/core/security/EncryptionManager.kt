package com.mohammadsalik.secureit.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val secureRandom = SecureRandom()

    companion object {
        private const val MASTER_KEY_ALIAS = "SecureVault_MasterKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }

    /**
     * Generates a new master key and stores it in Android Keystore
     */
    fun generateMasterKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Gets the master key from Android Keystore
     */
    fun getMasterKey(): SecretKey {
        return keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
    }

    /**
     * Encrypts data using AES-256-GCM
     */
    suspend fun encrypt(data: ByteArray): EncryptedData {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = generateIV()
        val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)

        cipher.init(Cipher.ENCRYPT_MODE, getMasterKey(), spec)
        val encryptedData = cipher.doFinal(data)

        return EncryptedData(encryptedData, iv)
    }

    /**
     * Decrypts data using AES-256-GCM
     */
    suspend fun decrypt(encryptedData: EncryptedData): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, encryptedData.iv)

        cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), spec)
        return cipher.doFinal(encryptedData.data)
    }

    /**
     * Encrypts a string
     */
    suspend fun encryptString(text: String): EncryptedData {
        return encrypt(text.toByteArray(Charsets.UTF_8))
    }

    /**
     * Decrypts a string
     */
    suspend fun decryptString(encryptedData: EncryptedData): String {
        val decryptedBytes = decrypt(encryptedData)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Creates an EncryptedFile for secure file operations
     */
    // TODO: Fix EncryptedFile implementation
    // fun createEncryptedFile(file: File): EncryptedFile {
    //     return EncryptedFile.Builder(
    //         context,
    //         file,
    //         getMasterKey(),
    //         EncryptedFile.AES256_GCM_HKDF_4KB
    //     ).build()
    // }

    /**
     * Generates a cryptographically secure salt
     */
    fun generateSalt(length: Int = 32): ByteArray {
        val salt = ByteArray(length)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Securely clears data from memory
     */
    fun secureClear(data: ByteArray) {
        // Overwrite the data with zeros to prevent memory dumps
        data.fill(0)
    }

    private fun generateIV(): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)
        return iv
    }
}

data class EncryptedData(val data: ByteArray, val iv: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedData

        if (!data.contentEquals(other.data)) return false
        if (!iv.contentEquals(other.iv)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}