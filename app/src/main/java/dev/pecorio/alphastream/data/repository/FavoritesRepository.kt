package dev.pecorio.alphastream.data.repository

import dev.pecorio.alphastream.data.database.FavoriteDao
import dev.pecorio.alphastream.data.model.Favorite
import dev.pecorio.alphastream.data.model.FavoriteType
import dev.pecorio.alphastream.data.model.Movie
import dev.pecorio.alphastream.data.model.Series
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val favoriteDao: FavoriteDao
) {
    
    /**
     * Get all favorites
     */
    fun getAllFavorites(): Flow<List<Favorite>> = favoriteDao.getAllFavorites()
    
    /**
     * Get favorites by type
     */
    fun getFavoritesByType(type: FavoriteType): Flow<List<Favorite>> = 
        favoriteDao.getFavoritesByType(type)
    
    /**
     * Get movies favorites
     */
    fun getFavoriteMovies(): Flow<List<Favorite>> = 
        favoriteDao.getFavoritesByType(FavoriteType.MOVIE)
    
    /**
     * Get series favorites
     */
    fun getFavoriteSeries(): Flow<List<Favorite>> = 
        favoriteDao.getFavoritesByType(FavoriteType.SERIES)
    
    /**
     * Check if item is favorite
     */
    suspend fun isFavorite(id: String): Boolean = withContext(Dispatchers.IO) {
        favoriteDao.isFavorite(id)
    }
    
    /**
     * Check if item is favorite (Flow)
     */
    fun isFavoriteFlow(id: String): Flow<Boolean> = favoriteDao.isFavoriteFlow(id)
    
    /**
     * Add movie to favorites
     */
    suspend fun addMovieToFavorites(movie: Movie): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val favorite = Favorite.fromMovie(movie)
            favoriteDao.insertFavorite(favorite)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add series to favorites
     */
    suspend fun addSeriesToFavorites(series: Series): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val favorite = Favorite.fromSeries(series)
            favoriteDao.insertFavorite(favorite)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove from favorites by ID
     */
    suspend fun removeFromFavorites(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            favoriteDao.deleteFavoriteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Toggle favorite status
     */
    suspend fun toggleFavorite(movie: Movie): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val id = movie.title.hashCode().toString()
            val isFav = favoriteDao.isFavorite(id)
            
            if (isFav) {
                favoriteDao.deleteFavoriteById(id)
                Result.success(false)
            } else {
                val favorite = Favorite.fromMovie(movie)
                favoriteDao.insertFavorite(favorite)
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Toggle favorite status for series
     */
    suspend fun toggleFavoriteSeries(series: Series): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val id = series.getSeriesId()
            val isFav = favoriteDao.isFavorite(id)
            
            if (isFav) {
                favoriteDao.deleteFavoriteById(id)
                Result.success(false)
            } else {
                val favorite = Favorite.fromSeries(series)
                favoriteDao.insertFavorite(favorite)
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get favorite by ID
     */
    suspend fun getFavoriteById(id: String): Favorite? = withContext(Dispatchers.IO) {
        favoriteDao.getFavoriteById(id)
    }
    
    /**
     * Search favorites
     */
    fun searchFavorites(query: String): Flow<List<Favorite>> = 
        favoriteDao.searchFavorites(query)
    
    /**
     * Get recently watched
     */
    fun getRecentlyWatched(limit: Int = 10): Flow<List<Favorite>> = 
        favoriteDao.getRecentlyWatched(limit)
    
    /**
     * Get recently added
     */
    fun getRecentlyAdded(daysAgo: Int = 7): Flow<List<Favorite>> {
        val timestamp = System.currentTimeMillis() - (daysAgo * 24 * 60 * 60 * 1000L)
        return favoriteDao.getRecentlyAdded(timestamp)
    }
    
    /**
     * Update watch progress
     */
    suspend fun updateWatchProgress(id: String, progress: Float): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            favoriteDao.updateWatchProgress(id, progress)
            favoriteDao.updateLastWatched(id, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark as completed
     */
    suspend fun markAsCompleted(id: String, completed: Boolean = true): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            favoriteDao.updateCompleted(id, completed)
            if (completed) {
                favoriteDao.updateWatchProgress(id, 1.0f)
            }
            favoriteDao.updateLastWatched(id, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get favorites count
     */
    suspend fun getFavoritesCount(): Int = withContext(Dispatchers.IO) {
        favoriteDao.getFavoritesCount()
    }
    
    /**
     * Get favorites count by type
     */
    suspend fun getFavoritesCountByType(type: FavoriteType): Int = withContext(Dispatchers.IO) {
        favoriteDao.getFavoritesCountByType(type)
    }
    
    /**
     * Clear all favorites
     */
    suspend fun clearAllFavorites(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            favoriteDao.deleteAllFavorites()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}