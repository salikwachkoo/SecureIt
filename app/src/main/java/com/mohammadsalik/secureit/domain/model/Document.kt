package com.mohammadsalik.secureit.domain.model

import java.time.LocalDateTime

data class Document(
    val id: Long = 0,
    val title: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String,
    val category: String,
    val tags: List<String>,
    val isFavorite: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val lastAccessed: LocalDateTime? = null
) {
    companion object {
        fun create(
            title: String,
            fileName: String,
            filePath: String,
            fileSize: Long,
            mimeType: String,
            category: String = "General",
            tags: List<String> = emptyList()
        ): Document {
            return Document(
                title = title,
                fileName = fileName,
                filePath = filePath,
                fileSize = fileSize,
                mimeType = mimeType,
                category = category,
                tags = tags
            )
        }
    }

    fun withUpdatedAt(): Document {
        return copy(updatedAt = LocalDateTime.now())
    }

    fun withLastAccessed(): Document {
        return copy(lastAccessed = LocalDateTime.now())
    }

    fun toggleFavorite(): Document {
        return copy(isFavorite = !isFavorite)
    }

    fun addTag(tag: String): Document {
        return copy(tags = tags + tag)
    }

    fun removeTag(tag: String): Document {
        return copy(tags = tags - tag)
    }

    fun getFileSizeFormatted(): String {
        return when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            fileSize < 1024 * 1024 * 1024 -> "${fileSize / (1024 * 1024)} MB"
            else -> "${fileSize / (1024 * 1024 * 1024)} GB"
        }
    }

    fun isImage(): Boolean {
        return mimeType.startsWith("image/")
    }

    fun isVideo(): Boolean {
        return mimeType.startsWith("video/")
    }

    fun isAudio(): Boolean {
        return mimeType.startsWith("audio/")
    }

    fun isPdf(): Boolean {
        return mimeType == "application/pdf"
    }

    fun isText(): Boolean {
        return mimeType.startsWith("text/")
    }
}