package dev.pecorio.alphastream.data.repository

import dev.pecorio.alphastream.data.api.AlphaStreamApiService
import dev.pecorio.alphastream.data.model.*
import dev.pecorio.alphastream.utils.SeriesValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepository @Inject constructor(
    private val apiService: AlphaStreamApiService
) {
    
    /**
     * Récupère les films avec gestion d'erreur
     */
    suspend fun getMovies(page: Int = 1, limit: Int = 50): Result<MoviesResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMovies(page, limit)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Erreur lors de la récupération des films: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Récupère les séries avec gestion d'erreur
     */
    suspend fun getSeries(page: Int = 1, limit: Int = 50): Result<SeriesResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getSeries(page, limit)
                if (response.isSuccessful && response.body() != null) {
                    val seriesResponse = response.body()!!
                    
                    // Log détaillé pour debug
                    android.util.Log.d("ContentRepository", "=== Début traitement séries ===")
                    android.util.Log.d("ContentRepository", "Séries reçues de l'API: ${seriesResponse.series.size}")
                    
                    // Afficher quelques exemples de séries reçues
                    seriesResponse.series.take(3).forEachIndexed { index, series ->
                        android.util.Log.d("ContentRepository", "Série $index: titre='${series.title}', id='${series.id}', saisons=${series.seasons?.size ?: 0}")
                    }
                    
                    // Valider et nettoyer les données des séries avec des critères très permissifs
                    val cleanedSeries = SeriesValidator.validateAndCleanSeriesList(seriesResponse.series)
                    
                    // Log après nettoyage
                    android.util.Log.d("ContentRepository", "Séries après validation: ${cleanedSeries.size}")
                    
                    // Si aucune série n'est valide, retourner quand même les données originales
                    val finalSeries = if (cleanedSeries.isEmpty() && seriesResponse.series.isNotEmpty()) {
                        android.util.Log.w("ContentRepository", "Aucune série valide après nettoyage, retour des données originales")
                        seriesResponse.series
                    } else {
                        cleanedSeries
                    }
                    
                    val cleanedResponse = seriesResponse.copy(series = finalSeries)
                    android.util.Log.d("ContentRepository", "Séries finales retournées: ${finalSeries.size}")
                    android.util.Log.d("ContentRepository", "=== Fin traitement séries ===")
                    
                    Result.success(cleanedResponse)
                } else {
                    Result.failure(Exception("Erreur lors de la récupération des séries: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Récupère les détails d'une série avec validation
     */
    suspend fun getSeriesDetails(seriesId: String): Result<Series> {
        return withContext(Dispatchers.IO) {
            try {
                if (seriesId.isBlank()) {
                    android.util.Log.e("ContentRepository", "getSeriesDetails: ID de série vide")
                    return@withContext Result.failure(Exception("ID de série invalide"))
                }
                
                android.util.Log.d("ContentRepository", "getSeriesDetails: Appel API pour seriesId='$seriesId'")
                val response = apiService.getSeriesDetails(seriesId)
                android.util.Log.d("ContentRepository", "getSeriesDetails: Réponse API - Code: ${response.code()}, Success: ${response.isSuccessful}")
                
                if (response.body() != null) {
                    android.util.Log.d("ContentRepository", "getSeriesDetails: Réponse JSON reçue avec succès")
                }
                if (response.isSuccessful && response.body() != null) {
                    val seriesDetailsResponse = response.body()!!
                    val series = seriesDetailsResponse.series
                    android.util.Log.d("ContentRepository", "getSeriesDetails: Série extraite - Titre: '${series.title}', ID: '${series.id}'")
                    
                    // Valider et nettoyer les données de la série
                    val cleanedSeries = SeriesValidator.validateAndCleanSeries(series)
                    
                    if (!SeriesValidator.isSeriesValid(cleanedSeries)) {
                        return@withContext Result.failure(Exception("Données de série invalides ou incomplètes"))
                    }
                    Result.success(cleanedSeries)
                } else {
                    val errorMsg = when (response.code()) {
                        404 -> "Série non trouvée"
                        500 -> "Erreur serveur"
                        else -> "Erreur lors de la récupération des détails: ${response.code()}"
                    }
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Erreur de connexion: ${e.message}", e))
            }
        }
    }
    
    /**
     * Récupère les flux de streaming pour un épisode avec validation
     */
    suspend fun getEpisodeStreams(
        seriesId: String,
        seasonNumber: Int,
        episodeNumber: Int
    ): Result<EpisodeStreamsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Validation des paramètres
                if (seriesId.isBlank()) {
                    return@withContext Result.failure(Exception("ID de série invalide"))
                }
                if (seasonNumber < 1) {
                    return@withContext Result.failure(Exception("Numéro de saison invalide: $seasonNumber"))
                }
                if (episodeNumber < 1) {
                    return@withContext Result.failure(Exception("Numéro d'épisode invalide: $episodeNumber"))
                }
                
                val response = apiService.getEpisodeStreams(seriesId, seasonNumber, episodeNumber)
                if (response.isSuccessful && response.body() != null) {
                    val streamsResponse = response.body()!!
                    // Vérification qu'il y a au moins un stream valide
                    if (!streamsResponse.hasStreams()) {
                        return@withContext Result.failure(Exception("Aucun flux de streaming disponible pour cet épisode"))
                    }
                    Result.success(streamsResponse)
                } else {
                    val errorMsg = when (response.code()) {
                        404 -> "Épisode non trouvé"
                        403 -> "Accès refusé aux flux de streaming"
                        500 -> "Erreur serveur"
                        else -> "Erreur lors de la récupération des streams: ${response.code()}"
                    }
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Erreur de connexion: ${e.message}", e))
            }
        }
    }
    
    /**
     * Recherche de contenu
     */
    suspend fun searchContent(query: String, page: Int = 1, limit: Int = 50): Result<SearchResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.searchContent(query, page, limit)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Erreur lors de la recherche: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Récupère les derniers contenus pour la page d'accueil
     */
    suspend fun getHomeContent(): Result<HomeContent> {
        return withContext(Dispatchers.IO) {
            try {
                // Récupération parallèle des films et séries
                val moviesResult = getMovies(1, 20)
                val seriesResult = getSeries(1, 20)
                
                val movies = moviesResult.getOrNull()?.movies ?: emptyList()
                val series = seriesResult.getOrNull()?.series ?: emptyList()
                
                val homeContent = HomeContent(
                    latestMovies = movies,
                    latestSeries = series,
                    trending = (movies + series.map { it.toSearchResult() }).shuffled().take(10)
                )
                
                Result.success(homeContent)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

/**
 * Modèle pour le contenu de la page d'accueil
 */
data class HomeContent(
    val latestMovies: List<Movie>,
    val latestSeries: List<Series>,
    val trending: List<Any> // Mix de films et séries
)

/**
 * Extension pour convertir une série en SearchResult
 */
private fun Series.toSearchResult(): SearchResult {
    return SearchResult(
        type = "series",
        title = title,
        imageUrl = imageUrl,
        remoteImageUrl = remoteImageUrl,
        synopsis = synopsis,
        genres = genres,
        releaseDate = releaseDate,
        rating = rating,
        quality = quality,
        cast = cast,
        tmdbTitle = null,
        tmdbOverview = null,
        tmdbVoteAverage = null,
        uqloadOldUrl = null,
        uqloadNewUrl = null,
        id = id,
        seasons = seasons
    )
}