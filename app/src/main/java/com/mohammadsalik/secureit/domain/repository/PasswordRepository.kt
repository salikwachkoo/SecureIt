package com.mohammadsalik.secureit.domain.repository

import com.mohammadsalik.secureit.domain.model.Password
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface PasswordRepository {

    fun getAllPasswords(): Flow<List<Password>>

    suspend fun getPasswordById(id: Long): Password?

    fun getPasswordsByCategory(category: String): Flow<List<Password>>

    fun getFavoritePasswords(): Flow<List<Password>>

    fun searchPasswords(query: String): Flow<List<Password>>

    fun getAllCategories(): Flow<List<String>>

    suspend fun insertPassword(password: Password): Long

    suspend fun updatePassword(password: Password)

    suspend fun deletePassword(password: Password)

    suspend fun deletePasswordById(id: Long)

    suspend fun updateLastUsed(id: Long, lastUsed: LocalDateTime)

    suspend fun getPasswordCount(): Int

    suspend fun getPasswordCountByCategory(category: String): Int
}