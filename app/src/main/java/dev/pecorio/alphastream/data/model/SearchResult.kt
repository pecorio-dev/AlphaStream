package dev.pecorio.alphastream.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import dev.pecorio.alphastream.utils.ImageUrlHelper

@Parcelize
data class SearchResult(
    @SerializedName("type")
    val type: String, // "movie" or "series"
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("image_url")
    val imageUrl: String?,
    
    @SerializedName("remote_image_url")
    val remoteImageUrl: String?,
    
    @SerializedName("synopsis")
    val synopsis: String?,
    
    @SerializedName("genres")
    val genres: List<String>?,
    
    @SerializedName("release_date")
    val releaseDate: String?,
    
    @SerializedName("rating")
    val rating: Double?,
    
    @SerializedName("quality")
    val quality: String?,
    
    @SerializedName("cast")
    val cast: List<String>?,
    
    // Movie specific fields
    @SerializedName("tmdb_title")
    val tmdbTitle: String?,
    
    @SerializedName("tmdb_overview")
    val tmdbOverview: String?,
    
    @SerializedName("tmdb_vote_average")
    val tmdbVoteAverage: Double?,
    
    @SerializedName("uqload_old_url")
    val uqloadOldUrl: String?,
    
    @SerializedName("uqload_new_url")
    val uqloadNewUrl: String?,
    
    // Series specific fields
    @SerializedName("id")
    val id: String?,
    
    @SerializedName("seasons")
    val seasons: List<Season>?
) : Parcelable {
    
    fun isMovie(): Boolean = type == "movie"
    
    fun isSeries(): Boolean = type == "series"
    
    fun getDisplayTitle(): String = tmdbTitle ?: title
    
    fun getDisplayOverview(): String? = synopsis ?: tmdbOverview
    
    fun getDisplayImageUrl(): String? = ImageUrlHelper.getOptimalImageUrl(imageUrl, remoteImageUrl)
    
    fun getDisplayRating(): Double? = tmdbVoteAverage ?: rating
    
    fun getDisplayReleaseDate(): String? = releaseDate
    
    fun getFormattedGenres(): String? = genres?.joinToString(", ")
    
    fun getFormattedCast(): String? = cast?.take(3)?.joinToString(", ")
    
    fun getQualityBadge(): String? = quality?.uppercase()
    
    /**
     * Convert SearchResult to Movie for navigation to MovieDetailsActivity
     */
    fun toMovie(): Movie {
        return Movie(
            title = title,
            imageUrl = imageUrl,
            remoteImageUrl = remoteImageUrl,
            detailsLink = null,
            uqloadOldUrl = uqloadOldUrl,
            uqloadNewUrl = uqloadNewUrl,
            tmdbId = null,
            tmdbTitle = tmdbTitle,
            tmdbOverview = tmdbOverview,
            tmdbReleaseDate = releaseDate,
            tmdbVoteAverage = tmdbVoteAverage,
            synopsis = synopsis,
            originalTitle = title,
            genres = genres,
            releaseDate = releaseDate,
            rating = rating,
            director = null,
            directors = emptyList(),
            cast = emptyList(),
            language = null,
            originalLanguage = null,
            quality = quality,
            version = null,
            scrapedAt = null
        )
    }
    
    fun toSeries(): Series? {
        return if (isSeries() && id != null) {
            Series(
                id = id,
                title = title,
                imageUrl = imageUrl,
                remoteImageUrl = remoteImageUrl,
                localImageUrl = null,
                link = null,
                synopsis = synopsis,
                genres = genres,
                releaseDate = releaseDate,
                rating = rating,
                originalTitle = null,
                cast = null,
                creators = null,
                directors = null,
                writers = null,
                producers = null,
                status = null,
                network = null,
                networks = null,
                country = null,
                countries = null,
                quality = quality,
                version = null,
                language = null,
                originalLanguage = null,
                details = null,
                seasons = seasons
            )
        } else null
    }
}

@Parcelize
data class SearchResponse(
    @SerializedName("results")
    val results: List<SearchResult>,
    
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("limit")
    val limit: Int,
    
    @SerializedName("total")
    val total: Int?
) : Parcelable