package dev.pecorio.alphastream.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.data.model.Favorite
import dev.pecorio.alphastream.data.model.FavoriteType
import dev.pecorio.alphastream.databinding.FragmentFavoritesBinding
import dev.pecorio.alphastream.ui.details.MovieDetailsActivity
import dev.pecorio.alphastream.ui.series.details.SeriesDetailsActivity
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoritesViewModel by viewModels()
    private lateinit var favoritesAdapter: FavoritesAdapter

    private var currentSnackbar: Snackbar? = null
    private var isSearchVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchBar()
        setupFilterChips()
        setupClickListeners()
        observeViewModel()
        
        // Load initial data
        viewModel.loadFavorites()
    }

    private fun setupRecyclerView() {
        favoritesAdapter = FavoritesAdapter(
            onItemClick = { favorite ->
                navigateToDetails(favorite)
            },
            onRemoveClick = { favorite ->
                showRemoveConfirmationDialog(favorite)
            },
            onPlayClick = { favorite ->
                // TODO: Navigate to video player
                showMessage("Lecture de ${favorite.title}")
            }
        )
        
        binding.favoritesRecyclerView.apply {
            adapter = favoritesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                viewModel.searchFavorites(query)
                
                binding.clearSearchButton.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
        })
        
        binding.clearSearchButton.setOnClickListener {
            binding.searchEditText.text?.clear()
        }
    }

    private fun setupFilterChips() {
        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chip_all -> viewModel.filterByType(null)
                R.id.chip_movies -> viewModel.filterByType(FavoriteType.MOVIE)
                R.id.chip_series -> viewModel.filterByType(FavoriteType.SERIES)
                R.id.chip_recently_added -> viewModel.loadRecentlyAdded()
                R.id.chip_completed -> viewModel.loadCompleted()
            }
        }
    }

    private fun setupClickListeners() {
        binding.searchButton.setOnClickListener {
            toggleSearchVisibility()
        }
        
        binding.browseContentButton.setOnClickListener {
            // Navigate to home tab
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.nav_home
        }
        
        binding.clearAllFab.setOnClickListener {
            showClearAllConfirmationDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is FavoritesUiState.Loading -> showLoading()
                is FavoritesUiState.Success -> showContent()
                is FavoritesUiState.Empty -> showEmptyState()
                is FavoritesUiState.Error -> showError(state.message)
            }
        }

        viewModel.favorites.observe(viewLifecycleOwner) { favorites ->
            favoritesAdapter.submitList(favorites)
            updateFavoritesCount(favorites.size)
            
            // Show/hide clear all FAB
            binding.clearAllFab.visibility = if (favorites.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun toggleSearchVisibility() {
        isSearchVisible = !isSearchVisible
        binding.searchCard.visibility = if (isSearchVisible) View.VISIBLE else View.GONE
        
        if (isSearchVisible) {
            binding.searchEditText.requestFocus()
        } else {
            binding.searchEditText.text?.clear()
            hideKeyboard()
        }
    }

    private fun navigateToDetails(favorite: Favorite) {
        when (favorite.type) {
            FavoriteType.MOVIE -> {
                // Convert Favorite to Movie for navigation
                val movie = dev.pecorio.alphastream.data.model.Movie(
                    title = favorite.title,
                    imageUrl = favorite.imageUrl,
                    remoteImageUrl = favorite.remoteImageUrl,
                    detailsLink = null,
                    uqloadOldUrl = null,
                    uqloadNewUrl = null,
                    tmdbId = null,
                    tmdbTitle = favorite.title,
                    tmdbOverview = favorite.synopsis,
                    tmdbReleaseDate = favorite.releaseDate,
                    tmdbVoteAverage = favorite.rating,
                    synopsis = favorite.synopsis,
                    originalTitle = favorite.title,
                    genres = favorite.getFormattedGenres(),
                    releaseDate = favorite.releaseDate,
                    rating = favorite.rating,
                    director = favorite.director,
                    directors = favorite.director?.let { listOf(it) },
                    cast = emptyList(),
                    language = null,
                    originalLanguage = null,
                    quality = null,
                    version = null,
                    scrapedAt = null
                )
                val intent = MovieDetailsActivity.newIntent(requireContext(), movie)
                startActivity(intent)
            }
            FavoriteType.SERIES -> {
                val intent = SeriesDetailsActivity.newIntent(
                    requireContext(),
                    favorite.id,
                    favorite.title
                )
                startActivity(intent)
            }
        }
    }

    private fun showRemoveConfirmationDialog(favorite: Favorite) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Retirer des favoris")
            .setMessage("Voulez-vous retirer \"${favorite.title}\" de vos favoris ?")
            .setPositiveButton("Retirer") { _, _ ->
                viewModel.removeFromFavorites(favorite.id)
                showMessage("${favorite.title} retiré des favoris")
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showClearAllConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Vider tous les favoris")
            .setMessage("Cette action supprimera tous vos favoris. Cette action est irréversible. Continuer ?")
            .setPositiveButton("Vider") { _, _ ->
                viewModel.clearAllFavorites()
                showMessage("Tous les favoris ont été supprimés")
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun updateFavoritesCount(count: Int) {
        binding.favoritesCount.text = when (count) {
            0 -> "Aucun élément"
            1 -> "1 élément"
            else -> "$count éléments"
        }
    }

    private fun showLoading() {
        binding.loadingLayout.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE
        binding.favoritesRecyclerView.visibility = View.GONE
    }

    private fun showContent() {
        binding.loadingLayout.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.favoritesRecyclerView.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        binding.loadingLayout.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.favoritesRecyclerView.visibility = View.GONE
        binding.clearAllFab.visibility = View.GONE
    }

    private fun showError(message: String) {
        showContent() // Show content even on error
        showMessage(message)
    }

    private fun showMessage(message: String) {
        currentSnackbar?.dismiss()
        currentSnackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        currentSnackbar?.show()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentSnackbar?.dismiss()
        currentSnackbar = null
        _binding = null
    }
}