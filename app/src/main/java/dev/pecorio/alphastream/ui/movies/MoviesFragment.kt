package dev.pecorio.alphastream.ui.movies

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.databinding.FragmentMoviesBinding
import dev.pecorio.alphastream.ui.details.MovieDetailsActivity
import dev.pecorio.alphastream.ui.tv.TVNavigationHelper

@AndroidEntryPoint
class MoviesFragment : Fragment() {

    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MoviesViewModel by viewModels()
    private var currentSnackbar: Snackbar? = null
    private lateinit var tvNavigationHelper: TVNavigationHelper

    private lateinit var moviesAdapter: MoviesGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoviesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupTVNavigation()
        setupRecyclerView()
        setupSearchInput()
        setupObservers()
        setupClickListeners()
        setupSwipeRefresh()
    }
    
    private fun setupTVNavigation() {
        tvNavigationHelper = TVNavigationHelper(requireContext())
        
        // Configurer la navigation TV pour toute l'interface
        tvNavigationHelper.setupTVViewGroup(binding.root as ViewGroup)
        
        // Configurer le RecyclerView
        tvNavigationHelper.setupTVRecyclerView(binding.moviesRecyclerView)
        
        // Configurer les éléments de recherche et filtres
        tvNavigationHelper.setupTVEditText(binding.searchEditText)
        tvNavigationHelper.setupTVCompatibility(binding.clearSearchButton)
        
        // Améliorer la navigation pour la recherche
        binding.searchEditText.nextFocusDownId = binding.chipAll.id
        binding.clearSearchButton.nextFocusDownId = binding.chipAll.id
        
        // Configurer les chips de filtre
        listOf(
            binding.chipAll,
            binding.chipAction,
            binding.chipComedy,
            binding.chipDrama,
            binding.chipHorror
        ).forEach { chip ->
            tvNavigationHelper.setupTVCompatibility(chip)
        }
    }

    private fun setupRecyclerView() {
        moviesAdapter = MoviesGridAdapter(
            onMovieClick = { movie ->
                val intent = MovieDetailsActivity.newIntent(requireContext(), movie)
                startActivity(intent)
            },
            onFavoriteClick = { movie ->
                viewModel.onFavoriteClick(movie)
            }
        )
        
        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        
        binding.moviesRecyclerView.apply {
            adapter = moviesAdapter
            layoutManager = gridLayoutManager
            
            // Add scroll listener for pagination
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as GridLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    // Load more when reaching the end
                    if (visibleItemCount + firstVisibleItemPosition >= totalItemCount - 4) {
                        viewModel.loadMoreMovies()
                    }
                }
            })
        }
    }

    private fun setupSearchInput() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                
                // Show/hide clear button
                binding.clearSearchButton.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                
                // Perform search
                viewModel.searchMovies(query)
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MoviesUiState.Loading -> showLoadingState()
                is MoviesUiState.Success -> showSuccessState()
                is MoviesUiState.Error -> showErrorState(state.message)
                is MoviesUiState.Empty -> showEmptyState()
            }
        }

        viewModel.movies.observe(viewLifecycleOwner) { movies ->
            moviesAdapter.submitList(movies)
        }
    }

    private fun setupClickListeners() {
        binding.clearSearchButton.setOnClickListener {
            binding.searchEditText.text?.clear()
        }

        binding.retryButton.setOnClickListener {
            viewModel.retry()
        }

        // Filter chips
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setFilter("all")
        }
        
        binding.chipAction.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setFilter("action")
        }
        
        binding.chipComedy.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setFilter("comedy")
        }
        
        binding.chipDrama.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setFilter("drama")
        }
        
        binding.chipHorror.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setFilter("horror")
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadMovies(refresh = true)
        }
        
        // Set colors for swipe refresh
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.primary_blue,
            R.color.accent_green,
            R.color.accent_orange
        )
    }

    private fun showLoadingState() {
        binding.loadingLayout.visibility = View.VISIBLE
        binding.moviesRecyclerView.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.errorStateLayout.visibility = View.GONE
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun showSuccessState() {
        binding.loadingLayout.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.errorStateLayout.visibility = View.GONE
        binding.moviesRecyclerView.visibility = View.VISIBLE
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun showEmptyState() {
        binding.loadingLayout.visibility = View.GONE
        binding.moviesRecyclerView.visibility = View.GONE
        binding.errorStateLayout.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun showErrorState(message: String) {
        binding.loadingLayout.visibility = View.GONE
        binding.moviesRecyclerView.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.errorStateLayout.visibility = View.VISIBLE
        binding.swipeRefreshLayout.isRefreshing = false
        
        binding.errorMessage.text = message
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