package com.mohammadsalik.secureit.data.local.database.dao

import androidx.room.*
import com.mohammadsalik.secureit.data.local.database.entities.DocumentEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE category = :category ORDER BY updatedAt DESC")
    fun getDocumentsByCategory(category: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE title LIKE '%' || :query || '%' OR fileName LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Query("SELECT DISTINCT category FROM documents ORDER BY category")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT mimeType FROM documents ORDER BY mimeType")
    fun getAllMimeTypes(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    @Query("UPDATE documents SET lastAccessed = :lastAccessed WHERE id = :id")
    suspend fun updateLastAccessed(id: Long, lastAccessed: LocalDateTime)

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun getDocumentCount(): Int

    @Query("SELECT COUNT(*) FROM documents WHERE category = :category")
    suspend fun getDocumentCountByCategory(category: String): Int

    @Query("SELECT SUM(fileSize) FROM documents")
    suspend fun getTotalStorageUsed(): Long?
}