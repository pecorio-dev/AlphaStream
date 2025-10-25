package dev.pecorio.alphastream.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import dev.pecorio.alphastream.utils.ImageUrlHelper

@Parcelize
data class Movie(
    @SerializedName("title")
    val title: String,
    
    @SerializedName("image_url")
    val imageUrl: String?,
    
    @SerializedName("remote_image_url")
    val remoteImageUrl: String?,
    
    @SerializedName("details_link")
    val detailsLink: String?,
    
    @SerializedName("uqload_old_url")
    val uqloadOldUrl: String?,
    
    @SerializedName("uqload_new_url")
    val uqloadNewUrl: String?,
    
    @SerializedName("tmdb_id")
    val tmdbId: String?,
    
    @SerializedName("tmdb_title")
    val tmdbTitle: String?,
    
    @SerializedName("tmdb_overview")
    val tmdbOverview: String?,
    
    @SerializedName("tmdb_release_date")
    val tmdbReleaseDate: String?,
    
    @SerializedName("tmdb_vote_average")
    val tmdbVoteAverage: Double?,
    
    @SerializedName("synopsis")
    val synopsis: String?,
    
    @SerializedName("original_title")
    val originalTitle: String?,
    
    @SerializedName("genres")
    val genres: List<String>?,
    
    @SerializedName("release_date")
    val releaseDate: String?,
    
    @SerializedName("rating")
    val rating: Double?,
    
    @SerializedName("director")
    val director: String?,
    
    @SerializedName("directors")
    val directors: List<String>?,
    
    @SerializedName("cast")
    val cast: List<String>?,
    
    @SerializedName("language")
    val language: String?,
    
    @SerializedName("original_language")
    val originalLanguage: String?,
    
    @SerializedName("quality")
    val quality: String?,
    
    @SerializedName("version")
    val version: String?,
    
    @SerializedName("scraped_at")
    val scrapedAt: String?
) : Parcelable {
    
    /**
     * Retourne l'ID unique du film pour la sauvegarde de progression
     */
    fun getMovieId(): String = tmdbId ?: title.hashCode().toString()
    
    fun getDisplayTitle(): String = tmdbTitle ?: title
    
    fun getDisplayOverview(): String? = synopsis ?: tmdbOverview
    
    fun getDisplayImageUrl(): String? = ImageUrlHelper.getOptimalImageUrl(imageUrl, remoteImageUrl)
    
    fun getDisplayRating(): Double? = tmdbVoteAverage ?: rating
    
    fun getDisplayReleaseDate(): String? = tmdbReleaseDate ?: releaseDate
    
    fun getDisplayDirectors(): List<String>? = directors ?: director?.let { listOf(it) }
    
    fun getStreamUrl(): String? {
        // Force old URL as requested, fallback to new URL if old is not available
        return uqloadOldUrl ?: uqloadNewUrl
    }
    
    fun getFormattedGenres(): String? = genres?.joinToString(", ")
    
    fun getFormattedCast(): String? = cast?.take(3)?.joinToString(", ")
    
    fun getFormattedDirectors(): String? = getDisplayDirectors()?.joinToString(", ")
    
    fun getQualityBadge(): String? = quality?.uppercase()
}

@Parcelize
data class MoviesResponse(
    @SerializedName("movies")
    val movies: List<Movie>,
    
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("limit")
    val limit: Int,
    
    @SerializedName("total")
    val total: Int?
) : Parcelable