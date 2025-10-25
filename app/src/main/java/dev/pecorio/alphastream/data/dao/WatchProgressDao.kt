package dev.pecorio.alphastream.data.dao

import androidx.room.*
import dev.pecorio.alphastream.data.model.WatchProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {
    
    @Query("SELECT * FROM watch_progress WHERE id = :id")
    suspend fun getProgress(id: String): WatchProgress?
    
    @Query("SELECT * FROM watch_progress WHERE contentType = 'movie' AND movieId = :movieId")
    suspend fun getMovieProgress(movieId: String): WatchProgress?
    
    @Query("SELECT * FROM watch_progress WHERE contentType = 'episode' AND seriesId = :seriesId ORDER BY seasonNumber DESC, episodeNumber DESC LIMIT 1")
    suspend fun getLastWatchedEpisode(seriesId: String): WatchProgress?
    
    @Query("SELECT * FROM watch_progress WHERE contentType = 'episode' AND seriesId = :seriesId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber")
    suspend fun getEpisodeProgress(seriesId: String, seasonNumber: Int, episodeNumber: Int): WatchProgress?
    
    @Query("SELECT * FROM watch_progress WHERE lastWatched > :since ORDER BY lastWatched DESC")
    fun getRecentProgress(since: Long): Flow<List<WatchProgress>>
    
    @Query("SELECT * FROM watch_progress WHERE progressPercent > 0.05 AND progressPercent < 0.9 ORDER BY lastWatched DESC LIMIT 10")
    fun getContinueWatching(): Flow<List<WatchProgress>>
    
    @Query("SELECT * FROM watch_progress WHERE contentType = 'episode' AND seriesId = :seriesId ORDER BY seasonNumber ASC, episodeNumber ASC")
    suspend fun getSeriesProgress(seriesId: String): List<WatchProgress>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: WatchProgress)
    
    @Update
    suspend fun updateProgress(progress: WatchProgress)
    
    @Delete
    suspend fun deleteProgress(progress: WatchProgress)
    
    @Query("DELETE FROM watch_progress WHERE id = :id")
    suspend fun deleteProgressById(id: String)
    
    @Query("DELETE FROM watch_progress WHERE contentType = 'episode' AND seriesId = :seriesId")
    suspend fun deleteSeriesProgress(seriesId: String)
    
    @Query("DELETE FROM watch_progress WHERE lastWatched < :before")
    suspend fun deleteOldProgress(before: Long)
    
    @Query("UPDATE watch_progress SET isCompleted = 1 WHERE id = :id")
    suspend fun markAsCompleted(id: String)
    
    @Query("SELECT COUNT(*) FROM watch_progress WHERE contentType = 'episode' AND seriesId = :seriesId AND isCompleted = 1")
    suspend fun getCompletedEpisodesCount(seriesId: String): Int
}