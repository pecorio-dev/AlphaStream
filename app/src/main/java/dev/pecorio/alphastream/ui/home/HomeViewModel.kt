package dev.pecorio.alphastream.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pecorio.alphastream.data.model.Movie
import dev.pecorio.alphastream.data.model.Series
import dev.pecorio.alphastream.data.repository.ContentRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<HomeUiState>()
    val uiState: LiveData<HomeUiState> = _uiState

    private val _latestMovies = MutableLiveData<List<Movie>>()
    val latestMovies: LiveData<List<Movie>> = _latestMovies

    private val _latestSeries = MutableLiveData<List<Series>>()
    val latestSeries: LiveData<List<Series>> = _latestSeries

    private val _trendingContent = MutableLiveData<List<Any>>()
    val trendingContent: LiveData<List<Any>> = _trendingContent

    private val _searchQuery = MutableLiveData<String>()
    val searchQuery: LiveData<String> = _searchQuery

    init {
        loadHomeContent()
    }

    fun loadHomeContent() {
        _uiState.value = HomeUiState.Loading
        
        viewModelScope.launch {
            try {
                val homeContentResult = contentRepository.getHomeContent()
                
                if (homeContentResult.isSuccess) {
                    val homeContent = homeContentResult.getOrNull()!!
                    
                    // Debug: Log movie URLs
                    homeContent.latestMovies.take(3).forEach { movie ->
                        android.util.Log.d("HomeViewModel", "Movie: ${movie.title}")
                        android.util.Log.d("HomeViewModel", "  uqloadOldUrl: ${movie.uqloadOldUrl}")
                        android.util.Log.d("HomeViewModel", "  uqloadNewUrl: ${movie.uqloadNewUrl}")
                        android.util.Log.d("HomeViewModel", "  streamUrl: ${movie.getStreamUrl()}")
                    }
                    
                    _latestMovies.value = homeContent.latestMovies
                    _latestSeries.value = homeContent.latestSeries
                    _trendingContent.value = homeContent.trending
                    
                    _uiState.value = HomeUiState.Success
                } else {
                    _uiState.value = HomeUiState.Error(
                        homeContentResult.exceptionOrNull()?.message ?: "Erreur inconnue"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun searchContent(query: String) {
        if (query.isBlank()) {
            _searchQuery.value = ""
            return
        }

        _searchQuery.value = query
        _uiState.value = HomeUiState.Loading

        viewModelScope.launch {
            try {
                val searchResult = contentRepository.searchContent(query, 1, 20)
                
                if (searchResult.isSuccess) {
                    val searchResponse = searchResult.getOrNull()!!
                    
                    // Convertir les résultats de recherche en films et séries
                    val movies = searchResponse.results.mapNotNull { it.toMovie() }
                    val series = searchResponse.results.mapNotNull { it.toSeries() }
                    
                    _latestMovies.value = movies
                    _latestSeries.value = series
                    _trendingContent.value = (movies + series).shuffled()
                    
                    _uiState.value = HomeUiState.Success
                } else {
                    _uiState.value = HomeUiState.Error(
                        searchResult.exceptionOrNull()?.message ?: "Erreur de recherche"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Erreur de recherche")
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        loadHomeContent()
    }

    fun retry() {
        if (_searchQuery.value.isNullOrBlank()) {
            loadHomeContent()
        } else {
            searchContent(_searchQuery.value!!)
        }
    }

    fun onMovieClick(movie: Movie) {
        // TODO: Navigation vers les détails du film
    }

    fun onSeriesClick(series: Series) {
        // TODO: Navigation vers les détails de la série
    }

    fun onTrendingItemClick(item: Any) {
        when (item) {
            is Movie -> onMovieClick(item)
            is Series -> onSeriesClick(item)
        }
    }

    fun onFavoriteMovieClick(movie: Movie) {
        // TODO: Gestion des favoris
    }

    fun onFavoriteSeriesClick(series: Series) {
        // TODO: Gestion des favoris
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    object Success : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}