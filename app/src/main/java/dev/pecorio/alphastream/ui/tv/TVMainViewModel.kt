package dev.pecorio.alphastream.ui.tv

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pecorio.alphastream.data.model.Movie
import dev.pecorio.alphastream.data.model.Series
import dev.pecorio.alphastream.data.repository.ContentRepository
import dev.pecorio.alphastream.data.repository.FavoritesRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TVMainViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {
    
    private val _content = MutableLiveData<List<Any>>()
    val content: LiveData<List<Any>> = _content
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadContent() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Charger un mélange de films et séries
                val movies = loadMoviesInternal()
                val series = loadSeriesInternal()
                
                // Mélanger le contenu
                val mixedContent = mutableListOf<Any>()
                val maxItems = maxOf(movies.size, series.size)
                
                for (i in 0 until maxItems) {
                    if (i < movies.size) mixedContent.add(movies[i])
                    if (i < series.size) mixedContent.add(series[i])
                }
                
                _content.value = mixedContent.take(20) // Limiter à 20 éléments
                
            } catch (e: Exception) {
                _error.value = "Erreur lors du chargement: ${e.message}"
                _content.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadMovies() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val movies = loadMoviesInternal()
                _content.value = movies
                
            } catch (e: Exception) {
                _error.value = "Erreur lors du chargement des films: ${e.message}"
                _content.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadSeries() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val series = loadSeriesInternal()
                _content.value = series
                
            } catch (e: Exception) {
                _error.value = "Erreur lors du chargement des séries: ${e.message}"
                _content.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // TODO: Implémenter le chargement des favoris
                // Pour l'instant, retourner une liste vide
                _content.value = emptyList()
                
            } catch (e: Exception) {
                _error.value = "Erreur lors du chargement des favoris: ${e.message}"
                _content.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun loadMoviesInternal(): List<Movie> {
        return try {
            val result = contentRepository.getMovies(page = 1, limit = 20)
            if (result.isSuccess) {
                result.getOrNull()?.movies ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("TVMainViewModel", "Error loading movies: ${e.message}")
            emptyList()
        }
    }
    
    private suspend fun loadSeriesInternal(): List<Series> {
        return try {
            val result = contentRepository.getSeries(page = 1, limit = 20)
            if (result.isSuccess) {
                result.getOrNull()?.series ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("TVMainViewModel", "Error loading series: ${e.message}")
            emptyList()
        }
    }
    
    fun retry() {
        loadContent()
    }
}