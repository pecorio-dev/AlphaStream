package dev.pecorio.alphastream.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.pecorio.alphastream.utils.ImageUrlHelper
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey
    val id: String,
    val type: FavoriteType,
    val title: String,
    val imageUrl: String?,
    val remoteImageUrl: String?,
    val synopsis: String?,
    val genres: String?, // JSON string of genres list
    val rating: Double?,
    val releaseDate: String?,
    val addedAt: Long = System.currentTimeMillis(),
    
    // Movie specific fields
    val duration: String? = null,
    val director: String? = null,
    
    // Series specific fields
    val status: String? = null,
    val seasonsCount: Int? = null,
    val episodesCount: Int? = null,
    val network: String? = null,
    
    // Additional metadata
    val lastWatched: Long? = null,
    val watchProgress: Float = 0f, // 0.0 to 1.0
    val isCompleted: Boolean = false
) : Parcelable {
    
    fun getDisplayImageUrl(): String? = ImageUrlHelper.getOptimalImageUrl(imageUrl, remoteImageUrl)
    
    fun getFormattedGenres(): List<String>? {
        return try {
            genres?.let { 
                com.google.gson.Gson().fromJson(it, Array<String>::class.java).toList()
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun getGenresString(): String? = getFormattedGenres()?.joinToString(", ")
    
    fun getDisplayRating(): String? = rating?.let { String.format("%.1f", it) }
    
    fun getDisplayYear(): String? = releaseDate?.let { date ->
        if (date.length >= 4) date.substring(0, 4) else date
    }
    
    fun getDisplayDescription(): String {
        val parts = mutableListOf<String>()
        
        when (type) {
            FavoriteType.MOVIE -> {
                getDisplayYear()?.let { parts.add(it) }
                duration?.let { parts.add(it) }
                getDisplayRating()?.let { parts.add("★ $it") }
            }
            FavoriteType.SERIES -> {
                getDisplayYear()?.let { parts.add(it) }
                seasonsCount?.let { seasons ->
                    episodesCount?.let { episodes ->
                        parts.add("$seasons saison${if (seasons > 1) "s" else ""} • $episodes épisodes")
                    } ?: parts.add("$seasons saison${if (seasons > 1) "s" else ""}")
                }
                status?.let { parts.add(it) }
                getDisplayRating()?.let { parts.add("★ $it") }
            }
        }
        
        return parts.joinToString(" • ").takeIf { it.isNotBlank() } ?: "Aucune information"
    }
    
    companion object {
        fun fromMovie(movie: Movie): Favorite {
            return Favorite(
                id = movie.title.hashCode().toString(),
                type = FavoriteType.MOVIE,
                title = movie.getDisplayTitle(),
                imageUrl = movie.imageUrl,
                remoteImageUrl = movie.remoteImageUrl,
                synopsis = movie.getDisplayOverview(),
                genres = movie.genres?.let { com.google.gson.Gson().toJson(it) },
                rating = movie.getDisplayRating(),
                releaseDate = movie.getDisplayReleaseDate(),
                director = movie.getFormattedDirectors()
            )
        }
        
        fun fromSeries(series: Series): Favorite {
            return Favorite(
                id = series.getSeriesId(),
                type = FavoriteType.SERIES,
                title = series.getDisplayTitle(),
                imageUrl = series.imageUrl,
                remoteImageUrl = series.remoteImageUrl,
                synopsis = series.getDisplayOverview(),
                genres = series.genres?.let { com.google.gson.Gson().toJson(it) },
                rating = series.getDisplayRating(),
                releaseDate = series.getDisplayReleaseDate(),
                status = series.getFormattedStatus(),
                seasonsCount = series.getTotalSeasons(),
                episodesCount = series.getTotalEpisodes(),
                network = series.getFormattedNetworks()
            )
        }
    }
}

enum class FavoriteType {
    MOVIE,
    SERIES
}