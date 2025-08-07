package com.mohammadsalik.secureit.data.local.database.dao

import androidx.room.*
import com.mohammadsalik.secureit.data.local.database.entities.PasswordEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface PasswordDao {

    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC")
    fun getAllPasswords(): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords WHERE id = :id")
    suspend fun getPasswordById(id: Long): PasswordEntity?

    @Query("SELECT * FROM passwords WHERE category = :category ORDER BY updatedAt DESC")
    fun getPasswordsByCategory(category: String): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoritePasswords(): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords WHERE title LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR website LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchPasswords(query: String): Flow<List<PasswordEntity>>

    @Query("SELECT DISTINCT category FROM passwords ORDER BY category")
    fun getAllCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(password: PasswordEntity): Long

    @Update
    suspend fun updatePassword(password: PasswordEntity)

    @Delete
    suspend fun deletePassword(password: PasswordEntity)

    @Query("DELETE FROM passwords WHERE id = :id")
    suspend fun deletePasswordById(id: Long)

    @Query("UPDATE passwords SET lastUsed = :lastUsed WHERE id = :id")
    suspend fun updateLastUsed(id: Long, lastUsed: LocalDateTime)

    @Query("SELECT COUNT(*) FROM passwords")
    suspend fun getPasswordCount(): Int

    @Query("SELECT COUNT(*) FROM passwords WHERE category = :category")
    suspend fun getPasswordCountByCategory(category: String): Int
}