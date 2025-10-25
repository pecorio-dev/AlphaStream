package dev.pecorio.alphastream.utils

import dev.pecorio.alphastream.data.model.Episode
import dev.pecorio.alphastream.data.model.Season
import dev.pecorio.alphastream.data.model.Series
import dev.pecorio.alphastream.data.model.StreamInfo

/**
 * Utilitaire pour valider et nettoyer les données des séries
 */
object SeriesValidator {
    
    /**
     * Valide une série et retourne une version nettoyée
     */
    fun validateAndCleanSeries(series: Series): Series {
        return series.copy(
            title = series.title.takeIf { it.isNotBlank() } ?: "Série sans titre",
            synopsis = series.synopsis?.takeIf { it.isNotBlank() },
            genres = series.genres?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() },
            cast = series.cast?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() },
            creators = series.creators?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() },
            directors = series.directors?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() },
            networks = series.networks?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() },
            countries = series.countries?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() },
            rating = series.rating?.takeIf { it >= 0.0 && it <= 10.0 },
            seasons = series.seasons?.mapNotNull { validateAndCleanSeason(it) }?.takeIf { it.isNotEmpty() }
        )
    }
    
    /**
     * Valide une saison et retourne une version nettoyée
     * Très permissif - accepte toutes les saisons
     */
    fun validateAndCleanSeason(season: Season): Season? {
        val validEpisodes = season.episodes?.mapNotNull { validateAndCleanEpisode(it) }
        
        // Accepter toutes les saisons, même sans numéro ou épisodes
        // Assigner un numéro par défaut si manquant
        return season.copy(
            number = season.number?.takeIf { it > 0 } ?: 1,
            episodes = validEpisodes ?: emptyList(),
            title = season.title?.takeIf { it.isNotBlank() }
        )
    }
    
    /**
     * Valide un épisode et retourne une version nettoyée
     * Très permissif - accepte tous les épisodes
     */
    fun validateAndCleanEpisode(episode: Episode): Episode? {
        // Accepter tous les épisodes, assigner un numéro par défaut si manquant
        val episodeNumber = episode.number ?: episode.index ?: 1
        
        // Nettoyer les streams (mais accepter les épisodes sans streams)
        val validStreams = episode.streams?.mapNotNull { validateAndCleanStream(it) }
        
        return episode.copy(
            number = episodeNumber,
            title = episode.title?.takeIf { it.isNotBlank() },
            overview = episode.overview?.takeIf { it.isNotBlank() },
            uqloadOldUrl = episode.uqloadOldUrl?.takeIf { it.isNotBlank() },
            uqloadNewUrl = episode.uqloadNewUrl?.takeIf { it.isNotBlank() },
            streams = validStreams,
            runtime = episode.runtime?.takeIf { it > 0 }
        )
    }
    
    /**
     * Valide un stream et retourne une version nettoyée
     * Très permissif - accepte tous les streams avec URL
     */
    fun validateAndCleanStream(stream: StreamInfo): StreamInfo? {
        // Un stream doit avoir une URL non vide (validation minimale)
        if (stream.url.isNullOrBlank()) {
            return null
        }
        
        return stream.copy(
            server = stream.server?.takeIf { it.isNotBlank() } ?: "Serveur inconnu",
            url = stream.url,
            quality = stream.quality?.takeIf { it.isNotBlank() },
            type = stream.type?.takeIf { it.isNotBlank() },
            language = stream.language?.takeIf { it.isNotBlank() }
        )
    }
    
    /**
     * Vérifie si une URL est valide (format basique)
     */
    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }
    
    /**
     * Vérifie si une série a des données minimales requises
     * Très permissif - accepte toute série avec un titre
     */
    fun isSeriesValid(series: Series): Boolean {
        // Une série est valide si elle a au moins un titre non vide
        // Toutes les autres données sont optionnelles
        return series.title.isNotBlank()
    }
    
    /**
     * Vérifie si une saison a des données valides
     */
    fun isSeasonValid(season: Season): Boolean {
        return season.hasValidEpisodes() && (season.number ?: 0) > 0
    }
    
    /**
     * Vérifie si un épisode a des données valides
     */
    fun isEpisodeValid(episode: Episode): Boolean {
        return episode.getEpisodeNumber() > 0 && 
               (episode.hasStreamingLinks() || episode.title?.isNotBlank() == true)
    }
    
    /**
     * Nettoie et valide une liste de séries
     */
    fun validateAndCleanSeriesList(seriesList: List<Series>): List<Series> {
        return seriesList.mapNotNull { series ->
            val cleanedSeries = validateAndCleanSeries(series)
            if (isSeriesValid(cleanedSeries)) cleanedSeries else null
        }
    }
    
    /**
     * Trouve la meilleure qualité disponible pour un épisode
     */
    fun getBestQualityForEpisode(episode: Episode): String? {
        val qualities = mutableListOf<String>()
        
        // Ajouter les qualités des streams
        episode.streams?.forEach { stream ->
            stream.getFormattedQuality()?.let { qualities.add(it) }
        }
        
        // Retourner la meilleure qualité
        return qualities.maxByOrNull { quality ->
            when (quality) {
                "4K", "2160P" -> 4
                "FHD", "1080P" -> 3
                "HD", "720P" -> 2
                "SD", "480P" -> 1
                else -> 0
            }
        }
    }
    
    /**
     * Compte le nombre total d'épisodes disponibles avec des liens de streaming
     */
    fun countAvailableEpisodes(series: Series): Int {
        return series.seasons?.sumOf { season ->
            season.episodes?.count { episode ->
                episode.hasStreamingLinks()
            } ?: 0
        } ?: 0
    }
    
    /**
     * Génère un résumé de la série pour l'affichage
     */
    fun generateSeriesSummary(series: Series): String {
        val parts = mutableListOf<String>()
        
        val totalSeasons = series.getTotalSeasons()
        val totalEpisodes = series.getTotalEpisodes()
        val availableEpisodes = countAvailableEpisodes(series)
        
        if (totalSeasons > 0) {
            parts.add("$totalSeasons saison${if (totalSeasons > 1) "s" else ""}")
        }
        
        if (totalEpisodes > 0) {
            if (availableEpisodes > 0 && availableEpisodes != totalEpisodes) {
                parts.add("$availableEpisodes/$totalEpisodes épisodes disponibles")
            } else {
                parts.add("$totalEpisodes épisode${if (totalEpisodes > 1) "s" else ""}")
            }
        }
        
        series.getFormattedStatus()?.let { parts.add(it) }
        
        return parts.joinToString(" • ").takeIf { it.isNotBlank() } 
            ?: "Informations non disponibles"
    }
}