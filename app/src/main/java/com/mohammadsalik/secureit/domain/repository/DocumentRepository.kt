package com.mohammadsalik.secureit.domain.repository

import com.mohammadsalik.secureit.domain.model.Document
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface DocumentRepository {

    fun getAllDocuments(): Flow<List<Document>>

    suspend fun getDocumentById(id: Long): Document?

    fun getDocumentsByCategory(category: String): Flow<List<Document>>

    fun getFavoriteDocuments(): Flow<List<Document>>

    fun searchDocuments(query: String): Flow<List<Document>>

    fun getAllCategories(): Flow<List<String>>

    fun getAllMimeTypes(): Flow<List<String>>

    suspend fun insertDocument(document: Document): Long

    suspend fun updateDocument(document: Document)

    suspend fun deleteDocument(document: Document)

    suspend fun deleteDocumentById(id: Long)

    suspend fun updateLastAccessed(id: Long, lastAccessed: LocalDateTime)

    suspend fun getDocumentCount(): Int

    suspend fun getDocumentCountByCategory(category: String): Int

    suspend fun getTotalStorageUsed(): Long?
}
