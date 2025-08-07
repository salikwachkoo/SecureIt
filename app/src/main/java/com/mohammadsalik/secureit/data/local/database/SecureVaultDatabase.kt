package com.mohammadsalik.secureit.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.mohammadsalik.secureit.data.local.database.dao.DocumentDao
import com.mohammadsalik.secureit.data.local.database.dao.PasswordDao
import com.mohammadsalik.secureit.data.local.database.dao.SecureNoteDao
import com.mohammadsalik.secureit.data.local.database.entities.DocumentEntity
import com.mohammadsalik.secureit.data.local.database.entities.PasswordEntity
import com.mohammadsalik.secureit.data.local.database.entities.SecureNoteEntity
import com.mohammadsalik.secureit.data.local.database.converters.DateTimeConverters
import dagger.hilt.android.qualifiers.ApplicationContext
import net.sqlcipher.database.SupportFactory
import javax.inject.Inject
import javax.inject.Singleton

@Database(
    entities = [
        PasswordEntity::class,
        DocumentEntity::class,
        SecureNoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateTimeConverters::class)
abstract class SecureVaultDatabase : RoomDatabase() {

    abstract fun passwordDao(): PasswordDao
    abstract fun documentDao(): DocumentDao
    abstract fun secureNoteDao(): SecureNoteDao

    companion object {
        private const val DATABASE_NAME = "secure_vault.db"
        const val DATABASE_PASSWORD = "SecureVault_2024!" // This should be encrypted and stored securely

        @Volatile
        private var INSTANCE: SecureVaultDatabase? = null

        fun getDatabase(context: Context, password: String): SecureVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SecureVaultDatabase::class.java,
                    DATABASE_NAME
                )
                    .openHelperFactory(SupportFactory(password.toByteArray()))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Singleton
class DatabaseModule @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun provideDatabase(): SecureVaultDatabase {
        return SecureVaultDatabase.getDatabase(context, SecureVaultDatabase.DATABASE_PASSWORD)
    }

    fun providePasswordDao(database: SecureVaultDatabase): PasswordDao {
        return database.passwordDao()
    }

    fun provideDocumentDao(database: SecureVaultDatabase): DocumentDao {
        return database.documentDao()
    }

    fun provideSecureNoteDao(database: SecureVaultDatabase): SecureNoteDao {
        return database.secureNoteDao()
    }
}