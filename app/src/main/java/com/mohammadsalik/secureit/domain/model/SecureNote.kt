package com.mohammadsalik.secureit.domain.model

import java.time.LocalDateTime

data class SecureNote(
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String,
    val tags: List<String>,
    val isFavorite: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun create(
            title: String,
            content: String,
            category: String = "General",
            tags: List<String> = emptyList()
        ): SecureNote {
            return SecureNote(
                title = title,
                content = content,
                category = category,
                tags = tags
            )
        }
    }

    fun withUpdatedAt(): SecureNote {
        return copy(updatedAt = LocalDateTime.now())
    }

    fun toggleFavorite(): SecureNote {
        return copy(isFavorite = !isFavorite)
    }

    fun addTag(tag: String): SecureNote {
        return copy(tags = tags + tag)
    }

    fun removeTag(tag: String): SecureNote {
        return copy(tags = tags - tag)
    }

    fun getPreview(): String {
        return content.take(100).let {
            if (content.length > 100) "$it..." else it
        }
    }
}