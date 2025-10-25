package dev.pecorio.alphastream.ui.series.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.data.model.Season
import dev.pecorio.alphastream.data.model.Series
import dev.pecorio.alphastream.databinding.ActivitySeriesDetailsBinding

@AndroidEntryPoint
class SeriesDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeriesDetailsBinding
    private val viewModel: SeriesDetailsViewModel by viewModels()
    private var currentSnackbar: Snackbar? = null

    private lateinit var seasonsAdapter: SeasonsAdapter

    companion object {
        private const val EXTRA_SERIES_ID = "extra_series_id"
        private const val EXTRA_SERIES_TITLE = "extra_series_title"

        fun newIntent(context: Context, seriesId: String, seriesTitle: String = ""): Intent {
            return Intent(context, SeriesDetailsActivity::class.java).apply {
                putExtra(EXTRA_SERIES_ID, seriesId)
                putExtra(EXTRA_SERIES_TITLE, seriesTitle)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySeriesDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // Load series details
        val seriesId = intent.getStringExtra(EXTRA_SERIES_ID)
        val seriesTitle = intent.getStringExtra(EXTRA_SERIES_TITLE)
        
        if (seriesId.isNullOrBlank()) {
            showError("ID de série invalide")
            return
        }
        
        // Set initial title if available
        if (seriesTitle?.isNotBlank() == true) {
            binding.collapsingToolbar.title = seriesTitle
        }
        
        viewModel.loadSeriesDetails(seriesId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        seasonsAdapter = SeasonsAdapter { season ->
            // Navigate to episodes activity
            val seriesTitle = viewModel.series.value?.getDisplayTitle() ?: ""
            val intent = dev.pecorio.alphastream.ui.episodes.EpisodesActivity.newIntent(
                this,
                season,
                seriesTitle
            )
            startActivity(intent)
        }
        
        binding.seasonsRecyclerView.apply {
            adapter = seasonsAdapter
            layoutManager = LinearLayoutManager(this@SeriesDetailsActivity)
            isNestedScrollingEnabled = false
        }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is SeriesDetailsUiState.Loading -> showLoading()
                is SeriesDetailsUiState.Success -> showContent()
                is SeriesDetailsUiState.Error -> showError(state.message)
            }
        }

        viewModel.series.observe(this) { series ->
            series?.let { displaySeriesDetails(it) }
        }
    }

    private fun setupClickListeners() {
        binding.playButton.setOnClickListener {
            val series = viewModel.series.value
            if (series != null && series.hasValidSeasons()) {
                // Navigate to first episode of first season
                val firstSeason = series.seasons?.firstOrNull()
                val firstEpisode = firstSeason?.episodes?.firstOrNull()
                
                if (firstEpisode != null && firstEpisode.hasStreamingLinks()) {
                    // TODO: Navigate to video player
                    showMessage("Lecture de l'épisode ${firstEpisode.getEpisodeNumber()}")
                } else {
                    showMessage("Aucun épisode disponible pour cette série")
                }
            } else {
                showMessage("Aucun contenu disponible pour cette série")
            }
        }

        binding.favoriteButton.setOnClickListener {
            // TODO: Implement favorites functionality
            showMessage("Fonctionnalité favoris bientôt disponible")
        }

        binding.retryButton.setOnClickListener {
            viewModel.retry()
        }
    }

    private fun displaySeriesDetails(series: Series) {
        binding.apply {
            // Title
            seriesTitle.text = series.getDisplayTitle()
            collapsingToolbar.title = series.getDisplayTitle()

            // Backdrop image
            series.getDisplayImageUrl()?.let { imageUrl ->
                Glide.with(this@SeriesDetailsActivity)
                    .load(imageUrl)
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.placeholder_series)
                            .error(R.drawable.placeholder_series)
                            .centerCrop()
                    )
                    .into(seriesBackdrop)
            } ?: run {
                seriesBackdrop.setImageResource(R.drawable.placeholder_series)
            }

            // Rating
            series.getDisplayRating()?.let { rating ->
                seriesRating.text = String.format("%.1f", rating)
                ratingContainer.visibility = View.VISIBLE
            } ?: run {
                ratingContainer.visibility = View.GONE
            }

            // Year
            series.getDisplayReleaseDate()?.let { date ->
                val year = if (date.length >= 4) date.substring(0, 4) else date
                seriesYear.text = year
                seriesYear.visibility = View.VISIBLE
            } ?: run {
                seriesYear.visibility = View.GONE
            }

            // Status
            series.getFormattedStatus()?.let { status ->
                seriesStatus.text = status
                seriesStatus.visibility = View.VISIBLE
            } ?: run {
                seriesStatus.visibility = View.GONE
            }

            // Genres
            series.getFormattedGenres()?.let { genres ->
                seriesGenres.text = genres
                seriesGenres.visibility = View.VISIBLE
            } ?: run {
                seriesGenres.visibility = View.GONE
            }

            // Overview
            series.getDisplayOverview()?.let { overview ->
                seriesOverview.text = overview
                seriesOverview.visibility = View.VISIBLE
            } ?: run {
                seriesOverview.text = "Aucune description disponible."
            }

            // Seasons info
            val seasonsInfo = series.getDisplayDescription()
            seriesSeasonsInfo.text = seasonsInfo

            // Network
            series.getFormattedNetworks()?.let { networks ->
                seriesNetwork.text = networks
                networkContainer.visibility = View.VISIBLE
            } ?: run {
                networkContainer.visibility = View.GONE
            }

            // Creators
            series.getFormattedCreators()?.let { creators ->
                seriesCreators.text = creators
                creatorsContainer.visibility = View.VISIBLE
            } ?: run {
                creatorsContainer.visibility = View.GONE
            }

            // Data completeness indicator
            dataCompletenessIndicator.setSeries(series)
            
            // Seasons list
            series.seasons?.let { seasons ->
                seasonsAdapter.submitList(seasons)
            } ?: run {
                seasonsAdapter.submitList(emptyList())
            }
        }
    }

    private fun showLoading() {
        binding.loadingLayout.visibility = View.VISIBLE
        binding.errorLayout.visibility = View.GONE
        binding.appBarLayout.visibility = View.GONE
    }

    private fun showContent() {
        binding.loadingLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
        binding.appBarLayout.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        binding.loadingLayout.visibility = View.GONE
        binding.appBarLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.errorMessage.text = message
    }

    private fun showMessage(message: String) {
        currentSnackbar?.dismiss()
        currentSnackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        currentSnackbar?.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up snackbar to prevent window leaks
        currentSnackbar?.dismiss()
        currentSnackbar = null
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        // Dismiss any showing snackbar to prevent window leaks
        currentSnackbar?.dismiss()
    }
}