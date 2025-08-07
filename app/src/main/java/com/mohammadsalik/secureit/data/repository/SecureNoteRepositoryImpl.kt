package com.mohammadsalik.secureit.data.repository

import com.mohammadsalik.secureit.core.security.EncryptionManager
import com.mohammadsalik.secureit.data.local.database.dao.SecureNoteDao
import com.mohammadsalik.secureit.data.mapper.SecureNoteMapper
import com.mohammadsalik.secureit.domain.model.SecureNote
import com.mohammadsalik.secureit.domain.repository.SecureNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class SecureNoteRepositoryImpl @Inject constructor(
    private val secureNoteDao: SecureNoteDao,
    private val encryptionManager: EncryptionManager
) : SecureNoteRepository {

    override fun getAllNotes(): Flow<List<SecureNote>> {
        return secureNoteDao.getAllNotes().map { entities ->
            SecureNoteMapper.toDomainList(entities)
        }
    }

    override suspend fun getNoteById(id: Long): SecureNote? {
        val entity = secureNoteDao.getNoteById(id) ?: return null
        return SecureNoteMapper.toDomain(entity)
    }

    override fun getNotesByCategory(category: String): Flow<List<SecureNote>> {
        return secureNoteDao.getNotesByCategory(category).map { entities ->
            SecureNoteMapper.toDomainList(entities)
        }
    }

    override fun getFavoriteNotes(): Flow<List<SecureNote>> {
        return secureNoteDao.getFavoriteNotes().map { entities ->
            SecureNoteMapper.toDomainList(entities)
        }
    }

    override fun searchNotes(query: String): Flow<List<SecureNote>> {
        return secureNoteDao.searchNotes(query).map { entities ->
            SecureNoteMapper.toDomainList(entities)
        }
    }

    override fun getAllCategories(): Flow<List<String>> {
        return secureNoteDao.getAllCategories()
    }

    override suspend fun insertNote(note: SecureNote): Long {
        val entity = SecureNoteMapper.toEntity(note)
        return secureNoteDao.insertNote(entity)
    }

    override suspend fun updateNote(note: SecureNote) {
        val entity = SecureNoteMapper.toEntity(note)
        secureNoteDao.updateNote(entity)
    }

    override suspend fun deleteNote(note: SecureNote) {
        secureNoteDao.deleteNote(SecureNoteMapper.toEntity(note))
    }

    override suspend fun deleteNoteById(id: Long) {
        secureNoteDao.deleteNoteById(id)
    }

    override suspend fun getNoteCount(): Int {
        return secureNoteDao.getNoteCount()
    }

    override suspend fun getNoteCountByCategory(category: String): Int {
        return secureNoteDao.getNoteCountByCategory(category)
    }
}