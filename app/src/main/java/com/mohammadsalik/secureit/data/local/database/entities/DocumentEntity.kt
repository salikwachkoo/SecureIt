package com.mohammadsalik.secureit.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val fileName: String,
    val filePath: String, // Path to encrypted file
    val fileSize: Long,
    val mimeType: String,
    val category: String,
    val tags: String, // Comma-separated tags
    val isFavorite: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val lastAccessed: LocalDateTime? = null
) 