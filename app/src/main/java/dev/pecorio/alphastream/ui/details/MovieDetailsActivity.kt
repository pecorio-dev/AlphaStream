package dev.pecorio.alphastream.ui.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.data.model.Movie
import dev.pecorio.alphastream.data.model.WatchProgress
import dev.pecorio.alphastream.databinding.ActivityMovieDetailsBinding
import dev.pecorio.alphastream.data.repository.FavoritesRepository
import dev.pecorio.alphastream.data.repository.WatchProgressRepository
import dev.pecorio.alphastream.ui.dialogs.ResumeWatchingDialog
import dev.pecorio.alphastream.ui.player.VideoPlayerActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MovieDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMovieDetailsBinding
    private val viewModel: MovieDetailsViewModel by viewModels()
    private var currentSnackbar: Snackbar? = null
    
    @Inject
    lateinit var favoritesRepository: FavoritesRepository
    
    @Inject
    lateinit var watchProgressRepository: WatchProgressRepository
    
    private var currentMovie: Movie? = null
    private var isFavorite = false

    companion object {
        private const val EXTRA_MOVIE_TITLE = "extra_movie_title"
        private const val EXTRA_MOVIE_ORIGINAL_TITLE = "extra_movie_original_title"
        private const val EXTRA_MOVIE_SYNOPSIS = "extra_movie_synopsis"
        private const val EXTRA_MOVIE_IMAGE_URL = "extra_movie_image_url"
        private const val EXTRA_MOVIE_RATING = "extra_movie_rating"
        private const val EXTRA_MOVIE_RELEASE_DATE = "extra_movie_release_date"
        private const val EXTRA_MOVIE_GENRES = "extra_movie_genres"
        private const val EXTRA_MOVIE_CAST = "extra_movie_cast"
        private const val EXTRA_MOVIE_DIRECTORS = "extra_movie_directors"
        private const val EXTRA_MOVIE_LANGUAGE = "extra_movie_language"
        private const val EXTRA_MOVIE_QUALITY = "extra_movie_quality"
        private const val EXTRA_MOVIE_UQLOAD_OLD_URL = "extra_movie_uqload_old_url"
        private const val EXTRA_MOVIE_UQLOAD_NEW_URL = "extra_movie_uqload_new_url"
        private const val EXTRA_MOVIE_TMDB_ID = "extra_movie_tmdb_id"

        fun newIntent(context: Context, movie: Movie): Intent {
            return Intent(context, MovieDetailsActivity::class.java).apply {
                putExtra(EXTRA_MOVIE_TITLE, movie.title)
                putExtra(EXTRA_MOVIE_ORIGINAL_TITLE, movie.originalTitle)
                putExtra(EXTRA_MOVIE_SYNOPSIS, movie.getDisplayOverview())
                putExtra(EXTRA_MOVIE_IMAGE_URL, movie.getDisplayImageUrl())
                putExtra(EXTRA_MOVIE_RATING, movie.getDisplayRating() ?: 0.0)
                putExtra(EXTRA_MOVIE_RELEASE_DATE, movie.getDisplayReleaseDate())
                putStringArrayListExtra(EXTRA_MOVIE_GENRES, ArrayList(movie.genres ?: emptyList()))
                putStringArrayListExtra(EXTRA_MOVIE_CAST, ArrayList(movie.cast ?: emptyList()))
                putStringArrayListExtra(EXTRA_MOVIE_DIRECTORS, ArrayList(movie.getDisplayDirectors() ?: emptyList()))
                putExtra(EXTRA_MOVIE_LANGUAGE, movie.language)
                putExtra(EXTRA_MOVIE_QUALITY, movie.quality)
                putExtra(EXTRA_MOVIE_UQLOAD_OLD_URL, movie.uqloadOldUrl)
                putExtra(EXTRA_MOVIE_UQLOAD_NEW_URL, movie.uqloadNewUrl)
                putExtra(EXTRA_MOVIE_TMDB_ID, movie.tmdbId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovieDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupToolbar()
        getMovieFromIntent()
        setupObservers()
        setupClickListeners()
        displayMovieDetails()
        
        // Check favorite status
        currentMovie?.let { checkFavoriteStatus(it) }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top)
            insets
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun getMovieFromIntent() {
        val title = intent.getStringExtra(EXTRA_MOVIE_TITLE)
        if (title.isNullOrEmpty()) {
            finish()
            return
        }
        
        // Reconstruct Movie object from intent extras
        currentMovie = Movie(
            title = title,
            imageUrl = intent.getStringExtra(EXTRA_MOVIE_IMAGE_URL),
            remoteImageUrl = intent.getStringExtra(EXTRA_MOVIE_IMAGE_URL),
            detailsLink = null,
            uqloadOldUrl = intent.getStringExtra(EXTRA_MOVIE_UQLOAD_OLD_URL),
            uqloadNewUrl = intent.getStringExtra(EXTRA_MOVIE_UQLOAD_NEW_URL),
            tmdbId = intent.getStringExtra(EXTRA_MOVIE_TMDB_ID),
            tmdbTitle = title,
            tmdbOverview = intent.getStringExtra(EXTRA_MOVIE_SYNOPSIS),
            tmdbReleaseDate = intent.getStringExtra(EXTRA_MOVIE_RELEASE_DATE),
            tmdbVoteAverage = intent.getDoubleExtra(EXTRA_MOVIE_RATING, 0.0).takeIf { it > 0.0 },
            synopsis = intent.getStringExtra(EXTRA_MOVIE_SYNOPSIS),
            originalTitle = intent.getStringExtra(EXTRA_MOVIE_ORIGINAL_TITLE),
            genres = intent.getStringArrayListExtra(EXTRA_MOVIE_GENRES),
            releaseDate = intent.getStringExtra(EXTRA_MOVIE_RELEASE_DATE),
            rating = intent.getDoubleExtra(EXTRA_MOVIE_RATING, 0.0).takeIf { it > 0.0 },
            director = null,
            directors = intent.getStringArrayListExtra(EXTRA_MOVIE_DIRECTORS),
            cast = intent.getStringArrayListExtra(EXTRA_MOVIE_CAST),
            language = intent.getStringExtra(EXTRA_MOVIE_LANGUAGE),
            originalLanguage = intent.getStringExtra(EXTRA_MOVIE_LANGUAGE),
            quality = intent.getStringExtra(EXTRA_MOVIE_QUALITY),
            version = null,
            scrapedAt = null
        )
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is MovieDetailsUiState.Loading -> showLoading()
                is MovieDetailsUiState.Success -> {
                    hideLoading()
                    startVideoPlayer(state.streamInfo)
                }
                is MovieDetailsUiState.Error -> {
                    hideLoading()
                    showError(state.message)
                }
                is MovieDetailsUiState.Idle -> hideLoading()
            }
        }

        viewModel.isFavorite.observe(this) { isFavorite ->
            updateFavoriteButton(isFavorite)
        }
    }

    private fun setupClickListeners() {
        // Disable tooltips to prevent window leaks
        disableTooltips()
        
        binding.playButton.setOnClickListener {
            currentMovie?.let { movie ->
                // Debug: Log available URLs
                android.util.Log.d("MovieDetails", "uqloadOldUrl: ${movie.uqloadOldUrl}")
                android.util.Log.d("MovieDetails", "uqloadNewUrl: ${movie.uqloadNewUrl}")
                
                val streamUrl = movie.getStreamUrl()
                android.util.Log.d("MovieDetails", "Selected streamUrl: $streamUrl")
                
                if (streamUrl.isNullOrEmpty()) {
                    showError("Aucun lien de streaming disponible pour ce film. Vérifiez les logs pour voir les URLs disponibles.")
                } else {
                    // Vérifier s'il y a une progression sauvegardée
                    checkProgressAndPlay(movie, streamUrl)
                }
            }
        }

        binding.favoriteButton.setOnClickListener {
            toggleFavorite()
        }
    }

    private fun disableTooltips() {
        // Disable tooltips on buttons to prevent window leaks
        binding.playButton.tooltipText = null
        binding.favoriteButton.tooltipText = null
        binding.toolbar.navigationContentDescription = null
    }

    private fun displayMovieDetails() {
        currentMovie?.let { movie ->
            // Title and basic info
            binding.movieTitle.text = movie.getDisplayTitle()
            
            // Original title (show only if different)
            if (!movie.originalTitle.isNullOrEmpty() && movie.originalTitle != movie.title) {
                binding.movieOriginalTitle.text = movie.originalTitle
                binding.movieOriginalTitle.visibility = View.VISIBLE
            } else {
                binding.movieOriginalTitle.visibility = View.GONE
            }

            // Rating
            movie.getDisplayRating()?.let { rating ->
                binding.movieRating.text = String.format("%.1f", rating)
            } ?: run {
                binding.movieRating.text = "N/A"
            }

            // Year
            movie.getDisplayReleaseDate()?.let { date ->
                val year = if (date.length >= 4) date.substring(0, 4) else date
                binding.movieYear.text = year
            } ?: run {
                binding.movieYear.visibility = View.GONE
            }

            // Quality
            movie.getQualityBadge()?.let { quality ->
                binding.movieQuality.text = quality
                binding.movieQuality.visibility = View.VISIBLE
            } ?: run {
                binding.movieQuality.visibility = View.GONE
            }

            // Genres
            movie.getFormattedGenres()?.let { genres ->
                binding.movieGenres.text = genres
            } ?: run {
                binding.movieGenres.visibility = View.GONE
            }

            // Synopsis
            movie.getDisplayOverview()?.let { synopsis ->
                binding.movieSynopsis.text = synopsis
            } ?: run {
                binding.movieSynopsis.text = "Aucun synopsis disponible."
            }

            // Cast
            movie.getFormattedCast()?.let { cast ->
                binding.movieCast.text = cast
                binding.castSection.visibility = View.VISIBLE
            } ?: run {
                binding.castSection.visibility = View.GONE
            }

            // Directors
            movie.getDisplayDirectors()?.let { directors ->
                binding.movieDirectors.text = directors.joinToString(", ")
                binding.directorsSection.visibility = View.VISIBLE
            } ?: run {
                binding.directorsSection.visibility = View.GONE
            }

            // Language
            movie.language?.let { language ->
                binding.movieLanguage.text = language.uppercase()
            } ?: run {
                binding.movieLanguage.text = "Non spécifié"
            }

            // Quality info
            movie.quality?.let { quality ->
                binding.movieQualityInfo.text = quality.uppercase()
            } ?: run {
                binding.movieQualityInfo.text = "Non spécifié"
            }

            // Load images
            loadImages(movie)

            // Favorite status will be checked separately
        }
    }

    private fun loadImages(movie: Movie) {
        // Backdrop image
        movie.getDisplayImageUrl()?.let { imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.placeholder_movie)
                        .error(R.drawable.placeholder_movie)
                )
                .into(binding.movieBackdrop)
        }

        // Poster image
        movie.getDisplayImageUrl()?.let { imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.placeholder_movie)
                        .error(R.drawable.placeholder_movie)
                        .transform(RoundedCorners(24))
                )
                .into(binding.moviePoster)
        }
    }

    private fun showLoading() {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.playButton.isEnabled = false
    }

    private fun hideLoading() {
        binding.loadingOverlay.visibility = View.GONE
        binding.playButton.isEnabled = true
    }

    private fun showError(message: String) {
        // Dismiss any existing snackbar first
        currentSnackbar?.dismiss()
        
        currentSnackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Réessayer") {
                currentMovie?.getStreamUrl()?.let { url ->
                    viewModel.extractStreamInfo(url)
                }
            }
            .setBackgroundTint(resources.getColor(R.color.error, null))
            .setTextColor(resources.getColor(R.color.white, null))
            .setActionTextColor(resources.getColor(R.color.white, null))
        
        currentSnackbar?.show()
    }

    private fun updateFavoriteButton(isFavorite: Boolean) {
        val iconRes = if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        binding.favoriteButton.setIconResource(iconRes)
        
        // Animation du bouton
        binding.favoriteButton.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(150)
            .withEndAction {
                binding.favoriteButton.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    private fun toggleFavorite() {
        currentMovie?.let { movie ->
            lifecycleScope.launch {
                try {
                    val result = favoritesRepository.toggleFavorite(movie)
                    if (result.isSuccess) {
                        isFavorite = result.getOrDefault(false)
                        updateFavoriteButton(isFavorite)
                        val message = if (isFavorite) {
                            "${movie.getDisplayTitle()} ajouté aux favoris"
                        } else {
                            "${movie.getDisplayTitle()} retiré des favoris"
                        }
                        showMessage(message)
                    } else {
                        showError("Erreur lors de la mise à jour des favoris")
                    }
                } catch (e: Exception) {
                    showError("Erreur: ${e.message}")
                }
            }
        }
    }
    
    private fun checkFavoriteStatus(movie: Movie) {
        lifecycleScope.launch {
            try {
                val movieId = movie.title.hashCode().toString()
                isFavorite = favoritesRepository.isFavorite(movieId)
                updateFavoriteButton(isFavorite)
            } catch (e: Exception) {
                // Ignore errors when checking favorite status
            }
        }
    }
    
    private fun showMessage(message: String) {
        currentSnackbar?.dismiss()
        currentSnackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        currentSnackbar?.show()
    }

    private fun checkProgressAndPlay(movie: Movie, streamUrl: String) {
        lifecycleScope.launch {
            try {
                val movieId = movie.getMovieId()
                val existingProgress = watchProgressRepository.getMovieProgress(movieId)
                
                if (existingProgress?.hasSignificantProgress() == true) {
                    // Afficher le dialog de reprise
                    showResumeDialog(existingProgress, movie, streamUrl)
                } else {
                    // Commencer directement l'extraction et la lecture
                    extractAndPlayMovie(movie, streamUrl, 0L)
                }
            } catch (e: Exception) {
                android.util.Log.e("MovieDetails", "Erreur lors de la vérification de progression: ${e.message}")
                extractAndPlayMovie(movie, streamUrl, 0L)
            }
        }
    }
    
    private fun showResumeDialog(progress: WatchProgress, movie: Movie, streamUrl: String) {
        val dialog = ResumeWatchingDialog.newInstance(
            watchProgress = progress,
            onResumeClicked = { progressData ->
                extractAndPlayMovie(movie, streamUrl, progressData.currentPosition)
            },
            onStartOverClicked = {
                extractAndPlayMovie(movie, streamUrl, 0L)
            }
        )
        dialog.show(supportFragmentManager, "ResumeWatchingDialog")
    }
    
    private fun extractAndPlayMovie(movie: Movie, streamUrl: String, startPosition: Long) {
        // Stocker les informations pour la reprise
        viewModel.setResumeInfo(movie, startPosition)
        // Lancer l'extraction
        viewModel.extractStreamInfo(streamUrl)
    }

    private fun startVideoPlayer(streamInfo: dev.pecorio.alphastream.data.extractor.ExtractedStreamInfo) {
        val movie = currentMovie ?: return
        val resumeInfo = viewModel.getResumeInfo()
        
        val intent = VideoPlayerActivity.newMovieIntent(
            context = this,
            videoUrl = streamInfo.url,
            movieTitle = streamInfo.title ?: movie.getDisplayTitle(),
            movieId = movie.getMovieId(),
            headers = streamInfo.headers,
            imageUrl = movie.getDisplayImageUrl(),
            startPosition = resumeInfo?.second ?: 0L
        )
        startActivity(intent)
    }

    override fun onPause() {
        super.onPause()
        // Dismiss any showing snackbar to prevent window leaks
        currentSnackbar?.dismiss()
        currentSnackbar = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up any remaining references
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