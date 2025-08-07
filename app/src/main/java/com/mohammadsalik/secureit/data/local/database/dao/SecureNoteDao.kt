package com.mohammadsalik.secureit.data.local.database.dao

import androidx.room.*
import com.mohammadsalik.secureit.data.local.database.entities.SecureNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SecureNoteDao {
    
    @Query("SELECT * FROM secure_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<SecureNoteEntity>>
    
    @Query("SELECT * FROM secure_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): SecureNoteEntity?
    
    @Query("SELECT * FROM secure_notes WHERE category = :category ORDER BY updatedAt DESC")
    fun getNotesByCategory(category: String): Flow<List<SecureNoteEntity>>
    
    @Query("SELECT * FROM secure_notes WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<SecureNoteEntity>>
    
    @Query("SELECT * FROM secure_notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchNotes(query: String): Flow<List<SecureNoteEntity>>
    
    @Query("SELECT DISTINCT category FROM secure_notes ORDER BY category")
    fun getAllCategories(): Flow<List<String>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: SecureNoteEntity): Long
    
    @Update
    suspend fun updateNote(note: SecureNoteEntity)
    
    @Delete
    suspend fun deleteNote(note: SecureNoteEntity)
    
    @Query("DELETE FROM secure_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
    
    @Query("SELECT COUNT(*) FROM secure_notes")
    suspend fun getNoteCount(): Int
    
    @Query("SELECT COUNT(*) FROM secure_notes WHERE category = :category")
    suspend fun getNoteCountByCategory(category: String): Int
}