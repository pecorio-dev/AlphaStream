package dev.pecorio.alphastream.data.database

import androidx.room.*
import dev.pecorio.alphastream.data.model.Favorite
import dev.pecorio.alphastream.data.model.FavoriteType
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<Favorite>>
    
    @Query("SELECT * FROM favorites WHERE type = :type ORDER BY addedAt DESC")
    fun getFavoritesByType(type: FavoriteType): Flow<List<Favorite>>
    
    @Query("SELECT * FROM favorites WHERE id = :id LIMIT 1")
    suspend fun getFavoriteById(id: String): Favorite?
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    suspend fun isFavorite(id: String): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    fun isFavoriteFlow(id: String): Flow<Boolean>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite)
    
    @Delete
    suspend fun deleteFavorite(favorite: Favorite)
    
    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavoriteById(id: String)
    
    @Query("DELETE FROM favorites")
    suspend fun deleteAllFavorites()
    
    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getFavoritesCount(): Int
    
    @Query("SELECT COUNT(*) FROM favorites WHERE type = :type")
    suspend fun getFavoritesCountByType(type: FavoriteType): Int
    
    @Query("UPDATE favorites SET lastWatched = :timestamp WHERE id = :id")
    suspend fun updateLastWatched(id: String, timestamp: Long)
    
    @Query("UPDATE favorites SET watchProgress = :progress WHERE id = :id")
    suspend fun updateWatchProgress(id: String, progress: Float)
    
    @Query("UPDATE favorites SET isCompleted = :completed WHERE id = :id")
    suspend fun updateCompleted(id: String, completed: Boolean)
    
    @Query("SELECT * FROM favorites WHERE title LIKE '%' || :query || '%' OR synopsis LIKE '%' || :query || '%' ORDER BY addedAt DESC")
    fun searchFavorites(query: String): Flow<List<Favorite>>
    
    @Query("SELECT * FROM favorites ORDER BY lastWatched DESC LIMIT :limit")
    fun getRecentlyWatched(limit: Int = 10): Flow<List<Favorite>>
    
    @Query("SELECT * FROM favorites WHERE addedAt >= :timestamp ORDER BY addedAt DESC")
    fun getRecentlyAdded(timestamp: Long): Flow<List<Favorite>>
}