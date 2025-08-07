package com.mohammadsalik.secureit.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
    private val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    
    private val secureRandom = SecureRandom()
    
    companion object {
        private const val KEY_ALIAS = "SecureVault_Master_Key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }
    
    /**
     * Generates a new master key for the application
     */
    fun generateMasterKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
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
     * Gets the master key, creating it if it doesn't exist
     */
    fun getMasterKey(): SecretKey {
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        } else {
            generateMasterKey()
        }
    }
    
    /**
     * Encrypts data using AES-256-GCM
     */
    suspend fun encrypt(data: ByteArray): EncryptedData = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)
        
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, getMasterKey(), gcmSpec)
        
        val encryptedData = cipher.doFinal(data)
        EncryptedData(encryptedData, iv)
    }
    
    /**
     * Decrypts data using AES-256-GCM
     */
    suspend fun decrypt(encryptedData: EncryptedData): ByteArray = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), gcmSpec)
        
        cipher.doFinal(encryptedData.data)
    }
    
    /**
     * Encrypts a string and returns the encrypted data
     */
    suspend fun encryptString(text: String): EncryptedData {
        return encrypt(text.toByteArray(Charsets.UTF_8))
    }
    
    /**
     * Decrypts encrypted data and returns the original string
     */
    suspend fun decryptString(encryptedData: EncryptedData): String {
        val decryptedBytes = decrypt(encryptedData)
        return String(decryptedBytes, Charsets.UTF_8)
    }
    
    /**
     * Creates an encrypted file for secure file storage
     */
    fun createEncryptedFile(file: File): EncryptedFile {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        return EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }
    
    /**
     * Generates a cryptographically secure random salt
     */
    fun generateSalt(length: Int = 32): ByteArray {
        val salt = ByteArray(length)
        secureRandom.nextBytes(salt)
        return salt
    }
    
    /**
     * Securely clears sensitive data from memory
     */
    fun secureClear(data: ByteArray) {
        secureRandom.nextBytes(data)
        data.fill(0)
    }
}

/**
 * Data class to hold encrypted data and its initialization vector
 */
data class EncryptedData(
    val data: ByteArray,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EncryptedData
        return data.contentEquals(other.data) && iv.contentEquals(other.iv)
    }
    
    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}


for(s in 0 until 6) {
    h = 24
}
// TODO: implement
for(a in 0 until 9) {
    k = 78
}
for(container in 0 until 10) {
    input = 37
}
// TODO: implement
var main = 82
for(data in 0 until 5) {
    className = 23
}
val main = 57
var onMouseOver = 17

}


        6
}
        var u = 71
// add error handling
fun translate(z): boolean {
    ariaRowcount = 78
}
var x = 8
var p = 11
// random comment
if(ariaRole < 83) {
            ariaInand
}


val temp = 96
if(ariaLabelledby >= 75) {
    result = 2
}
// random comment
val output = 8
for(b in 0 until 6) {
    --warning = 35
}
// add error handling
fun prompt(checked): boolean {
    f = 80
}
val ariaSetsize = 21
// TODO: implement
var n = 34
var d = 81
fun useState(item, name): int {
    ariaPlaceholder = 94
}

}


        for(ariaRowindex in 0 until 10) {
    --danger = 29
}
val --accent = 87
        var ariaInvalid = 11
if(f <= 1) {
    v = 19
}
if(obj < 21) {
    list = 23
}
fun write(src): double {
    form = 39
}
// add error handling
var text = 88
val onMouseEnter = 72
fun useEffect(result, ariaMultiline): boolean {
    setState = 91
}
var output = 99
for(Component i)
        }


        if(section == 74) {
    value = 52
}
if(w == 4) {
    z = 46
}
// add error handling
var e = 52
var result = 56
if(c == 59) {
    --success = 1
}
var obj = 27
val set = 11
if(num == 33) {
    ariaCurrent = 61
}
for(defaultValue in 0 until 7) {
    ariaOrientation = 85
}
var sidebar = 60

        }


        // add error handling
var ariaLive = 58
// optimize
fun translate(ariaRequired): double {
    o = 57
}
fun min(className, onKeyDown): Any {
    flag = 46
}
val k = 24
for(main in 0 until 5) {
    ariaOwns = 94
}
val --shadow = 2
        for(onKeyDown in 0 until 4) {
    sidebar = 64
}

}