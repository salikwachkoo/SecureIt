package com.mohammadsalik.secureit.presentation.documents

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*

@Composable
fun getDocumentIcon(mimeType: String): ImageVector {
    return when {
        mimeType.startsWith("image/") -> Icons.Default.Image
        mimeType.startsWith("video/") -> Icons.Default.VideoFile
        mimeType.startsWith("audio/") -> Icons.Default.AudioFile
        mimeType.contains("pdf") -> Icons.Default.PictureAsPdf
        mimeType.contains("word") || mimeType.contains("document") -> Icons.Default.Description
        mimeType.contains("excel") || mimeType.contains("spreadsheet") -> Icons.Default.TableChart
        mimeType.contains("powerpoint") || mimeType.contains("presentation") -> Icons.Default.Slideshow
        mimeType.contains("text/") -> Icons.Default.TextSnippet
        else -> Icons.Default.InsertDriveFile
    }
}

fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> "${size / (1024 * 1024 * 1024)} GB"
    }
}

fun formatDate(date: LocalDateTime?): String {
    if (date == null) return "Unknown"
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(java.util.Date.from(date.atZone(java.time.ZoneId.systemDefault()).toInstant()))
}
