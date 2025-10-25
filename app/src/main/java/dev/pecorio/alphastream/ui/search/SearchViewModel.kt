package dev.pecorio.alphastream.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pecorio.alphastream.data.model.SearchResult
import dev.pecorio.alphastream.data.repository.ContentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<SearchUiState>(SearchUiState.Idle)
    val uiState: LiveData<SearchUiState> = _uiState

    private val _recentSearches = MutableLiveData<List<String>>()
    val recentSearches: LiveData<List<String>> = _recentSearches

    private var searchJob: Job? = null
    private var currentFilter = "all"
    private var allResults: List<SearchResult> = emptyList()

    // Simple in-memory storage for recent searches (in a real app, use SharedPreferences or Room)
    private val recentSearchesList = mutableListOf<String>()

    fun search(query: String) {
        // Cancel previous search
        searchJob?.cancel()
        
        searchJob = viewModelScope.launch {
            // Add debounce delay
            delay(300)
            
            _uiState.value = SearchUiState.Loading
            
            try {
                val result = contentRepository.searchContent(query)
                
                if (result.isSuccess) {
                    val searchResponse = result.getOrNull()!!
                    allResults = searchResponse.results
                    
                    // Add to recent searches
                    addRecentSearch(query)
                    
                    // Apply current filter
                    applyFilter()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Erreur lors de la recherche"
                    _uiState.value = SearchUiState.Error(error)
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun setFilter(filter: String) {
        currentFilter = filter
        applyFilter()
    }

    private fun applyFilter() {
        val filteredResults = when (currentFilter) {
            "movies" -> allResults.filter { it.type == "movie" }
            "series" -> allResults.filter { it.type == "series" }
            else -> allResults
        }
        
        if (filteredResults.isEmpty()) {
            _uiState.value = SearchUiState.Empty
        } else {
            _uiState.value = SearchUiState.Success(filteredResults)
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        allResults = emptyList()
        _uiState.value = SearchUiState.Idle
    }

    fun loadRecentSearches() {
        _recentSearches.value = recentSearchesList.toList()
    }

    private fun addRecentSearch(query: String) {
        // Remove if already exists
        recentSearchesList.remove(query)
        // Add to beginning
        recentSearchesList.add(0, query)
        // Keep only last 10 searches
        if (recentSearchesList.size > 10) {
            recentSearchesList.removeAt(recentSearchesList.size - 1)
        }
        _recentSearches.value = recentSearchesList.toList()
    }

    fun removeRecentSearch(query: String) {
        recentSearchesList.remove(query)
        _recentSearches.value = recentSearchesList.toList()
    }

    fun clearRecentSearches() {
        recentSearchesList.clear()
        _recentSearches.value = emptyList()
    }
}

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val results: List<SearchResult>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
    object Empty : SearchUiState()
}