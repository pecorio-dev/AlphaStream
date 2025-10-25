package dev.pecorio.alphastream.data.api

import dev.pecorio.alphastream.data.model.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AlphaStreamApiService {
    
    companion object {
        const val BASE_URL = "http://78.197.211.192:25315/"
    }
    
    /**
     * Récupère une liste de films avec pagination
     */
    @GET("api/movies")
    suspend fun getMovies(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<MoviesResponse>
    
    /**
     * Récupère une liste de séries avec pagination
     */
    @GET("api/series")
    suspend fun getSeries(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<SeriesResponse>
    
    /**
     * Récupère les détails d'une série spécifique
     */
    @GET("api/series/{series_id}")
    suspend fun getSeriesDetails(
        @Path("series_id") seriesId: String
    ): Response<SeriesDetailsResponse>
    
    /**
     * Récupère les flux de streaming pour un épisode spécifique
     */
    @GET("api/series/{series_id}/season/{season_number}/episode/{episode_number}/streams")
    suspend fun getEpisodeStreams(
        @Path("series_id") seriesId: String,
        @Path("season_number") seasonNumber: Int,
        @Path("episode_number") episodeNumber: Int
    ): Response<EpisodeStreamsResponse>
    
    /**
     * Recherche basique dans films et séries
     */
    @GET("api/search/basic")
    suspend fun searchContent(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<SearchResponse>
}