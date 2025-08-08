package com.mohammadsalik.secureit.data.mapper

import com.mohammadsalik.secureit.data.local.database.entities.PasswordEntity
import com.mohammadsalik.secureit.domain.model.Password

object PasswordMapper {

    fun toDomain(entity: PasswordEntity): Password {
        return Password(
            id = entity.id,
            title = entity.title,
            username = entity.username,
            password = entity.password,
            website = entity.website,
            notes = entity.notes,
            category = entity.category,
            tags = entity.tags.split(",").filter { it.isNotBlank() },
            isFavorite = entity.isFavorite,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            lastUsed = entity.lastUsed
        )
    }

    fun toEntity(domain: Password): PasswordEntity {
        return PasswordEntity(
            id = domain.id,
            title = domain.title,
            username = domain.username,
            password = domain.password,
            website = domain.website,
            notes = domain.notes,
            category = domain.category,
            tags = domain.tags.joinToString(","),
            isFavorite = domain.isFavorite,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            lastUsed = domain.lastUsed
        )
    }

    fun toDomainList(entities: List<PasswordEntity>): List<Password> {
        return entities.map { toDomain(it) }
    }

    fun toEntityList(domains: List<Password>): List<PasswordEntity> {
        return domains.map { toEntity(it) }
    }
}