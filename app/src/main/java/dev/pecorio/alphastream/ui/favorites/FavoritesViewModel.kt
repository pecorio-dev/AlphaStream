package dev.pecorio.alphastream.ui.favorites

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pecorio.alphastream.data.model.Favorite
import dev.pecorio.alphastream.data.model.FavoriteType
import dev.pecorio.alphastream.data.repository.FavoritesRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<FavoritesUiState>(FavoritesUiState.Loading)
    val uiState: LiveData<FavoritesUiState> = _uiState

    private val _favorites = MutableLiveData<List<Favorite>>()
    val favorites: LiveData<List<Favorite>> = _favorites

    private var currentFilter: FavoriteType? = null
    private var currentSearchQuery: String = ""

    fun loadFavorites() {
        viewModelScope.launch {
            _uiState.value = FavoritesUiState.Loading
            
            try {
                favoritesRepository.getAllFavorites()
                    .catch { exception ->
                        _uiState.value = FavoritesUiState.Error("Erreur lors du chargement des favoris: ${exception.message}")
                    }
                    .collect { favoritesList ->
                        if (favoritesList.isEmpty()) {
                            _uiState.value = FavoritesUiState.Empty
                        } else {
                            _uiState.value = FavoritesUiState.Success
                        }
                        _favorites.value = favoritesList
                    }
            } catch (e: Exception) {
                _uiState.value = FavoritesUiState.Error("Erreur inattendue: ${e.message}")
            }
        }
    }

    fun filterByType(type: FavoriteType?) {
        currentFilter = type
        viewModelScope.launch {
            _uiState.value = FavoritesUiState.Loading
            
            try {
                val flow = if (type != null) {
                    favoritesRepository.getFavoritesByType(type)
                } else {
                    favoritesRepository.getAllFavorites()
                }
                
                flow.catch { exception ->
                    _uiState.value = FavoritesUiState.Error("Erreur lors du filtrage: ${exception.message}")
                }.collect { favoritesList ->
                    if (favoritesList.isEmpty()) {
                        _uiState.value = FavoritesUiState.Empty
                    } else {
                        _uiState.value = FavoritesUiState.Success
                    }
                    _favorites.value = favoritesList
                }
            } catch (e: Exception) {
                _uiState.value = FavoritesUiState.Error("Erreur inattendue: ${e.message}")
            }
        }
    }

    fun searchFavorites(query: String) {
        currentSearchQuery = query
        
        if (query.isBlank()) {
            // If search is empty, reload based on current filter
            if (currentFilter != null) {
                filterByType(currentFilter)
            } else {
                loadFavorites()
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = FavoritesUiState.Loading
            
            try {
                favoritesRepository.searchFavorites(query)
                    .catch { exception ->
                        _uiState.value = FavoritesUiState.Error("Erreur lors de la recherche: ${exception.message}")
                    }
                    .collect { favoritesList ->
                        if (favoritesList.isEmpty()) {
                            _uiState.value = FavoritesUiState.Empty
                        } else {
                            _uiState.value = FavoritesUiState.Success
                        }
                        _favorites.value = favoritesList
                    }
            } catch (e: Exception) {
                _uiState.value = FavoritesUiState.Error("Erreur inattendue: ${e.message}")
            }
        }
    }

    fun loadRecentlyAdded() {
        viewModelScope.launch {
            _uiState.value = FavoritesUiState.Loading
            
            try {
                favoritesRepository.getRecentlyAdded(7) // Last 7 days
                    .catch { exception ->
                        _uiState.value = FavoritesUiState.Error("Erreur lors du chargement: ${exception.message}")
                    }
                    .collect { favoritesList ->
                        if (favoritesList.isEmpty()) {
                            _uiState.value = FavoritesUiState.Empty
                        } else {
                            _uiState.value = FavoritesUiState.Success
                        }
                        _favorites.value = favoritesList
                    }
            } catch (e: Exception) {
                _uiState.value = FavoritesUiState.Error("Erreur inattendue: ${e.message}")
            }
        }
    }

    fun loadCompleted() {
        viewModelScope.launch {
            _uiState.value = FavoritesUiState.Loading
            
            try {
                favoritesRepository.getAllFavorites()
                    .catch { exception ->
                        _uiState.value = FavoritesUiState.Error("Erreur lors du chargement: ${exception.message}")
                    }
                    .collect { allFavorites ->
                        val completedFavorites = allFavorites.filter { it.isCompleted }
                        
                        if (completedFavorites.isEmpty()) {
                            _uiState.value = FavoritesUiState.Empty
                        } else {
                            _uiState.value = FavoritesUiState.Success
                        }
                        _favorites.value = completedFavorites
                    }
            } catch (e: Exception) {
                _uiState.value = FavoritesUiState.Error("Erreur inattendue: ${e.message}")
            }
        }
    }

    fun removeFromFavorites(favoriteId: String) {
        viewModelScope.launch {
            try {
                val result = favoritesRepository.removeFromFavorites(favoriteId)
                if (result.isFailure) {
                    _uiState.value = FavoritesUiState.Error("Erreur lors de la suppression: ${result.exceptionOrNull()?.message}")
                }
                // The list will be automatically updated through the Flow
            } catch (e: Exception) {
                _uiState.value = FavoritesUiState.Error("Erreur inattendue: ${e.message}")
            }
        }
    }

    fun clearAllFavorites() {
        viewModelScope.launch {
            try {
                val result = favoritesRepository.clearAllFavorites()
                if (result.isFailure) {
                    _uiState.value = FavoritesUiState.Error("Erreur lors de la suppression: ${result.exceptionOrNull()?.message}")
                }
                // The list will be automatically updated through the Flow
            } catch (e: Exception) {
                _uiState.value = FavoritesUiState.Error("Erreur inattendue: ${e.message}")
            }
        }
    }

    fun markAsCompleted(favoriteId: String, completed: Boolean = true) {
        viewModelScope.launch {
            try {
                val result = favoritesRepository.markAsCompleted(favoriteId, completed)
                if (result.isFailure) {
                    _uiState.value = FavoritesUiState.Error("Erreur lors de la mise à jour: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _uiState.value = FavoritesUiState.Error("Erreur inattendue: ${e.message}")
            }
        }
    }

    fun updateWatchProgress(favoriteId: String, progress: Float) {
        viewModelScope.launch {
            try {
                val result = favoritesRepository.updateWatchProgress(favoriteId, progress)
                if (result.isFailure) {
                    _uiState.value = FavoritesUiState.Error("Erreur lors de la mise à jour: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _uiState.value = FavoritesUiState.Error("Erreur inattendue: ${e.message}")
            }
        }
    }
}

sealed class FavoritesUiState {
    object Loading : FavoritesUiState()
    object Success : FavoritesUiState()
    object Empty : FavoritesUiState()
    data class Error(val message: String) : FavoritesUiState()
}