package dev.pecorio.alphastream.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.databinding.FragmentHomeBinding
import dev.pecorio.alphastream.ui.adapters.TrendingAdapter
import dev.pecorio.alphastream.ui.adapters.HomeMoviesAdapter
import dev.pecorio.alphastream.ui.adapters.HomeSeriesAdapter

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private var currentSnackbar: Snackbar? = null

    private lateinit var trendingAdapter: TrendingAdapter
    private lateinit var moviesAdapter: HomeMoviesAdapter
    private lateinit var seriesAdapter: HomeSeriesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerViews()
        setupObservers()
        setupSearchBar()
        setupClickListeners()
        
        // Load initial data
        viewModel.loadHomeContent()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top)
            insets
        }
    }

    private fun setupRecyclerViews() {
        // Trending RecyclerView
        trendingAdapter = TrendingAdapter { item ->
            when (item) {
                is dev.pecorio.alphastream.data.model.Movie -> {
                    val intent = dev.pecorio.alphastream.ui.details.MovieDetailsActivity.newIntent(requireContext(), item)
                    startActivity(intent)
                }
                is dev.pecorio.alphastream.data.model.Series -> {
                    val seriesId = item.id
                    android.util.Log.d("HomeFragment", "Clic sur série trending: titre='${item.title}', id='${item.id}'")
                    
                    if (!seriesId.isNullOrBlank()) {
                        val intent = dev.pecorio.alphastream.ui.series.details.SeriesDetailsActivity.newIntent(
                            requireContext(),
                            seriesId,
                            item.getDisplayTitle()
                        )
                        startActivity(intent)
                    } else {
                        showMessage("Cette série n'a pas d'ID valide")
                    }
                }
            }
        }
        
        binding.trendingRecyclerView.apply {
            adapter = trendingAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }
        
        // Movies RecyclerView
        moviesAdapter = HomeMoviesAdapter { movie ->
            val intent = dev.pecorio.alphastream.ui.details.MovieDetailsActivity.newIntent(requireContext(), movie)
            startActivity(intent)
        }
        
        binding.moviesRecyclerView.apply {
            adapter = moviesAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }
        
        // Series RecyclerView
        seriesAdapter = HomeSeriesAdapter { series ->
            val seriesId = series.id
            android.util.Log.d("HomeFragment", "Clic sur série home: titre='${series.title}', id='${series.id}'")
            
            if (!seriesId.isNullOrBlank()) {
                val intent = dev.pecorio.alphastream.ui.series.details.SeriesDetailsActivity.newIntent(
                    requireContext(),
                    seriesId,
                    series.getDisplayTitle()
                )
                startActivity(intent)
            } else {
                showMessage("Cette série n'a pas d'ID valide")
            }
        }
        
        binding.seriesRecyclerView.apply {
            adapter = seriesAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }
    }

    private fun setupSearchBar() {
        binding.searchBar.setOnClickListener {
            // Navigate to search fragment
            findNavController().navigate(R.id.nav_search)
        }
    }

    private fun setupClickListeners() {
        // Boutons "Voir tout"
        binding.trendingSeeAll.setOnClickListener {
            // TODO: Navigation vers la page de tendances
        }
        
        binding.moviesSeeAll.setOnClickListener {
            // Navigation vers l'onglet films
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.nav_movies
        }
        
        binding.seriesSeeAll.setOnClickListener {
            // Navigation vers l'onglet séries
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.nav_series
        }
        
        binding.settingsButton.setOnClickListener {
            // Navigation vers les paramètres
            findNavController().navigate(R.id.nav_settings)
        }
        
        // Bouton retry
        binding.retryButton.setOnClickListener {
            viewModel.retry()
        }
    }

    private fun setupObservers() {
        // État de l'UI
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is HomeUiState.Loading -> showLoading()
                is HomeUiState.Success -> showContent()
                is HomeUiState.Error -> showError(state.message)
            }
        }

        // Contenu trending
        viewModel.trendingContent.observe(viewLifecycleOwner) { content ->
            trendingAdapter.submitList(content)
        }

        // Films
        viewModel.latestMovies.observe(viewLifecycleOwner) { movies ->
            moviesAdapter.submitList(movies.take(10)) // Limiter à 10 films pour la page d'accueil
        }
        
        // Séries
        viewModel.latestSeries.observe(viewLifecycleOwner) { series ->
            seriesAdapter.submitList(series.take(10)) // Limiter à 10 séries pour la page d'accueil
        }
    }

    private fun showLoading() {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.errorState.visibility = View.GONE
        
        // Animation de l'overlay de chargement
        binding.loadingOverlay.alpha = 0f
        binding.loadingOverlay.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    private fun showContent() {
        binding.loadingOverlay.visibility = View.GONE
        binding.errorState.visibility = View.GONE
        
        // Animation d'apparition du contenu
        binding.root.alpha = 0f
        binding.root.animate()
            .alpha(1f)
            .setDuration(400)
            .start()
    }

    private fun showError(message: String) {
        binding.loadingOverlay.visibility = View.GONE
        binding.errorState.visibility = View.VISIBLE
        
        // Dismiss any existing snackbar first
        currentSnackbar?.dismiss()
        
        // Afficher un Snackbar avec le message d'erreur
        currentSnackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.retry)) {
                viewModel.retry()
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