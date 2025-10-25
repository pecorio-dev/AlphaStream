package dev.pecorio.alphastream.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import dev.pecorio.alphastream.utils.ImageUrlHelper

@Parcelize
data class Series(
    @SerializedName("id")
    val id: String? = null,
    
    @SerializedName("title")
    val title: String = "",
    
    @SerializedName("image_url")
    val imageUrl: String? = null,
    
    @SerializedName("remote_image_url")
    val remoteImageUrl: String? = null,
    
    @SerializedName("local_image_url")
    val localImageUrl: String? = null,
    
    @SerializedName("link")
    val link: String? = null,
    
    @SerializedName("synopsis")
    val synopsis: String? = null,
    
    @SerializedName("genres")
    val genres: List<String>? = null,
    
    @SerializedName("release_date")
    val releaseDate: String? = null,
    
    @SerializedName("rating")
    val rating: Double? = null,
    
    @SerializedName("original_title")
    val originalTitle: String? = null,
    
    @SerializedName("cast")
    val cast: List<String>? = null,
    
    @SerializedName("creators")
    val creators: List<String>? = null,
    
    @SerializedName("directors")
    val directors: List<String>? = null,
    
    @SerializedName("writers")
    val writers: List<String>? = null,
    
    @SerializedName("producers")
    val producers: List<String>? = null,
    
    @SerializedName("status")
    val status: String? = null,
    
    @SerializedName("network")
    val network: String? = null,
    
    @SerializedName("networks")
    val networks: List<String>? = null,
    
    @SerializedName("country")
    val country: String? = null,
    
    @SerializedName("countries")
    val countries: List<String>? = null,
    
    @SerializedName("quality")
    val quality: String? = null,
    
    @SerializedName("version")
    val version: String? = null,
    
    @SerializedName("language")
    val language: String? = null,
    
    @SerializedName("original_language")
    val originalLanguage: String? = null,
    
    @SerializedName("details")
    val details: String? = null,
    
    @SerializedName("seasons")
    val seasons: List<Season>? = null
) : Parcelable {
    
    /**
     * Retourne l'ID de la série, avec fallback sur le titre si l'ID est null
     */
    fun getSeriesId(): String = id ?: title.hashCode().toString()
    
    /**
     * Retourne le titre d'affichage avec fallback
     */
    fun getDisplayTitle(): String = title.takeIf { it.isNotBlank() } ?: "Série sans titre"
    
    /**
     * Retourne le synopsis d'affichage avec fallback
     */
    fun getDisplayOverview(): String? = synopsis?.takeIf { it.isNotBlank() } ?: details?.takeIf { it.isNotBlank() }
    
    /**
     * Retourne l'URL d'image avec priorité et fallback
     */
    fun getDisplayImageUrl(): String? = ImageUrlHelper.getOptimalImageUrl(imageUrl, remoteImageUrl)
    
    /**
     * Retourne la note avec validation
     */
    fun getDisplayRating(): Double? = rating?.takeIf { it >= 0.0 && it <= 10.0 }
    
    /**
     * Retourne la date de sortie formatée
     */
    fun getDisplayReleaseDate(): String? = releaseDate?.takeIf { it.isNotBlank() }
    
    /**
     * Retourne la première date de diffusion si disponible
     */
    fun getFirstAirDate(): String? {
        // Essayer d'extraire depuis les métadonnées supplémentaires si disponibles
        return releaseDate?.takeIf { it.isNotBlank() }
    }
    
    /**
     * Retourne la dernière date de diffusion si disponible
     */
    fun getLastAirDate(): String? {
        // Pour l'instant, retourne null car pas disponible dans l'API
        return null
    }
    
    /**
     * Retourne les genres formatés avec sécurité
     */
    fun getFormattedGenres(): String? = genres?.filter { it.isNotBlank() }?.joinToString(", ")?.takeIf { it.isNotBlank() }
    
    /**
     * Retourne le casting formaté (3 premiers acteurs)
     */
    fun getFormattedCast(): String? = cast?.filter { it.isNotBlank() }?.take(3)?.joinToString(", ")?.takeIf { it.isNotBlank() }
    
    /**
     * Retourne le badge de qualité formaté
     */
    fun getQualityBadge(): String? = quality?.takeIf { it.isNotBlank() }?.uppercase()
    
    /**
     * Retourne les réseaux formatés avec fallback
     */
    fun getFormattedNetworks(): String? = networks?.filter { it.isNotBlank() }?.joinToString(", ")?.takeIf { it.isNotBlank() }
        ?: network?.takeIf { it.isNotBlank() }
    
    /**
     * Retourne les pays formatés avec fallback
     */
    fun getFormattedCountries(): String? = countries?.filter { it.isNotBlank() }?.joinToString(", ")?.takeIf { it.isNotBlank() }
        ?: country?.takeIf { it.isNotBlank() }
    
    /**
     * Retourne le nombre total d'épisodes avec sécurité
     */
    fun getTotalEpisodes(): Int {
        return try {
            seasons?.sumOf { season -> 
                try {
                    season.episodes?.size ?: 0
                } catch (e: Exception) {
                    0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Retourne le nombre total de saisons avec sécurité
     */
    fun getTotalSeasons(): Int {
        return try {
            seasons?.size ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Retourne le statut formaté
     */
    fun getFormattedStatus(): String? = when (status?.lowercase()) {
        "ended", "completed", "finished" -> "Terminée"
        "continuing", "ongoing", "running" -> "En cours"
        "canceled", "cancelled" -> "Annulée"
        "upcoming" -> "À venir"
        "hiatus" -> "En pause"
        else -> status?.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() }
    }
    
    /**
     * Vérifie si la série a des saisons valides
     */
    fun hasValidSeasons(): Boolean {
        return try {
            !seasons.isNullOrEmpty() && seasons.any { 
                try {
                    it.hasValidEpisodes()
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Retourne les créateurs formatés
     */
    fun getFormattedCreators(): String? = creators?.filter { it.isNotBlank() }?.joinToString(", ")?.takeIf { it.isNotBlank() }
    
    /**
     * Retourne les réalisateurs formatés
     */
    fun getFormattedDirectors(): String? = directors?.filter { it.isNotBlank() }?.joinToString(", ")?.takeIf { it.isNotBlank() }
    
    /**
     * Retourne une description complète pour l'affichage
     */
    fun getDisplayDescription(): String {
        val parts = mutableListOf<String>()
        
        getTotalSeasons().takeIf { it > 0 }?.let { seasons ->
            getTotalEpisodes().takeIf { it > 0 }?.let { episodes ->
                parts.add("$seasons saison${if (seasons > 1) "s" else ""} • $episodes épisode${if (episodes > 1) "s" else ""}")
            } ?: parts.add("$seasons saison${if (seasons > 1) "s" else ""}")
        }
        
        getFormattedStatus()?.let { parts.add(it) }
        getDisplayReleaseDate()?.let { date ->
            if (date.length >= 4) parts.add(date.substring(0, 4))
        }
        
        return parts.joinToString(" • ").takeIf { it.isNotBlank() } ?: "Informations non disponibles"
    }
    
    /**
     * Vérifie si les données essentielles sont disponibles
     */
    fun hasEssentialData(): Boolean {
        return title.isNotBlank() && !seasons.isNullOrEmpty()
    }
    
    /**
     * Vérifie si les métadonnées sont complètes
     */
    fun hasCompleteMetadata(): Boolean {
        return hasEssentialData() && 
               synopsis?.isNotBlank() == true &&
               !genres.isNullOrEmpty() &&
               !cast.isNullOrEmpty()
    }
    
    /**
     * Retourne un indicateur de complétude des données
     */
    fun getDataCompletenessIndicator(): String {
        val score = calculateCompletenessScore()
        return when {
            score >= 80 -> "Données complètes"
            score >= 60 -> "Données partielles"
            score >= 40 -> "Données limitées"
            else -> "Données minimales"
        }
    }
    
    /**
     * Calcule un score de complétude des données (0-100)
     * Très généreux - toute série avec titre obtient un bon score
     */
    fun calculateCompletenessScore(): Int {
        var score = 0
        
        // Titre = 80 points (base très généreuse)
        if (title.isNotBlank()) score += 80
        
        // Bonus pour données supplémentaires (20 points total)
        if (!seasons.isNullOrEmpty()) score += 5
        if (synopsis?.isNotBlank() == true) score += 5
        if (!genres.isNullOrEmpty()) score += 3
        if (getDisplayImageUrl()?.isNotBlank() == true) score += 3
        if (!cast.isNullOrEmpty() || !directors.isNullOrEmpty()) score += 2
        if (rating != null) score += 1
        if (releaseDate?.isNotBlank() == true) score += 1
        
        return score
    }
    
    /**
     * Retourne les champs manquants pour un affichage complet
     * Très permissif - seul le titre est vraiment requis
     */
    fun getMissingDataFields(): List<String> {
        val missing = mutableListOf<String>()
        
        // Seul le titre est vraiment essentiel
        // Tous les autres champs sont optionnels et ne doivent pas empêcher l'affichage
        // if (synopsis.isNullOrBlank()) missing.add("Synopsis")
        // if (genres.isNullOrEmpty()) missing.add("Genres")
        // if (getDisplayImageUrl().isNullOrBlank()) missing.add("Image")
        
        // Retourner une liste vide pour que toutes les séries soient considérées comme "complètes"
        // tant qu'elles ont un titre
        
        return missing
    }
}

@Parcelize
data class Season(
    @SerializedName("number")
    val number: Int? = null,
    
    @SerializedName("episodes")
    val episodes: List<Episode>? = null,
    
    @SerializedName("title")
    val title: String? = null,
    
    @SerializedName("overview")
    val overview: String? = null,
    
    @SerializedName("poster_path")
    val posterPath: String? = null,
    
    @SerializedName("air_date")
    val airDate: String? = null
) : Parcelable {
    
    /**
     * Retourne le numéro de saison avec fallback
     */
    fun getSeasonNumber(): Int = number ?: 1
    
    /**
     * Retourne le titre de la saison avec fallback
     */
    fun getDisplayTitle(): String = title?.takeIf { it.isNotBlank() } ?: "Saison ${getSeasonNumber()}"
    
    /**
     * Vérifie si la saison a des épisodes valides
     */
    fun hasValidEpisodes(): Boolean = !episodes.isNullOrEmpty()
    
    /**
     * Retourne le nombre d'épisodes
     */
    fun getEpisodeCount(): Int = episodes?.size ?: 0
    
    /**
     * Retourne la description de la saison
     */
    fun getDisplayDescription(): String {
        val episodeCount = getEpisodeCount()
        return if (episodeCount > 0) {
            "$episodeCount épisode${if (episodeCount > 1) "s" else ""}"
        } else {
            "Aucun épisode disponible"
        }
    }
    
    /**
     * Vérifie si la saison a des données complètes
     */
    fun hasCompleteData(): Boolean {
        return number != null && hasValidEpisodes() && title?.isNotBlank() == true
    }
    
    /**
     * Retourne le statut de disponibilité de la saison
     */
    fun getAvailabilityStatus(): String {
        val episodeCount = getEpisodeCount()
        val availableEpisodes = episodes?.count { it.hasStreamingLinks() } ?: 0
        
        return when {
            episodeCount == 0 -> "Aucun épisode"
            availableEpisodes == 0 -> "Indisponible"
            availableEpisodes == episodeCount -> "Complète"
            else -> "Partielle ($availableEpisodes/$episodeCount)"
        }
    }
}

@Parcelize
data class Episode(
    @SerializedName("id")
    val id: String? = null,
    
    @SerializedName("number")
    val number: Int? = null,
    
    @SerializedName("title")
    val title: String? = null,
    
    @SerializedName("overview")
    val overview: String? = null,
    
    @SerializedName("link")
    val link: String? = null,
    
    @SerializedName("uqload_old_url")
    val uqloadOldUrl: String? = null,
    
    @SerializedName("uqload_new_url")
    val uqloadNewUrl: String? = null,
    
    @SerializedName("index")
    val index: Int? = null,
    
    @SerializedName("streams")
    val streams: List<StreamInfo>? = null,
    
    @SerializedName("air_date")
    val airDate: String? = null,
    
    @SerializedName("runtime")
    val runtime: Int? = null,
    
    @SerializedName("still_path")
    val stillPath: String? = null
) : Parcelable {
    
    /**
     * Retourne l'ID de l'épisode avec fallback
     */
    fun getEpisodeId(): String = id ?: "${number ?: index ?: 0}"
    
    /**
     * Retourne le numéro d'épisode avec fallback
     */
    fun getEpisodeNumber(): Int = number ?: index ?: 1
    
    /**
     * Retourne le titre de l'épisode avec fallback
     */
    fun getDisplayTitle(): String = title?.takeIf { it.isNotBlank() } ?: "Épisode ${getEpisodeNumber()}"
    
    /**
     * Retourne l'URL de streaming avec priorité sur l'ancienne URL
     */
    fun getStreamUrl(): String? = uqloadOldUrl?.takeIf { it.isNotBlank() } 
        ?: uqloadNewUrl?.takeIf { it.isNotBlank() }
    
    /**
     * Vérifie si l'épisode a des liens de streaming valides
     */
    fun hasStreamingLinks(): Boolean {
        return !uqloadOldUrl.isNullOrBlank() || 
               !uqloadNewUrl.isNullOrBlank() || 
               !streams.isNullOrEmpty()
    }
    
    /**
     * Retourne le nombre de serveurs disponibles
     */
    fun getAvailableServersCount(): Int {
        var count = 0
        if (!uqloadOldUrl.isNullOrBlank()) count++
        if (!uqloadNewUrl.isNullOrBlank()) count++
        streams?.forEach { stream ->
            if (!stream.url.isNullOrBlank()) count++
        }
        return count
    }
    
    /**
     * Retourne la liste des serveurs disponibles
     */
    fun getAvailableServers(): List<String> {
        val servers = mutableListOf<String>()
        
        if (!uqloadOldUrl.isNullOrBlank()) servers.add("UQLOAD (Old)")
        if (!uqloadNewUrl.isNullOrBlank()) servers.add("UQLOAD (New)")
        
        streams?.forEach { stream ->
            if (!stream.url.isNullOrBlank() && !stream.server.isNullOrBlank()) {
                servers.add(stream.server)
            }
        }
        
        return servers.distinct()
    }
    
    /**
     * Retourne le statut de disponibilité de l'épisode
     */
    fun getAvailabilityStatus(): String {
        val serverCount = getAvailableServersCount()
        return when {
            serverCount == 0 -> "Indisponible"
            serverCount == 1 -> "1 serveur"
            else -> "$serverCount serveurs"
        }
    }
    
    /**
     * Vérifie si l'épisode a des données minimales
     */
    fun hasMinimalData(): Boolean {
        return number != null && number > 0
    }
    
    /**
     * Retourne tous les liens de streaming disponibles
     */
    fun getAllStreamUrls(): List<String> {
        val urls = mutableListOf<String>()
        getStreamUrl()?.let { urls.add(it) }
        streams?.mapNotNull { it.url?.takeIf { url -> url.isNotBlank() } }?.let { urls.addAll(it) }
        return urls.distinct()
    }
    
    /**
     * Retourne la durée formatée
     */
    fun getFormattedRuntime(): String? = runtime?.let { minutes ->
        when {
            minutes < 60 -> "${minutes}min"
            else -> "${minutes / 60}h ${minutes % 60}min"
        }
    }
    
    /**
     * Retourne la description complète de l'épisode
     */
    fun getDisplayDescription(): String {
        val parts = mutableListOf<String>()
        
        getFormattedRuntime()?.let { parts.add(it) }
        airDate?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        
        return parts.joinToString(" • ").takeIf { it.isNotBlank() } ?: ""
    }
}

@Parcelize
data class StreamInfo(
    @SerializedName("server")
    val server: String? = null,
    
    @SerializedName("url")
    val url: String? = null,
    
    @SerializedName("quality")
    val quality: String? = null,
    
    @SerializedName("type")
    val type: String? = null,
    
    @SerializedName("language")
    val language: String? = null
) : Parcelable {
    
    /**
     * Retourne le nom du serveur avec fallback
     */
    fun getServerName(): String = server?.takeIf { it.isNotBlank() } ?: "Serveur inconnu"
    
    /**
     * Retourne la qualité formatée
     */
    fun getFormattedQuality(): String? = quality?.takeIf { it.isNotBlank() }?.uppercase()
    
    /**
     * Vérifie si le stream est valide
     */
    fun isValid(): Boolean = !url.isNullOrBlank()
    
    /**
     * Retourne la description du stream
     */
    fun getDisplayDescription(): String {
        val parts = mutableListOf<String>()
        
        parts.add(getServerName())
        getFormattedQuality()?.let { parts.add(it) }
        language?.takeIf { it.isNotBlank() }?.let { parts.add(it.uppercase()) }
        
        return parts.joinToString(" • ")
    }
}

@Parcelize
data class SeriesResponse(
    @SerializedName("series")
    val series: List<Series> = emptyList(),
    
    @SerializedName("page")
    val page: Int = 1,
    
    @SerializedName("limit")
    val limit: Int = 50,
    
    @SerializedName("total")
    val total: Int? = null,
    
    @SerializedName("has_more")
    val hasMore: Boolean? = null
) : Parcelable {
    
    /**
     * Vérifie s'il y a plus de pages
     */
    fun hasMorePages(): Boolean = hasMore ?: (series.size >= limit)
    
    /**
     * Retourne le nombre total avec fallback
     */
    fun getTotalCount(): Int = total ?: series.size
}

@Parcelize
data class SeriesDetailsResponse(
    @SerializedName("series")
    val series: Series,
    
    @SerializedName("performance")
    val performance: @RawValue Map<String, Any>? = null,
    
    @SerializedName("timestamp")
    val timestamp: String? = null
) : Parcelable

@Parcelize
data class EpisodeStreamsResponse(
    @SerializedName("streams")
    val streams: List<StreamInfo> = emptyList(),
    
    @SerializedName("episode_id")
    val episodeId: String? = null,
    
    @SerializedName("series_id")
    val seriesId: String? = null,
    
    @SerializedName("season_number")
    val seasonNumber: Int? = null,
    
    @SerializedName("episode_number")
    val episodeNumber: Int? = null
) : Parcelable {
    
    /**
     * Retourne les streams valides uniquement
     */
    fun getValidStreams(): List<StreamInfo> = streams.filter { it.isValid() }
    
    /**
     * Vérifie s'il y a des streams disponibles
     */
    fun hasStreams(): Boolean = getValidStreams().isNotEmpty()
    
    /**
     * Retourne le meilleur stream (priorité sur la qualité)
     */
    fun getBestStream(): StreamInfo? = getValidStreams().maxByOrNull { stream ->
        when (stream.getFormattedQuality()) {
            "4K", "2160P" -> 4
            "FHD", "1080P" -> 3
            "HD", "720P" -> 2
            "SD", "480P" -> 1
            else -> 0
        }
    }
}

/**
 * Extension pour créer un Series sécurisé à partir de données potentiellement incomplètes
 */
fun createSafeSeries(
    id: String? = null,
    title: String? = null,
    imageUrl: String? = null,
    synopsis: String? = null,
    genres: List<String>? = null,
    rating: Double? = null,
    releaseDate: String? = null,
    status: String? = null,
    seasons: List<Season>? = null
): Series {
    return Series(
        id = id,
        title = title ?: "Série sans titre",
        imageUrl = imageUrl,
        remoteImageUrl = imageUrl,
        synopsis = synopsis,
        genres = genres?.filter { it.isNotBlank() },
        rating = rating?.takeIf { it >= 0.0 && it <= 10.0 },
        releaseDate = releaseDate,
        status = status,
        seasons = seasons?.filter { it.hasValidEpisodes() }
    )
}