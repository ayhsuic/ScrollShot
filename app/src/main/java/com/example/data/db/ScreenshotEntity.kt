package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_screenshots")
data class ScreenshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val width: Int,
    val height: Int,
    val sliceCount: Int,
    val fileSizeBytes: Long,
    val captureType: String, // "SCROLL_CAPTURE", "STITCHED", "WEB_CAPTURE", "MOCKUP"
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
