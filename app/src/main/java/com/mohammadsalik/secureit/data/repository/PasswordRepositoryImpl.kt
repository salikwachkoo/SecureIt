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
    private val passwordDao: PasswordDao,
    private val encryptionManager: EncryptionManager
) : PasswordRepository {

    override fun getAllPasswords(): Flow<List<Password>> {
        return passwordDao.getAllPasswords().map { entities ->
            PasswordMapper.toDomainList(entities)
        }
    }

    override suspend fun getPasswordById(id: Long): Password? {
        val entity = passwordDao.getPasswordById(id) ?: return null
        return PasswordMapper.toDomain(entity)
    }

    override fun getPasswordsByCategory(category: String): Flow<List<Password>> {
        return passwordDao.getPasswordsByCategory(category).map { entities ->
            PasswordMapper.toDomainList(entities)
        }
    }

    override fun getFavoritePasswords(): Flow<List<Password>> {
        return passwordDao.getFavoritePasswords().map { entities ->
            PasswordMapper.toDomainList(entities)
        }
    }

    override fun searchPasswords(query: String): Flow<List<Password>> {
        return passwordDao.searchPasswords(query).map { entities ->
            PasswordMapper.toDomainList(entities)
        }
    }

    override fun getAllCategories(): Flow<List<String>> {
        return passwordDao.getAllCategories()
    }

    override suspend fun insertPassword(password: Password): Long {
        val entity = PasswordMapper.toEntity(password)
        return passwordDao.insertPassword(entity)
    }

    override suspend fun updatePassword(password: Password) {
        val entity = PasswordMapper.toEntity(password)
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