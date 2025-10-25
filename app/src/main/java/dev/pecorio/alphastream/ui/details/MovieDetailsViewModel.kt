package dev.pecorio.alphastream.ui.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pecorio.alphastream.data.extractor.ExtractedStreamInfo
import dev.pecorio.alphastream.data.extractor.FastDirectExtractor
import dev.pecorio.alphastream.data.model.Movie
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieDetailsViewModel @Inject constructor(
    private val fastDirectExtractor: FastDirectExtractor
) : ViewModel() {

    private val _uiState = MutableLiveData<MovieDetailsUiState>(MovieDetailsUiState.Idle)
    val uiState: LiveData<MovieDetailsUiState> = _uiState

    private val _isFavorite = MutableLiveData<Boolean>()
    val isFavorite: LiveData<Boolean> = _isFavorite

    // Simple in-memory favorites storage (in a real app, use Room database)
    private val favoriteMovies = mutableSetOf<String>()
    
    // Store resume information temporarily
    private var resumeInfo: Pair<Movie, Long>? = null

    fun extractStreamInfo(streamUrl: String) {
        _uiState.value = MovieDetailsUiState.Loading
        
        viewModelScope.launch {
            try {
                val result = fastDirectExtractor.extractStreamInfo(streamUrl)
                
                if (result.isSuccess) {
                    val streamInfo = result.getOrNull()!!
                    _uiState.value = MovieDetailsUiState.Success(streamInfo)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Erreur lors de l'extraction du lien vidéo"
                    _uiState.value = MovieDetailsUiState.Error(error)
                }
            } catch (e: Exception) {
                _uiState.value = MovieDetailsUiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun checkIfFavorite(movie: Movie) {
        val movieId = movie.title + movie.tmdbId
        _isFavorite.value = favoriteMovies.contains(movieId)
    }

    fun toggleFavorite(movie: Movie) {
        val movieId = movie.title + movie.tmdbId
        val currentlyFavorite = favoriteMovies.contains(movieId)
        
        if (currentlyFavorite) {
            favoriteMovies.remove(movieId)
            _isFavorite.value = false
        } else {
            favoriteMovies.add(movieId)
            _isFavorite.value = true
        }
    }
    
    fun setResumeInfo(movie: Movie, startPosition: Long) {
        resumeInfo = Pair(movie, startPosition)
    }
    
    fun getResumeInfo(): Pair<Movie, Long>? {
        return resumeInfo
    }
    
    fun clearResumeInfo() {
        resumeInfo = null
    }
}

sealed class MovieDetailsUiState {
    object Idle : MovieDetailsUiState()
    object Loading : MovieDetailsUiState()
    data class Success(val streamInfo: ExtractedStreamInfo) : MovieDetailsUiState()
    data class Error(val message: String) : MovieDetailsUiState()
}