package com.mohammadsalik.secureit.data.mapper

import com.mohammadsalik.secureit.data.local.database.entities.DocumentEntity
import com.mohammadsalik.secureit.domain.model.Document

object DocumentMapper {

    fun toDomain(entity: DocumentEntity): Document {
        return Document(
            id = entity.id,
            title = entity.title,
            fileName = entity.fileName,
            filePath = entity.filePath,
            fileSize = entity.fileSize,
            mimeType = entity.mimeType,
            category = entity.category,
            tags = entity.tags.split(",").filter { it.isNotBlank() },
            isFavorite = entity.isFavorite,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            lastAccessed = entity.lastAccessed
        )
    }

    fun toEntity(domain: Document): DocumentEntity {
        return DocumentEntity(
            id = domain.id,
            title = domain.title,
            fileName = domain.fileName,
            filePath = domain.filePath,
            fileSize = domain.fileSize,
            mimeType = domain.mimeType,
            category = domain.category,
            tags = domain.tags.joinToString(","),
            isFavorite = domain.isFavorite,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            lastAccessed = domain.lastAccessed
        )
    }

    fun toDomainList(entities: List<DocumentEntity>): List<Document> {
        return entities.map { toDomain(it) }
    }

    fun toEntityList(domains: List<Document>): List<DocumentEntity> {
        return domains.map { toEntity(it) }
    }
}