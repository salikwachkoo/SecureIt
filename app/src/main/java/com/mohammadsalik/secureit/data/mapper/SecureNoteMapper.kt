package com.mohammadsalik.secureit.data.mapper

import com.mohammadsalik.secureit.data.local.database.entities.SecureNoteEntity
import com.mohammadsalik.secureit.domain.model.SecureNote

object SecureNoteMapper {

    fun toDomain(entity: SecureNoteEntity): SecureNote {
        return SecureNote(
            id = entity.id,
            title = entity.title,
            content = entity.content,
            category = entity.category,
            tags = entity.tags.split(",").filter { it.isNotBlank() },
            isFavorite = entity.isFavorite,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: SecureNote): SecureNoteEntity {
        return SecureNoteEntity(
            id = domain.id,
            title = domain.title,
            content = domain.content,
            category = domain.category,
            tags = domain.tags.joinToString(","),
            isFavorite = domain.isFavorite,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    fun toDomainList(entities: List<SecureNoteEntity>): List<SecureNote> {
        return entities.map { toDomain(it) }
    }

    fun toEntityList(domains: List<SecureNote>): List<SecureNoteEntity> {
        return domains.map { toEntity(it) }
    }
}