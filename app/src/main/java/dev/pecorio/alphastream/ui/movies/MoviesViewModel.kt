package dev.pecorio.alphastream.ui.movies

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pecorio.alphastream.data.model.Movie
import dev.pecorio.alphastream.data.repository.ContentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<MoviesUiState>(MoviesUiState.Loading)
    val uiState: LiveData<MoviesUiState> = _uiState

    private val _movies = MutableLiveData<List<Movie>>()
    val movies: LiveData<List<Movie>> = _movies

    private var searchJob: Job? = null
    private var currentPage = 1
    private var isLoading = false
    private var hasMorePages = true
    private var currentFilter = "all"
    private var currentSearchQuery = ""
    private var allMovies: List<Movie> = emptyList()

    init {
        loadMovies()
    }

    fun loadMovies(refresh: Boolean = false) {
        android.util.Log.d("MoviesViewModel", "loadMovies() démarré - refresh: $refresh")
        
        if (refresh) {
            currentPage = 1
            hasMorePages = true
            allMovies = emptyList()
        }

        if (isLoading) {
            android.util.Log.d("MoviesViewModel", "Déjà en cours de chargement, ignoré")
            return
        }

        viewModelScope.launch {
            isLoading = true
            if (refresh || allMovies.isEmpty()) {
                _uiState.value = MoviesUiState.Loading
            }

            try {
                android.util.Log.d("MoviesViewModel", "Appel API getMovies - page: $currentPage")
                val result = contentRepository.getMovies(currentPage, 50)
                
                if (result.isSuccess) {
                    val moviesResponse = result.getOrNull()!!
                    val newMovies = moviesResponse.movies
                    
                    android.util.Log.d("MoviesViewModel", "Films reçus: ${newMovies.size}")
                    
                    allMovies = if (refresh || currentPage == 1) {
                        newMovies
                    } else {
                        allMovies + newMovies
                    }
                    
                    android.util.Log.d("MoviesViewModel", "Total films: ${allMovies.size}")
                    
                    // Check if there are more pages
                    hasMorePages = newMovies.size >= 50
                    currentPage++
                    
                    // Apply current filters
                    applyFilters()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Erreur lors du chargement des films"
                    android.util.Log.e("MoviesViewModel", "Erreur API: $error")
                    _uiState.value = MoviesUiState.Error(error)
                }
            } catch (e: Exception) {
                android.util.Log.e("MoviesViewModel", "Exception", e)
                _uiState.value = MoviesUiState.Error(e.message ?: "Erreur inconnue")
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMoreMovies() {
        if (!isLoading && hasMorePages) {
            loadMovies(refresh = false)
        }
    }

    fun searchMovies(query: String) {
        currentSearchQuery = query
        
        // Cancel previous search
        searchJob?.cancel()
        
        if (query.isBlank()) {
            // Si la recherche est vide, afficher tous les films
            applyFilters()
            return
        }
        
        searchJob = viewModelScope.launch {
            // Add debounce delay
            delay(300)
            
            try {
                _uiState.value = MoviesUiState.Loading
                
                // Utiliser l'API de recherche
                val result = contentRepository.searchContent(query)
                
                if (result.isSuccess) {
                    val searchResponse = result.getOrNull()!!
                    // Filtrer seulement les films
                    val searchMovies = searchResponse.results
                        .filter { it.isMovie() }
                        .map { it.toMovie() }
                    
                    // Remplacer temporairement la liste des films par les résultats de recherche
                    _movies.value = searchMovies
                    
                    if (searchMovies.isEmpty()) {
                        _uiState.value = MoviesUiState.Empty
                    } else {
                        _uiState.value = MoviesUiState.Success
                    }
                } else {
                    // En cas d'erreur, faire une recherche locale
                    applyFilters()
                }
            } catch (e: Exception) {
                // En cas d'erreur, faire une recherche locale
                applyFilters()
            }
        }
    }

    fun setFilter(filter: String) {
        currentFilter = filter
        applyFilters()
    }

    private fun applyFilters() {
        var filteredMovies = allMovies

        // Apply search filter
        if (currentSearchQuery.isNotEmpty()) {
            filteredMovies = filteredMovies.filter { movie ->
                movie.getDisplayTitle().contains(currentSearchQuery, ignoreCase = true) ||
                movie.getDisplayOverview()?.contains(currentSearchQuery, ignoreCase = true) == true ||
                movie.getFormattedGenres()?.contains(currentSearchQuery, ignoreCase = true) == true
            }
        }

        // Apply genre filter
        if (currentFilter != "all") {
            filteredMovies = filteredMovies.filter { movie ->
                movie.getFormattedGenres()?.contains(currentFilter, ignoreCase = true) == true
            }
        }

        _movies.value = filteredMovies
        
        if (filteredMovies.isEmpty() && allMovies.isNotEmpty()) {
            _uiState.value = MoviesUiState.Empty
        } else if (filteredMovies.isEmpty()) {
            _uiState.value = MoviesUiState.Loading
        } else {
            _uiState.value = MoviesUiState.Success
        }
    }

    fun retry() {
        loadMovies(refresh = true)
    }

    fun onMovieClick(@Suppress("UNUSED_PARAMETER") movie: Movie) {
        // Handle movie click - navigation will be handled in the fragment
    }

    fun onFavoriteClick(@Suppress("UNUSED_PARAMETER") movie: Movie) {
        // TODO: Implement favorites functionality
    }
}

sealed class MoviesUiState {
    object Loading : MoviesUiState()
    object Success : MoviesUiState()
    object Empty : MoviesUiState()
    data class Error(val message: String) : MoviesUiState()
}