package dev.pecorio.alphastream.ui.series

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pecorio.alphastream.data.model.Series
import dev.pecorio.alphastream.data.repository.ContentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<SeriesUiState>(SeriesUiState.Loading)
    val uiState: LiveData<SeriesUiState> = _uiState

    private val _series = MutableLiveData<List<Series>>()
    val series: LiveData<List<Series>> = _series

    private var searchJob: Job? = null
    private var currentPage = 1
    private var isLoading = false
    private var hasMorePages = true
    private var currentFilter = "all"
    private var currentSearchQuery = ""
    private var allSeries: List<Series> = emptyList()

    init {
        loadSeries()
    }

    fun loadSeries(refresh: Boolean = false) {
        if (refresh) {
            currentPage = 1
            hasMorePages = true
            allSeries = emptyList()
        }

        if (isLoading) return

        viewModelScope.launch {
            isLoading = true
            if (refresh || allSeries.isEmpty()) {
                _uiState.value = SeriesUiState.Loading
            }

            try {
                val result = contentRepository.getSeries(currentPage, 50)
                
                if (result.isSuccess) {
                    val seriesResponse = result.getOrNull()!!
                    val newSeries = seriesResponse.series
                    
                    allSeries = if (refresh || currentPage == 1) {
                        newSeries
                    } else {
                        allSeries + newSeries
                    }
                    
                    // Check if there are more pages
                    hasMorePages = newSeries.size >= 50
                    currentPage++
                    
                    // Apply current filters
                    applyFilters()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Erreur lors du chargement des séries"
                    _uiState.value = SeriesUiState.Error(error)
                }
            } catch (e: Exception) {
                _uiState.value = SeriesUiState.Error(e.message ?: "Erreur inconnue")
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMoreSeries() {
        if (!isLoading && hasMorePages) {
            loadSeries(refresh = false)
        }
    }

    fun searchSeries(query: String) {
        currentSearchQuery = query
        
        // Cancel previous search
        searchJob?.cancel()
        
        if (query.isBlank()) {
            // Si la recherche est vide, afficher toutes les séries
            currentSearchQuery = ""
            applyFilters()
            return
        }
        
        searchJob = viewModelScope.launch {
            // Add debounce delay
            delay(300)
            
            try {
                _uiState.value = SeriesUiState.Loading
                
                // Utiliser l'API de recherche
                val result = contentRepository.searchContent(query)
                
                if (result.isSuccess) {
                    val searchResponse = result.getOrNull()!!
                    // Filtrer seulement les séries et convertir
                    val searchSeries = searchResponse.results
                        .filter { it.isSeries() }
                        .mapNotNull { it.toSeries() }
                    
                    // Remplacer temporairement la liste des séries par les résultats de recherche
                    _series.value = searchSeries
                    
                    if (searchSeries.isEmpty()) {
                        _uiState.value = SeriesUiState.Empty
                    } else {
                        _uiState.value = SeriesUiState.Success
                    }
                } else {
                    // En cas d'erreur API, faire une recherche locale sur les séries déjà chargées
                    performLocalSearch(query)
                }
            } catch (e: Exception) {
                // En cas d'exception, faire une recherche locale sur les séries déjà chargées
                performLocalSearch(query)
            }
        }
    }
    
    private fun performLocalSearch(query: String) {
        val filteredSeries = allSeries.filter { series ->
            series.getDisplayTitle().contains(query, ignoreCase = true) ||
            series.getDisplayOverview()?.contains(query, ignoreCase = true) == true ||
            series.getFormattedGenres()?.contains(query, ignoreCase = true) == true
        }
        
        _series.value = filteredSeries
        
        if (filteredSeries.isEmpty()) {
            _uiState.value = SeriesUiState.Empty
        } else {
            _uiState.value = SeriesUiState.Success
        }
    }

    fun setFilter(filter: String) {
        currentFilter = filter
        // Si on change de filtre, on revient aux séries locales
        if (currentSearchQuery.isNotEmpty()) {
            currentSearchQuery = ""
        }
        applyFilters()
    }

    private fun applyFilters() {
        var filteredSeries = allSeries

        // Apply search filter seulement si on n'est pas en mode recherche API
        if (currentSearchQuery.isNotEmpty()) {
            filteredSeries = filteredSeries.filter { series ->
                series.getDisplayTitle().contains(currentSearchQuery, ignoreCase = true) ||
                series.getDisplayOverview()?.contains(currentSearchQuery, ignoreCase = true) == true ||
                series.getFormattedGenres()?.contains(currentSearchQuery, ignoreCase = true) == true
            }
        }

        // Apply genre filter
        if (currentFilter != "all") {
            filteredSeries = filteredSeries.filter { series ->
                series.getFormattedGenres()?.contains(currentFilter, ignoreCase = true) == true
            }
        }

        _series.value = filteredSeries
        
        if (filteredSeries.isEmpty() && allSeries.isNotEmpty()) {
            _uiState.value = SeriesUiState.Empty
        } else if (filteredSeries.isEmpty()) {
            _uiState.value = SeriesUiState.Loading
        } else {
            _uiState.value = SeriesUiState.Success
        }
    }

    fun retry() {
        loadSeries(refresh = true)
    }

    fun onSeriesClick(@Suppress("UNUSED_PARAMETER") series: Series) {
        // Handle series click - navigation will be handled in the fragment
    }

    fun onFavoriteClick(@Suppress("UNUSED_PARAMETER") series: Series) {
        // TODO: Implement favorites functionality
    }
}

sealed class SeriesUiState {
    object Loading : SeriesUiState()
    object Success : SeriesUiState()
    object Empty : SeriesUiState()
    data class Error(val message: String) : SeriesUiState()
}