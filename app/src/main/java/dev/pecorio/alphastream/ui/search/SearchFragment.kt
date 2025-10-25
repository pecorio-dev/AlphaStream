package dev.pecorio.alphastream.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.databinding.FragmentSearchBinding
import dev.pecorio.alphastream.ui.details.MovieDetailsActivity
import dev.pecorio.alphastream.ui.series.details.SeriesDetailsActivity
import dev.pecorio.alphastream.ui.tv.TVNavigationHelper

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var tvNavigationHelper: TVNavigationHelper
    private lateinit var searchResultsAdapter: SearchResultsAdapter
    private lateinit var recentSearchesAdapter: RecentSearchesAdapter
    private var currentSnackbar: Snackbar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupTVNavigation()
        setupRecyclerViews()
        setupSearchInput()
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupTVNavigation() {
        tvNavigationHelper = TVNavigationHelper(requireContext())
        
        // Configurer la navigation TV pour toute l'interface
        tvNavigationHelper.setupTVViewGroup(binding.root as ViewGroup)
        
        // Configurer les RecyclerViews
        tvNavigationHelper.setupTVRecyclerView(binding.searchResultsRecyclerView)
        tvNavigationHelper.setupTVRecyclerView(binding.recentSearchesRecyclerView)
        
        // Configurer les éléments de recherche
        tvNavigationHelper.setupTVEditText(binding.searchEditText)
        tvNavigationHelper.setupTVCompatibility(binding.clearSearchButton)
        
        // Améliorer la navigation pour la recherche
        binding.searchEditText.nextFocusDownId = binding.chipAll.id
        binding.clearSearchButton.nextFocusDownId = binding.chipAll.id
        
        // Configurer les chips de filtre
        listOf(
            binding.chipAll,
            binding.chipMovies,
            binding.chipSeries
        ).forEach { chip ->
            tvNavigationHelper.setupTVCompatibility(chip)
        }
    }

    private fun setupRecyclerViews() {
        // Search Results RecyclerView
        searchResultsAdapter = SearchResultsAdapter { searchResult ->
            when (searchResult.type) {
                "movie" -> {
                    // Convert SearchResult to Movie and open details
                    val movie = searchResult.toMovie()
                    val intent = MovieDetailsActivity.newIntent(requireContext(), movie)
                    startActivity(intent)
                }
                "series" -> {
                    // Convert SearchResult to Series and open details
                    val series = searchResult.toSeries()
                    if (series != null) {
                        val intent = SeriesDetailsActivity.newIntent(requireContext(), series.id ?: "")
                        startActivity(intent)
                    } else {
                        showMessage("Impossible d'ouvrir les détails de cette série")
                    }
                }
            }
        }
        
        binding.searchResultsRecyclerView.apply {
            adapter = searchResultsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Recent Searches RecyclerView
        recentSearchesAdapter = RecentSearchesAdapter(
            onSearchClick = { query ->
                binding.searchEditText.setText(query)
                viewModel.search(query)
            },
            onRemoveClick = { query ->
                viewModel.removeRecentSearch(query)
            }
        )
        
        binding.recentSearchesRecyclerView.apply {
            adapter = recentSearchesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSearchInput() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                
                // Show/hide clear button
                binding.clearSearchButton.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                
                // Perform search if query is not empty
                if (query.isNotEmpty() && query.length >= 2) {
                    viewModel.search(query)
                } else if (query.isEmpty()) {
                    viewModel.clearSearch()
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SearchUiState.Idle -> showIdleState()
                is SearchUiState.Loading -> showLoadingState()
                is SearchUiState.Success -> showSearchResults(state.results)
                is SearchUiState.Error -> showError(state.message)
                is SearchUiState.Empty -> showEmptyState()
            }
        }

        viewModel.recentSearches.observe(viewLifecycleOwner) { searches ->
            recentSearchesAdapter.submitList(searches)
            binding.recentSearchesLayout.visibility = if (searches.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.clearSearchButton.setOnClickListener {
            binding.searchEditText.text?.clear()
        }

        binding.clearHistoryButton.setOnClickListener {
            viewModel.clearRecentSearches()
        }

        // Filter chips
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setFilter("all")
        }
        
        binding.chipMovies.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setFilter("movies")
        }
        
        binding.chipSeries.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setFilter("series")
        }
    }

    private fun showIdleState() {
        binding.loadingLayout.visibility = View.GONE
        binding.searchResultsRecyclerView.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.recentSearchesLayout.visibility = View.VISIBLE
    }

    private fun showLoadingState() {
        binding.loadingLayout.visibility = View.VISIBLE
        binding.searchResultsRecyclerView.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.recentSearchesLayout.visibility = View.GONE
    }

    private fun showSearchResults(results: List<dev.pecorio.alphastream.data.model.SearchResult>) {
        binding.loadingLayout.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.recentSearchesLayout.visibility = View.GONE
        binding.searchResultsRecyclerView.visibility = View.VISIBLE
        
        // Filter out null results and ensure list is not empty
        val validResults = results.filter { it.title.isNotBlank() }
        if (validResults.isNotEmpty()) {
            searchResultsAdapter.submitList(validResults)
        } else {
            showEmptyState()
        }
    }

    private fun showEmptyState() {
        binding.loadingLayout.visibility = View.GONE
        binding.searchResultsRecyclerView.visibility = View.GONE
        binding.recentSearchesLayout.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        binding.loadingLayout.visibility = View.GONE
        
        // Dismiss any existing snackbar first
        currentSnackbar?.dismiss()
        
        currentSnackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.retry)) {
                val query = binding.searchEditText.text?.toString()?.trim()
                if (!query.isNullOrEmpty()) {
                    viewModel.search(query)
                }
            }
            .setBackgroundTint(resources.getColor(R.color.error, null))
            .setTextColor(resources.getColor(R.color.white, null))
            .setActionTextColor(resources.getColor(R.color.white, null))
        
        currentSnackbar?.show()
    }

    private fun showMessage(message: String) {
        currentSnackbar?.dismiss()
        currentSnackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        currentSnackbar?.show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up snackbar to prevent window leaks
        currentSnackbar?.dismiss()
        currentSnackbar = null
        _binding = null
    }
    
    override fun onPause() {
        super.onPause()
        // Dismiss any showing snackbar to prevent window leaks
        currentSnackbar?.dismiss()
    }
}