package com.mohammadsalik.secureit.domain.model

import java.time.LocalDateTime

data class Password(
    val id: Long = 0,
    val title: String,
    val username: String,
    val password: String,
    val website: String,
    val notes: String,
    val category: String,
    val tags: List<String>,
    val isFavorite: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val lastUsed: LocalDateTime? = null
) {
    companion object {
        fun create(
            title: String,
            username: String,
            password: String,
            website: String = "",
            notes: String = "",
            category: String = "General",
            tags: List<String> = emptyList()
        ): Password {
            return Password(
                title = title,
                username = username,
                password = password,
                website = website,
                notes = notes,
                category = category,
                tags = tags
            )
        }
    }

    fun withUpdatedAt(): Password {
        return copy(updatedAt = LocalDateTime.now())
    }

    fun withLastUsed(): Password {
        return copy(lastUsed = LocalDateTime.now())
    }

    fun toggleFavorite(): Password {
        return copy(isFavorite = !isFavorite)
    }

    fun addTag(tag: String): Password {
        return copy(tags = tags + tag)
    }

    fun removeTag(tag: String): Password {
        return copy(tags = tags - tag)
    }
}