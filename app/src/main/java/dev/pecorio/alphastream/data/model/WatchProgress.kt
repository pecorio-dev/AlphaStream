package dev.pecorio.alphastream.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_progress")
data class WatchProgress(
    @PrimaryKey
    val id: String, // Pour films: movie_id, pour séries: series_id_season_episode
    val contentType: String, // "movie" ou "episode"
    val contentId: String, // ID du film ou de la série
    val title: String,
    val subtitle: String? = null, // Pour les épisodes: "S1E1 - Titre épisode"
    val currentPosition: Long, // Position en millisecondes
    val duration: Long, // Durée totale en millisecondes
    val progressPercent: Float, // Pourcentage de progression (0.0 à 1.0)
    val lastWatched: Long, // Timestamp de la dernière lecture
    val isCompleted: Boolean = false, // Marqué comme terminé si > 90%
    
    // Pour les séries
    val seriesId: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    
    // Pour les films
    val movieId: String? = null,
    
    // Métadonnées
    val imageUrl: String? = null,
    val streamUrl: String? = null
) {
    companion object {
        fun createForMovie(
            movieId: String,
            title: String,
            currentPosition: Long,
            duration: Long,
            imageUrl: String? = null,
            streamUrl: String? = null
        ): WatchProgress {
            val progressPercent = if (duration > 0) currentPosition.toFloat() / duration else 0f
            return WatchProgress(
                id = "movie_$movieId",
                contentType = "movie",
                contentId = movieId,
                title = title,
                subtitle = null,
                currentPosition = currentPosition,
                duration = duration,
                progressPercent = progressPercent,
                lastWatched = System.currentTimeMillis(),
                isCompleted = progressPercent >= 0.9f,
                movieId = movieId,
                imageUrl = imageUrl,
                streamUrl = streamUrl
            )
        }
        
        fun createForEpisode(
            seriesId: String,
            seasonNumber: Int,
            episodeNumber: Int,
            seriesTitle: String,
            episodeTitle: String,
            currentPosition: Long,
            duration: Long,
            imageUrl: String? = null,
            streamUrl: String? = null
        ): WatchProgress {
            val progressPercent = if (duration > 0) currentPosition.toFloat() / duration else 0f
            return WatchProgress(
                id = "episode_${seriesId}_${seasonNumber}_${episodeNumber}",
                contentType = "episode",
                contentId = seriesId,
                title = seriesTitle,
                subtitle = "S${seasonNumber}E${episodeNumber} - $episodeTitle",
                currentPosition = currentPosition,
                duration = duration,
                progressPercent = progressPercent,
                lastWatched = System.currentTimeMillis(),
                isCompleted = progressPercent >= 0.9f,
                seriesId = seriesId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                imageUrl = imageUrl,
                streamUrl = streamUrl
            )
        }
    }
    
    /**
     * Retourne true si la progression est significative (> 5% et < 90%)
     */
    fun hasSignificantProgress(): Boolean {
        return progressPercent > 0.05f && progressPercent < 0.9f
    }
    
    /**
     * Retourne le temps formaté de la position actuelle
     */
    fun getFormattedCurrentPosition(): String {
        return formatTime(currentPosition)
    }
    
    /**
     * Retourne le temps formaté de la durée totale
     */
    fun getFormattedDuration(): String {
        return formatTime(duration)
    }
    
    /**
     * Retourne le temps restant formaté
     */
    fun getFormattedTimeRemaining(): String {
        return formatTime(duration - currentPosition)
    }
    
    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}