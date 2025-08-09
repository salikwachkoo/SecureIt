package com.mohammadsalik.secureit.data.repository

import com.mohammadsalik.secureit.core.security.EncryptionManager
import com.mohammadsalik.secureit.data.local.database.dao.PasswordDao
import com.mohammadsalik.secureit.data.mapper.PasswordMapper
import com.mohammadsalik.secureit.domain.model.Password
import com.mohammadsalik.secureit.domain.repository.PasswordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordRepositoryImpl @Inject constructor(
    private val passwordDao: PasswordDao
) : PasswordRepository {

    override fun getAllPasswords(): Flow<List<Password>> {
        return passwordDao.getAllPasswords().map { entities ->
            entities.map { entity ->
                PasswordMapper.toDomain(entity).copy(
                    title = EncryptionManager.decryptString(entity.title),
                    username = EncryptionManager.decryptString(entity.username),
                    password = EncryptionManager.decryptString(entity.password),
                    website = EncryptionManager.decryptString(entity.website),
                    notes = EncryptionManager.decryptString(entity.notes),
                    category = EncryptionManager.decryptString(entity.category)
                )
            }
        }
    }

    override suspend fun getPasswordById(id: Long): Password? {
        val entity = passwordDao.getPasswordById(id) ?: return null
        return PasswordMapper.toDomain(entity).copy(
            title = EncryptionManager.decryptString(entity.title),
            username = EncryptionManager.decryptString(entity.username),
            password = EncryptionManager.decryptString(entity.password),
            website = EncryptionManager.decryptString(entity.website),
            notes = EncryptionManager.decryptString(entity.notes),
            category = EncryptionManager.decryptString(entity.category)
        )
    }

    override fun getPasswordsByCategory(category: String): Flow<List<Password>> {
        return passwordDao.getPasswordsByCategory(EncryptionManager.encryptString(category)).map { entities ->
            entities.map { entity ->
                PasswordMapper.toDomain(entity).copy(
                    title = EncryptionManager.decryptString(entity.title),
                    username = EncryptionManager.decryptString(entity.username),
                    password = EncryptionManager.decryptString(entity.password),
                    website = EncryptionManager.decryptString(entity.website),
                    notes = EncryptionManager.decryptString(entity.notes),
                    category = EncryptionManager.decryptString(entity.category)
                )
            }
        }
    }

    override fun getFavoritePasswords(): Flow<List<Password>> {
        return passwordDao.getFavoritePasswords().map { entities ->
            entities.map { entity ->
                PasswordMapper.toDomain(entity).copy(
                    title = EncryptionManager.decryptString(entity.title),
                    username = EncryptionManager.decryptString(entity.username),
                    password = EncryptionManager.decryptString(entity.password),
                    website = EncryptionManager.decryptString(entity.website),
                    notes = EncryptionManager.decryptString(entity.notes),
                    category = EncryptionManager.decryptString(entity.category)
                )
            }
        }
    }

    override fun searchPasswords(query: String): Flow<List<Password>> {
        return passwordDao.getAllPasswords().map { entities ->
            entities.filter { entity ->
                val decryptedTitle = EncryptionManager.decryptString(entity.title)
                val decryptedUsername = EncryptionManager.decryptString(entity.username)
                val decryptedWebsite = EncryptionManager.decryptString(entity.website)
                val decryptedNotes = EncryptionManager.decryptString(entity.notes)
                
                decryptedTitle.contains(query, ignoreCase = true) ||
                decryptedUsername.contains(query, ignoreCase = true) ||
                decryptedWebsite.contains(query, ignoreCase = true) ||
                decryptedNotes.contains(query, ignoreCase = true)
            }.map { entity ->
                PasswordMapper.toDomain(entity).copy(
                    title = EncryptionManager.decryptString(entity.title),
                    username = EncryptionManager.decryptString(entity.username),
                    password = EncryptionManager.decryptString(entity.password),
                    website = EncryptionManager.decryptString(entity.website),
                    notes = EncryptionManager.decryptString(entity.notes),
                    category = EncryptionManager.decryptString(entity.category)
                )
            }
        }
    }

    override fun getAllCategories(): Flow<List<String>> {
        return passwordDao.getAllCategories().map { encryptedCategories ->
            encryptedCategories.map { EncryptionManager.decryptString(it) }
        }
    }

    override suspend fun insertPassword(password: Password): Long {
        val encryptedPassword = password.copy(
            title = EncryptionManager.encryptString(password.title),
            username = EncryptionManager.encryptString(password.username),
            password = EncryptionManager.encryptString(password.password),
            website = EncryptionManager.encryptString(password.website),
            notes = EncryptionManager.encryptString(password.notes),
            category = EncryptionManager.encryptString(password.category)
        )
        val entity = PasswordMapper.toEntity(encryptedPassword)
        return passwordDao.insertPassword(entity)
    }

    override suspend fun updatePassword(password: Password) {
        val encryptedPassword = password.copy(
            title = EncryptionManager.encryptString(password.title),
            username = EncryptionManager.encryptString(password.username),
            password = EncryptionManager.encryptString(password.password),
            website = EncryptionManager.encryptString(password.website),
            notes = EncryptionManager.encryptString(password.notes),
            category = EncryptionManager.encryptString(password.category)
        )
        val entity = PasswordMapper.toEntity(encryptedPassword)
        passwordDao.updatePassword(entity)
    }

    override suspend fun deletePassword(password: Password) {
        passwordDao.deletePassword(PasswordMapper.toEntity(password))
    }

    override suspend fun deletePasswordById(id: Long) {
        passwordDao.deletePasswordById(id)
    }

    override suspend fun updateLastUsed(id: Long, lastUsed: LocalDateTime) {
        passwordDao.updateLastUsed(id, lastUsed)
    }

    override suspend fun getPasswordCount(): Int {
        return passwordDao.getPasswordCount()
    }

    override suspend fun getPasswordCountByCategory(category: String): Int {
        return passwordDao.getPasswordCountByCategory(category)
    }
}