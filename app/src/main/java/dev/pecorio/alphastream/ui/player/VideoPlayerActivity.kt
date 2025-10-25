package dev.pecorio.alphastream.ui.player

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.dash.DashMediaSource
import dagger.hilt.android.AndroidEntryPoint
import dev.pecorio.alphastream.databinding.ActivityVideoPlayerBinding
import dev.pecorio.alphastream.data.repository.WatchProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.widget.TextView
import javax.inject.Inject

@AndroidEntryPoint
class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding
    private var player: ExoPlayer? = null
    private var videoUrl: String = ""
    private var videoTitle: String = ""
    private var headers: Map<String, String> = emptyMap()
    
    @Inject
    lateinit var watchProgressRepository: WatchProgressRepository
    
    // Progress tracking
    private var progressTrackingJob: Job? = null
    private val progressScope = CoroutineScope(Dispatchers.Main)
    
    // Content info for progress tracking
    private var contentType: String = "" // "movie" or "episode"
    private var contentId: String = ""
    private var seriesId: String? = null
    private var seasonNumber: Int? = null
    private var episodeNumber: Int? = null
    private var imageUrl: String? = null
    private var startPosition: Long = 0L
    
    // Progressive seek controls
    private var isSeekingForward = false
    private var isSeekingBackward = false
    private var currentSeekSpeed = 1.0f
    private var seekSpeedLevel = 0
    private val seekSpeeds = floatArrayOf(2.0f, 4.0f, 8.0f, 16.0f, 32.0f)
    private val seekHandler = Handler(Looper.getMainLooper())
    private var seekRunnable: Runnable? = null
    private var speedIndicator: TextView? = null
    private var speedIndicatorHideRunnable: Runnable? = null

    companion object {
        private const val EXTRA_VIDEO_URL = "extra_video_url"
        private const val EXTRA_VIDEO_TITLE = "extra_video_title"
        private const val EXTRA_HEADERS = "extra_headers"
        private const val EXTRA_CONTENT_TYPE = "extra_content_type"
        private const val EXTRA_CONTENT_ID = "extra_content_id"
        private const val EXTRA_SERIES_ID = "extra_series_id"
        private const val EXTRA_SEASON_NUMBER = "extra_season_number"
        private const val EXTRA_EPISODE_NUMBER = "extra_episode_number"
        private const val EXTRA_IMAGE_URL = "extra_image_url"
        private const val EXTRA_START_POSITION = "extra_start_position"

        fun newIntent(
            context: Context,
            videoUrl: String,
            videoTitle: String,
            headers: Map<String, String>
        ): Intent {
            return Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, videoUrl)
                putExtra(EXTRA_VIDEO_TITLE, videoTitle)
                putExtra(EXTRA_HEADERS, HashMap(headers))
            }
        }
        
        fun newMovieIntent(
            context: Context,
            videoUrl: String,
            movieTitle: String,
            movieId: String,
            headers: Map<String, String>,
            imageUrl: String? = null,
            startPosition: Long = 0L
        ): Intent {
            return Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, videoUrl)
                putExtra(EXTRA_VIDEO_TITLE, movieTitle)
                putExtra(EXTRA_HEADERS, HashMap(headers))
                putExtra(EXTRA_CONTENT_TYPE, "movie")
                putExtra(EXTRA_CONTENT_ID, movieId)
                putExtra(EXTRA_IMAGE_URL, imageUrl)
                putExtra(EXTRA_START_POSITION, startPosition)
            }
        }
        
        fun newEpisodeIntent(
            context: Context,
            videoUrl: String,
            episodeTitle: String,
            seriesId: String,
            seasonNumber: Int,
            episodeNumber: Int,
            headers: Map<String, String>,
            imageUrl: String? = null,
            startPosition: Long = 0L
        ): Intent {
            return Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, videoUrl)
                putExtra(EXTRA_VIDEO_TITLE, episodeTitle)
                putExtra(EXTRA_HEADERS, HashMap(headers))
                putExtra(EXTRA_CONTENT_TYPE, "episode")
                putExtra(EXTRA_CONTENT_ID, seriesId)
                putExtra(EXTRA_SERIES_ID, seriesId)
                putExtra(EXTRA_SEASON_NUMBER, seasonNumber)
                putExtra(EXTRA_EPISODE_NUMBER, episodeNumber)
                putExtra(EXTRA_IMAGE_URL, imageUrl)
                putExtra(EXTRA_START_POSITION, startPosition)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getExtrasFromIntent()
        setupFullscreen()
        setupPlayer()
        setupClickListeners()
        setupSpeedIndicator()
    }

    private fun getExtrasFromIntent() {
        videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: ""
        videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: ""
        @Suppress("UNCHECKED_CAST")
        headers = intent.getSerializableExtra(EXTRA_HEADERS) as? Map<String, String> ?: emptyMap()
        
        // Progress tracking info
        contentType = intent.getStringExtra(EXTRA_CONTENT_TYPE) ?: ""
        contentId = intent.getStringExtra(EXTRA_CONTENT_ID) ?: ""
        seriesId = intent.getStringExtra(EXTRA_SERIES_ID)
        seasonNumber = intent.getIntExtra(EXTRA_SEASON_NUMBER, 0).takeIf { it > 0 }
        episodeNumber = intent.getIntExtra(EXTRA_EPISODE_NUMBER, 0).takeIf { it > 0 }
        imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL)
        startPosition = intent.getLongExtra(EXTRA_START_POSITION, 0L)

        if (videoUrl.isEmpty()) {
            showError("URL de la vidéo manquante")
            return
        }
    }

    private fun setupFullscreen() {
        // Force landscape orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Hide system UI
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, binding.root)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build()

        binding.playerView.player = player

        // Set video title in custom controls
        val titleView = binding.playerView.findViewById<android.widget.TextView>(
            resources.getIdentifier("exo_title", "id", packageName)
        )
        titleView?.text = videoTitle

        // Setup player listener
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> showLoading()
                    Player.STATE_READY -> hideLoading()
                    Player.STATE_ENDED -> finish()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                showError("Erreur de lecture: ${error.message}")
            }
        })

        // Setup custom controls
        setupCustomControls()

        // Load and play video
        loadVideo()
    }

    private fun setupCustomControls() {
        // Back button
        val backButton = binding.playerView.findViewById<android.widget.ImageButton>(
            resources.getIdentifier("exo_back", "id", packageName)
        )
        backButton?.setOnClickListener { finish() }

        // Settings button (placeholder)
        val settingsButton = binding.playerView.findViewById<android.widget.ImageButton>(
            resources.getIdentifier("exo_settings", "id", packageName)
        )
        settingsButton?.setOnClickListener {
            // TODO: Implement settings menu
        }

        // Fullscreen button (placeholder since we're already fullscreen)
        val fullscreenButton = binding.playerView.findViewById<android.widget.ImageButton>(
            resources.getIdentifier("exo_fullscreen", "id", packageName)
        )
        fullscreenButton?.visibility = View.GONE
    }

    private fun loadVideo() {
        try {
            // Create data source factory with headers
            val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
                setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:130.0) Gecko/20100101 Firefox/130.0")
                if (headers.isNotEmpty()) {
                    setDefaultRequestProperties(headers)
                }
                setConnectTimeoutMs(15000)
                setReadTimeoutMs(15000)
                setAllowCrossProtocolRedirects(true)
            }

            // Create media source based on URL format
            val mediaSource = createMediaSource(videoUrl, dataSourceFactory)

            // Prepare and play
            player?.setMediaSource(mediaSource)
            player?.prepare()
            
            // Set start position if provided
            if (startPosition > 0) {
                player?.seekTo(startPosition)
            }
            
            player?.playWhenReady = true
            
            // Start progress tracking
            startProgressTracking()

        } catch (e: Exception) {
            showError("Erreur lors du chargement: ${e.message}")
        }
    }

    private fun createMediaSource(url: String, dataSourceFactory: DefaultHttpDataSource.Factory): MediaSource {
        val mediaItem = MediaItem.fromUri(url)

        return when {
            url.contains(".m3u8") -> {
                // HLS stream
                HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            url.contains(".mpd") -> {
                // DASH stream
                DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            else -> {
                // Progressive MP4
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }
    }

    private fun setupClickListeners() {
        binding.retryButton.setOnClickListener {
            hideError()
            loadVideo()
        }
    }

    private fun showLoading() {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.errorOverlay.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.loadingOverlay.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.errorOverlay.visibility = View.VISIBLE
        binding.loadingOverlay.visibility = View.GONE
        binding.errorMessage.text = message
    }

    private fun hideError() {
        binding.errorOverlay.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        player?.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Stop progressive seek
        stopProgressiveSeek()
        
        // Save final progress before destroying
        saveCurrentProgress()
        
        // Stop progress tracking
        stopProgressTracking()
        
        // Release player resources
        player?.release()
        player = null
        
        // Clear any potential window references
        binding.playerView.player = null
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                startProgressiveSeek(true)
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> {
                startProgressiveSeek(false)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> {
                stopProgressiveSeek()
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }
    
    /**
     * Configure l'indicateur de vitesse
     */
    private fun setupSpeedIndicator() {
        speedIndicator = TextView(this).apply {
            textSize = 24f
            setTextColor(android.graphics.Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#CC000000"))
                cornerRadius = 16f
            }
            setPadding(32, 16, 32, 16)
            visibility = View.GONE
        }
        
        // Ajouter l'indicateur au layout du player
        val playerContainer = binding.playerView
        if (playerContainer is android.widget.FrameLayout) {
            val layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            playerContainer.addView(speedIndicator, layoutParams)
        }
    }
    
    /**
     * Démarre l'avance rapide progressive
     */
    private fun startProgressiveSeek(forward: Boolean) {
        if (forward && isSeekingForward) {
            // Déjà en train d'avancer, augmenter la vitesse
            increaseSeekSpeed()
            return
        }
        if (!forward && isSeekingBackward) {
            // Déjà en train de reculer, augmenter la vitesse
            increaseSeekSpeed()
            return
        }
        
        // Arrêter tout seek en cours
        stopProgressiveSeek()
        
        // Démarrer le nouveau seek
        isSeekingForward = forward
        isSeekingBackward = !forward
        seekSpeedLevel = 0
        currentSeekSpeed = seekSpeeds[seekSpeedLevel]
        
        // Afficher l'indicateur de vitesse
        showSpeedIndicator(forward)
        
        // Démarrer le seek
        startSeekLoop(forward)
    }
    
    /**
     * Augmente la vitesse de seek
     */
    private fun increaseSeekSpeed() {
        if (seekSpeedLevel < seekSpeeds.size - 1) {
            seekSpeedLevel++
            currentSeekSpeed = seekSpeeds[seekSpeedLevel]
            
            // Mettre à jour l'indicateur
            updateSpeedIndicator()
        }
    }
    
    /**
     * Démarre la boucle de seek
     */
    private fun startSeekLoop(forward: Boolean) {
        seekRunnable = object : Runnable {
            override fun run() {
                val currentPlayer = player ?: return
                val currentPosition = currentPlayer.currentPosition
                val duration = currentPlayer.duration
                
                if (duration <= 0) return
                
                // Calculer le saut en fonction de la vitesse
                val seekAmount = (1000 * currentSeekSpeed).toLong() // 1 seconde * vitesse
                val newPosition = if (forward) {
                    (currentPosition + seekAmount).coerceAtMost(duration)
                } else {
                    (currentPosition - seekAmount).coerceAtLeast(0)
                }
                
                currentPlayer.seekTo(newPosition)
                
                // Programmer le prochain seek
                val delay = (200 / currentSeekSpeed).toLong().coerceAtLeast(50) // Minimum 50ms
                seekHandler.postDelayed(this, delay)
            }
        }
        
        seekRunnable?.let { seekHandler.post(it) }
    }
    
    /**
     * Arrête l'avance rapide progressive
     */
    private fun stopProgressiveSeek() {
        isSeekingForward = false
        isSeekingBackward = false
        seekSpeedLevel = 0
        currentSeekSpeed = 1.0f
        
        // Arrêter la boucle de seek
        seekRunnable?.let {
            seekHandler.removeCallbacks(it)
            seekRunnable = null
        }
        
        // Cacher l'indicateur de vitesse
        hideSpeedIndicator()
    }
    
    /**
     * Affiche l'indicateur de vitesse
     */
    private fun showSpeedIndicator(forward: Boolean) {
        speedIndicator?.let { indicator ->
            val direction = if (forward) "►" else "◄"
            val speed = String.format("%.0fx", currentSeekSpeed)
            indicator.text = "$direction $speed"
            indicator.visibility = View.VISIBLE
            
            // Annuler le masquage automatique précédent
            speedIndicatorHideRunnable?.let { seekHandler.removeCallbacks(it) }
        }
    }
    
    /**
     * Met à jour l'indicateur de vitesse
     */
    private fun updateSpeedIndicator() {
        speedIndicator?.let { indicator ->
            val direction = if (isSeekingForward) "►" else "◄"
            val speed = String.format("%.0fx", currentSeekSpeed)
            indicator.text = "$direction $speed"
        }
    }
    
    /**
     * Cache l'indicateur de vitesse
     */
    private fun hideSpeedIndicator() {
        speedIndicatorHideRunnable = Runnable {
            speedIndicator?.visibility = View.GONE
        }
        seekHandler.postDelayed(speedIndicatorHideRunnable!!, 500) // Délai de 500ms
    }
    
    /**
     * Démarre le suivi de progression
     */
    private fun startProgressTracking() {
        if (contentType.isEmpty() || contentId.isEmpty()) {
            android.util.Log.d("VideoPlayer", "No content info for progress tracking")
            return
        }
        
        stopProgressTracking() // Stop any existing tracking
        
        progressTrackingJob = progressScope.launch {
            while (true) {
                delay(10000) // Save progress every 10 seconds
                saveCurrentProgress()
            }
        }
        
        android.util.Log.d("VideoPlayer", "Started progress tracking for $contentType: $contentId")
    }
    
    /**
     * Arrête le suivi de progression
     */
    private fun stopProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
    }
    
    /**
     * Sauvegarde la progression actuelle
     */
    private fun saveCurrentProgress() {
        val currentPlayer = player ?: return
        if (contentType.isEmpty() || contentId.isEmpty()) return
        
        val currentPosition = currentPlayer.currentPosition
        val duration = currentPlayer.duration
        
        if (duration <= 0 || currentPosition <= 0) return
        
        progressScope.launch {
            try {
                when (contentType) {
                    "movie" -> {
                        watchProgressRepository.saveMovieProgress(
                            movieId = contentId,
                            title = videoTitle,
                            currentPosition = currentPosition,
                            duration = duration,
                            imageUrl = imageUrl,
                            streamUrl = videoUrl
                        )
                    }
                    "episode" -> {
                        val sId = seriesId ?: return@launch
                        val sNum = seasonNumber ?: return@launch
                        val eNum = episodeNumber ?: return@launch
                        
                        watchProgressRepository.saveEpisodeProgress(
                            seriesId = sId,
                            seasonNumber = sNum,
                            episodeNumber = eNum,
                            seriesTitle = contentId, // Using contentId as series title for now
                            episodeTitle = videoTitle,
                            currentPosition = currentPosition,
                            duration = duration,
                            imageUrl = imageUrl,
                            streamUrl = videoUrl
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayer", "Error saving progress: ${e.message}")
            }
        }
    }
}