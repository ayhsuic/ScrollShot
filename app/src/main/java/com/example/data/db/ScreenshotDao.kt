package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotDao {
    @Query("SELECT * FROM saved_screenshots ORDER BY timestamp DESC")
    fun getAllScreenshots(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM saved_screenshots WHERE id = :id")
    suspend fun getScreenshotById(id: Long): ScreenshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreenshot(screenshot: ScreenshotEntity): Long

    @Update
    suspend fun updateScreenshot(screenshot: ScreenshotEntity)

    @Delete
    suspend fun deleteScreenshot(screenshot: ScreenshotEntity)

    @Query("DELETE FROM saved_screenshots WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM saved_screenshots")
    suspend fun deleteAll()
}
