package com.mohammadsalik.secureit.domain.repository

import com.mohammadsalik.secureit.domain.model.SecureNote
import kotlinx.coroutines.flow.Flow

interface SecureNoteRepository {

    fun getAllNotes(): Flow<List<SecureNote>>

    suspend fun getNoteById(id: Long): SecureNote?

    fun getNotesByCategory(category: String): Flow<List<SecureNote>>

    fun getFavoriteNotes(): Flow<List<SecureNote>>

    fun searchNotes(query: String): Flow<List<SecureNote>>

    fun getAllCategories(): Flow<List<String>>

    suspend fun insertNote(note: SecureNote): Long

    suspend fun updateNote(note: SecureNote)

    suspend fun deleteNote(note: SecureNote)

    suspend fun deleteNoteById(id: Long)

    suspend fun getNoteCount(): Int

    suspend fun getNoteCountByCategory(category: String): Int
}