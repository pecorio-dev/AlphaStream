package dev.pecorio.alphastream.ui.series.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pecorio.alphastream.data.model.EpisodeStreamsResponse
import dev.pecorio.alphastream.data.model.Series
import dev.pecorio.alphastream.data.repository.ContentRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeriesDetailsViewModel @Inject constructor(
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<SeriesDetailsUiState>(SeriesDetailsUiState.Loading)
    val uiState: LiveData<SeriesDetailsUiState> = _uiState

    private val _series = MutableLiveData<Series>()
    val series: LiveData<Series> = _series

    private val _episodeStreams = MutableLiveData<EpisodeStreamsResponse>()
    val episodeStreams: LiveData<EpisodeStreamsResponse> = _episodeStreams

    private val _streamingState = MutableLiveData<StreamingUiState>(StreamingUiState.Idle)
    val streamingState: LiveData<StreamingUiState> = _streamingState

    fun loadSeriesDetails(seriesId: String) {
        if (seriesId.isBlank()) {
            _uiState.value = SeriesDetailsUiState.Error("ID de série invalide")
            return
        }

        viewModelScope.launch {
            _uiState.value = SeriesDetailsUiState.Loading

            try {
                val result = contentRepository.getSeriesDetails(seriesId)
                
                if (result.isSuccess) {
                    val seriesData = result.getOrNull()!!
                    _series.value = seriesData
                    
                    // Vérifier si la série a des données valides
                    if (seriesData.title.isBlank()) {
                        _uiState.value = SeriesDetailsUiState.Error("Données de série incomplètes")
                    } else {
                        _uiState.value = SeriesDetailsUiState.Success
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Erreur lors du chargement des détails"
                    _uiState.value = SeriesDetailsUiState.Error(error)
                }
            } catch (e: Exception) {
                _uiState.value = SeriesDetailsUiState.Error("Erreur inattendue: ${e.message}")
            }
        }
    }

    fun loadEpisodeStreams(seriesId: String, seasonNumber: Int, episodeNumber: Int) {
        if (seriesId.isBlank() || seasonNumber < 1 || episodeNumber < 1) {
            _streamingState.value = StreamingUiState.Error("Paramètres d'épisode invalides")
            return
        }

        viewModelScope.launch {
            _streamingState.value = StreamingUiState.Loading

            try {
                val result = contentRepository.getEpisodeStreams(seriesId, seasonNumber, episodeNumber)
                
                if (result.isSuccess) {
                    val streams = result.getOrNull()!!
                    _episodeStreams.value = streams
                    
                    if (streams.hasStreams()) {
                        _streamingState.value = StreamingUiState.Success
                    } else {
                        _streamingState.value = StreamingUiState.Error("Aucun flux de streaming disponible")
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Erreur lors du chargement des flux"
                    _streamingState.value = StreamingUiState.Error(error)
                }
            } catch (e: Exception) {
                _streamingState.value = StreamingUiState.Error("Erreur inattendue: ${e.message}")
            }
        }
    }

    fun retry() {
        val currentSeries = _series.value
        if (currentSeries != null) {
            loadSeriesDetails(currentSeries.getSeriesId())
        }
    }

    fun retryStreaming(seriesId: String, seasonNumber: Int, episodeNumber: Int) {
        loadEpisodeStreams(seriesId, seasonNumber, episodeNumber)
    }

    fun clearStreamingState() {
        _streamingState.value = StreamingUiState.Idle
    }
}

sealed class SeriesDetailsUiState {
    object Loading : SeriesDetailsUiState()
    object Success : SeriesDetailsUiState()
    data class Error(val message: String) : SeriesDetailsUiState()
}

sealed class StreamingUiState {
    object Idle : StreamingUiState()
    object Loading : StreamingUiState()
    object Success : StreamingUiState()
    data class Error(val message: String) : StreamingUiState()
}