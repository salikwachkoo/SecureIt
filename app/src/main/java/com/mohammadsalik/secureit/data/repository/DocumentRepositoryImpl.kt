package com.mohammadsalik.secureit.data.repository

import com.mohammadsalik.secureit.core.security.EncryptionManager
import com.mohammadsalik.secureit.data.local.database.dao.DocumentDao
import com.mohammadsalik.secureit.data.mapper.DocumentMapper
import com.mohammadsalik.secureit.domain.model.Document
import com.mohammadsalik.secureit.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val documentDao: DocumentDao
) : DocumentRepository {
    
    override fun getAllDocuments(): Flow<List<Document>> {
        return documentDao.getAllDocuments().map { entities ->
            DocumentMapper.toDomainList(entities)
        }
    }
    
    override suspend fun getDocumentById(id: Long): Document? {
        val entity = documentDao.getDocumentById(id) ?: return null
        return DocumentMapper.toDomain(entity)
    }
    
    override fun getDocumentsByCategory(category: String): Flow<List<Document>> {
        return documentDao.getDocumentsByCategory(category).map { entities ->
            DocumentMapper.toDomainList(entities)
        }
    }
    
    override fun getFavoriteDocuments(): Flow<List<Document>> {
        return documentDao.getFavoriteDocuments().map { entities ->
            DocumentMapper.toDomainList(entities)
        }
    }
    
    override fun searchDocuments(query: String): Flow<List<Document>> {
        return documentDao.searchDocuments(query).map { entities ->
            DocumentMapper.toDomainList(entities)
        }
    }
    
    override fun getAllCategories(): Flow<List<String>> {
        return documentDao.getAllCategories()
    }
    
    override fun getAllMimeTypes(): Flow<List<String>> {
        return documentDao.getAllMimeTypes()
    }
    
    override suspend fun insertDocument(document: Document): Long {
        val entity = DocumentMapper.toEntity(document)
        return documentDao.insertDocument(entity)
    }
    
    override suspend fun updateDocument(document: Document) {
        val entity = DocumentMapper.toEntity(document)
        documentDao.updateDocument(entity)
    }
    
    override suspend fun deleteDocument(document: Document) {
        documentDao.deleteDocument(DocumentMapper.toEntity(document))
    }
    
    override suspend fun deleteDocumentById(id: Long) {
        documentDao.deleteDocumentById(id)
    }
    
    override suspend fun updateLastAccessed(id: Long, lastAccessed: LocalDateTime) {
        documentDao.updateLastAccessed(id, lastAccessed)
    }
    
    override suspend fun getDocumentCount(): Int {
        return documentDao.getDocumentCount()
    }
    
    override suspend fun getDocumentCountByCategory(category: String): Int {
        return documentDao.getDocumentCountByCategory(category)
    }
    
    override suspend fun getTotalStorageUsed(): Long? {
        return documentDao.getTotalStorageUsed()
    }
}