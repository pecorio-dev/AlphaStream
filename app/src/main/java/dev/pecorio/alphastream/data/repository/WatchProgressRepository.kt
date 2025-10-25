package dev.pecorio.alphastream.data.repository

import dev.pecorio.alphastream.data.dao.WatchProgressDao
import dev.pecorio.alphastream.data.model.WatchProgress
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchProgressRepository @Inject constructor(
    private val watchProgressDao: WatchProgressDao
) {
    
    /**
     * Sauvegarde la progression d'un film
     */
    suspend fun saveMovieProgress(
        movieId: String,
        title: String,
        currentPosition: Long,
        duration: Long,
        imageUrl: String? = null,
        streamUrl: String? = null
    ) {
        val progress = WatchProgress.createForMovie(
            movieId = movieId,
            title = title,
            currentPosition = currentPosition,
            duration = duration,
            imageUrl = imageUrl,
            streamUrl = streamUrl
        )
        watchProgressDao.saveProgress(progress)
        
        android.util.Log.d("WatchProgress", "Saved movie progress: $movieId at ${progress.getFormattedCurrentPosition()}")
    }
    
    /**
     * Sauvegarde la progression d'un épisode
     */
    suspend fun saveEpisodeProgress(
        seriesId: String,
        seasonNumber: Int,
        episodeNumber: Int,
        seriesTitle: String,
        episodeTitle: String,
        currentPosition: Long,
        duration: Long,
        imageUrl: String? = null,
        streamUrl: String? = null
    ) {
        val progress = WatchProgress.createForEpisode(
            seriesId = seriesId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            seriesTitle = seriesTitle,
            episodeTitle = episodeTitle,
            currentPosition = currentPosition,
            duration = duration,
            imageUrl = imageUrl,
            streamUrl = streamUrl
        )
        watchProgressDao.saveProgress(progress)
        
        android.util.Log.d("WatchProgress", "Saved episode progress: $seriesId S${seasonNumber}E${episodeNumber} at ${progress.getFormattedCurrentPosition()}")
    }
    
    /**
     * Récupère la progression d'un film
     */
    suspend fun getMovieProgress(movieId: String): WatchProgress? {
        return watchProgressDao.getMovieProgress(movieId)
    }
    
    /**
     * Récupère la progression d'un épisode
     */
    suspend fun getEpisodeProgress(seriesId: String, seasonNumber: Int, episodeNumber: Int): WatchProgress? {
        return watchProgressDao.getEpisodeProgress(seriesId, seasonNumber, episodeNumber)
    }
    
    /**
     * Récupère le dernier épisode regardé d'une série
     */
    suspend fun getLastWatchedEpisode(seriesId: String): WatchProgress? {
        return watchProgressDao.getLastWatchedEpisode(seriesId)
    }
    
    /**
     * Récupère la liste "Continuer à regarder"
     */
    fun getContinueWatching(): Flow<List<WatchProgress>> {
        return watchProgressDao.getContinueWatching()
    }
    
    /**
     * Récupère les progressions récentes
     */
    fun getRecentProgress(daysBack: Int = 7): Flow<List<WatchProgress>> {
        val since = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L)
        return watchProgressDao.getRecentProgress(since)
    }
    
    /**
     * Récupère toute la progression d'une série
     */
    suspend fun getSeriesProgress(seriesId: String): List<WatchProgress> {
        return watchProgressDao.getSeriesProgress(seriesId)
    }
    
    /**
     * Marque un contenu comme terminé
     */
    suspend fun markAsCompleted(contentId: String) {
        watchProgressDao.markAsCompleted(contentId)
    }
    
    /**
     * Supprime la progression d'un contenu
     */
    suspend fun deleteProgress(contentId: String) {
        watchProgressDao.deleteProgressById(contentId)
    }
    
    /**
     * Supprime toute la progression d'une série
     */
    suspend fun deleteSeriesProgress(seriesId: String) {
        watchProgressDao.deleteSeriesProgress(seriesId)
    }
    
    /**
     * Nettoie les anciennes progressions (plus de 30 jours)
     */
    suspend fun cleanOldProgress() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        watchProgressDao.deleteOldProgress(thirtyDaysAgo)
    }
    
    /**
     * Vérifie s'il y a une progression significative pour un film
     */
    suspend fun hasSignificantMovieProgress(movieId: String): Boolean {
        val progress = getMovieProgress(movieId)
        return progress?.hasSignificantProgress() == true
    }
    
    /**
     * Vérifie s'il y a une progression significative pour un épisode
     */
    suspend fun hasSignificantEpisodeProgress(seriesId: String, seasonNumber: Int, episodeNumber: Int): Boolean {
        val progress = getEpisodeProgress(seriesId, seasonNumber, episodeNumber)
        return progress?.hasSignificantProgress() == true
    }
    
    /**
     * Récupère le nombre d'épisodes terminés pour une série
     */
    suspend fun getCompletedEpisodesCount(seriesId: String): Int {
        return watchProgressDao.getCompletedEpisodesCount(seriesId)
    }
}