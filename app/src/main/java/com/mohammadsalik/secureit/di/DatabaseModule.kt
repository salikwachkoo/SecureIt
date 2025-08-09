package com.mohammadsalik.secureit.di

import android.content.Context
import com.mohammadsalik.secureit.core.security.BiometricAuthManager
import com.mohammadsalik.secureit.core.security.EncryptionManager
import com.mohammadsalik.secureit.core.security.PinAuthManager
import com.mohammadsalik.secureit.core.security.SessionManager
import com.mohammadsalik.secureit.data.local.database.SecureVaultDatabase
import com.mohammadsalik.secureit.data.local.database.dao.DocumentDao
import com.mohammadsalik.secureit.data.local.database.dao.PasswordDao
import com.mohammadsalik.secureit.data.local.database.dao.SecureNoteDao
import com.mohammadsalik.secureit.data.repository.DocumentRepositoryImpl
import com.mohammadsalik.secureit.data.repository.PasswordRepositoryImpl
import com.mohammadsalik.secureit.data.repository.SecureNoteRepositoryImpl
import com.mohammadsalik.secureit.domain.repository.DocumentRepository
import com.mohammadsalik.secureit.domain.repository.PasswordRepository
import com.mohammadsalik.secureit.domain.repository.SecureNoteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.mohammadsalik.secureit.core.audit.AuditLogger
import com.mohammadsalik.secureit.core.audit.LogcatAuditLogger

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSecureVaultDatabase(@ApplicationContext context: Context): SecureVaultDatabase {
        return SecureVaultDatabase.getDatabase(context, "SecureVault_2024!")
    }

    @Provides
    @Singleton
    fun providePasswordDao(database: SecureVaultDatabase): PasswordDao {
        return database.passwordDao()
    }

    @Provides
    @Singleton
    fun provideDocumentDao(database: SecureVaultDatabase): DocumentDao {
        return database.documentDao()
    }

    @Provides
    @Singleton
    fun provideSecureNoteDao(database: SecureVaultDatabase): SecureNoteDao {
        return database.secureNoteDao()
    }

    // EncryptionManager is now an object, no need to provide it

    @Provides
    @Singleton
    fun provideBiometricAuthManager(@ApplicationContext context: Context): BiometricAuthManager {
        return BiometricAuthManager(context)
    }

    @Provides
    @Singleton
    fun providePinAuthManager(
        @ApplicationContext context: Context
    ): PinAuthManager {
        return PinAuthManager(context)
    }

    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }

    @Provides
    @Singleton
    fun providePasswordRepository(
        passwordDao: PasswordDao
    ): PasswordRepository {
        return PasswordRepositoryImpl(passwordDao)
    }

    @Provides
    @Singleton
    fun provideDocumentRepository(
        documentDao: DocumentDao
    ): DocumentRepository {
        return DocumentRepositoryImpl(documentDao)
    }

    @Provides
    @Singleton
    fun provideSecureNoteRepository(
        secureNoteDao: SecureNoteDao
    ): SecureNoteRepository {
        return SecureNoteRepositoryImpl(secureNoteDao)
    }

    @Provides
    @Singleton
    fun provideAuditLogger(): AuditLogger = LogcatAuditLogger()
}