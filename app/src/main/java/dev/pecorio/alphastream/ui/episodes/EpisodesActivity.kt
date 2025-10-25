package dev.pecorio.alphastream.ui.episodes

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.data.extractor.FastDirectExtractor
import dev.pecorio.alphastream.data.model.Episode
import dev.pecorio.alphastream.data.model.Season
import dev.pecorio.alphastream.data.model.WatchProgress
import dev.pecorio.alphastream.data.repository.WatchProgressRepository
import dev.pecorio.alphastream.databinding.ActivityEpisodesBinding
import dev.pecorio.alphastream.ui.dialogs.ResumeWatchingDialog
import dev.pecorio.alphastream.ui.player.VideoPlayerActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EpisodesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEpisodesBinding
    private var currentSnackbar: Snackbar? = null

    private lateinit var episodesAdapter: EpisodesAdapter
    
    @Inject
    lateinit var fastDirectExtractor: FastDirectExtractor
    
    @Inject
    lateinit var watchProgressRepository: WatchProgressRepository
    
    private var currentSeason: Season? = null
    private var seriesTitle: String = ""

    companion object {
        private const val EXTRA_SEASON = "extra_season"
        private const val EXTRA_SERIES_TITLE = "extra_series_title"

        fun newIntent(context: Context, season: Season, seriesTitle: String = ""): Intent {
            return Intent(context, EpisodesActivity::class.java).apply {
                putExtra(EXTRA_SEASON, season)
                putExtra(EXTRA_SERIES_TITLE, seriesTitle)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityEpisodesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        currentSeason = intent.getParcelableExtra<Season>(EXTRA_SEASON)
        seriesTitle = intent.getStringExtra(EXTRA_SERIES_TITLE) ?: ""
        
        if (currentSeason == null) {
            showError("Données de saison invalides")
            return
        }
        
        setupToolbar(currentSeason!!, seriesTitle)
        setupRecyclerView(currentSeason!!)
        displaySeasonDetails(currentSeason!!)
    }

    private fun setupToolbar(season: Season, seriesTitle: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // Set title
        val title = if (seriesTitle.isNotBlank()) {
            "$seriesTitle - ${season.getDisplayTitle()}"
        } else {
            season.getDisplayTitle()
        }
        binding.collapsingToolbar.title = title
        
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView(season: Season) {
        episodesAdapter = EpisodesAdapter { episode ->
            playEpisode(episode)
        }
        
        binding.episodesRecyclerView.apply {
            adapter = episodesAdapter
            layoutManager = LinearLayoutManager(this@EpisodesActivity)
        }
        
        // Submit episodes list
        season.episodes?.let { episodes ->
            episodesAdapter.submitList(episodes)
        } ?: run {
            episodesAdapter.submitList(emptyList())
        }
    }

    private fun displaySeasonDetails(season: Season) {
        binding.apply {
            // Season title
            seasonTitle.text = season.getDisplayTitle()
            
            // Episode count
            val episodeCount = season.getEpisodeCount()
            seasonEpisodeCount.text = if (episodeCount > 0) {
                "$episodeCount épisode${if (episodeCount > 1) "s" else ""}"
            } else {
                "Aucun épisode"
            }
            
            // Season overview
            season.overview?.takeIf { it.isNotBlank() }?.let { overview ->
                seasonOverview.text = overview
                seasonOverview.visibility = View.VISIBLE
            } ?: run {
                seasonOverview.visibility = View.GONE
            }
            
            // Air date
            season.airDate?.takeIf { it.isNotBlank() }?.let { airDate ->
                val year = if (airDate.length >= 4) airDate.substring(0, 4) else airDate
                seasonAirDate.text = "Diffusée en $year"
                seasonAirDate.visibility = View.VISIBLE
            } ?: run {
                seasonAirDate.visibility = View.GONE
            }
            
            // Show/hide empty state
            if (episodeCount == 0) {
                emptyStateLayout.visibility = View.VISIBLE
                episodesRecyclerView.visibility = View.GONE
            } else {
                emptyStateLayout.visibility = View.GONE
                episodesRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun playEpisode(episode: Episode) {
        // Priorité à uqload_old_url comme demandé
        val embedUrl = episode.uqloadOldUrl?.takeIf { it.isNotBlank() }
            ?: episode.uqloadNewUrl?.takeIf { it.isNotBlank() }
            ?: episode.streams?.firstOrNull()?.url?.takeIf { it.isNotBlank() }
        
        if (embedUrl != null) {
            android.util.Log.d("EpisodesActivity", "Extraction pour épisode ${episode.getEpisodeNumber()}: $embedUrl")
            
            // Vérifier s'il y a une progression sauvegardée
            lifecycleScope.launch {
                try {
                    val season = currentSeason ?: return@launch
                    val seriesId = extractSeriesIdFromSeason(season)
                    
                    val existingProgress = watchProgressRepository.getEpisodeProgress(
                        seriesId = seriesId,
                        seasonNumber = season.getSeasonNumber(),
                        episodeNumber = episode.getEpisodeNumber()
                    )
                    
                    if (existingProgress?.hasSignificantProgress() == true) {
                        // Afficher le dialog de reprise
                        showResumeDialog(existingProgress, episode, embedUrl)
                    } else {
                        // Commencer directement l'extraction et la lecture
                        extractAndPlayEpisode(episode, embedUrl, 0L)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EpisodesActivity", "Erreur lors de la vérification de progression: ${e.message}")
                    extractAndPlayEpisode(episode, embedUrl, 0L)
                }
            }
        } else {
            showMessage("Aucun lien de streaming disponible pour cet épisode")
        }
    }
    
    private fun showResumeDialog(progress: WatchProgress, episode: Episode, embedUrl: String) {
        val dialog = ResumeWatchingDialog.newInstance(
            watchProgress = progress,
            onResumeClicked = { progressData ->
                extractAndPlayEpisode(episode, embedUrl, progressData.currentPosition)
            },
            onStartOverClicked = {
                extractAndPlayEpisode(episode, embedUrl, 0L)
            }
        )
        dialog.show(supportFragmentManager, "ResumeWatchingDialog")
    }
    
    private fun extractAndPlayEpisode(episode: Episode, embedUrl: String, startPosition: Long) {
        lifecycleScope.launch {
            try {
                showMessage("Extraction du lien vidéo...")
                
                val extractionResult = fastDirectExtractor.extractStreamInfo(embedUrl)
                
                if (extractionResult.isSuccess) {
                    val streamInfo = extractionResult.getOrNull()!!
                    android.util.Log.d("EpisodesActivity", "Extraction réussie: ${streamInfo.url}")
                    
                    val season = currentSeason ?: return@launch
                    val seriesId = extractSeriesIdFromSeason(season)
                    
                    // Lancer le player avec les informations de progression
                    val intent = VideoPlayerActivity.newEpisodeIntent(
                        context = this@EpisodesActivity,
                        videoUrl = streamInfo.url,
                        episodeTitle = "${seriesTitle} - S${season.getSeasonNumber()}E${episode.getEpisodeNumber()} - ${episode.getDisplayTitle()}",
                        seriesId = seriesId,
                        seasonNumber = season.getSeasonNumber(),
                        episodeNumber = episode.getEpisodeNumber(),
                        headers = streamInfo.headers,
                        imageUrl = null, // TODO: Get series image
                        startPosition = startPosition
                    )
                    startActivity(intent)
                } else {
                    val error = extractionResult.exceptionOrNull()?.message ?: "Erreur inconnue"
                    showMessage("Impossible d'extraire le lien vidéo: $error")
                }
            } catch (e: Exception) {
                android.util.Log.e("EpisodesActivity", "Erreur lors de l'extraction: ${e.message}")
                showMessage("Erreur lors de l'extraction: ${e.message}")
            }
        }
    }
    
    private fun extractSeriesIdFromSeason(season: Season): String {
        // Puisque Season n'a pas d'ID, nous devons utiliser une autre approche
        // Nous pouvons utiliser le titre de la série ou un ID généré
        // Pour l'instant, utilisons le titre de la série comme ID
        return seriesTitle.takeIf { it.isNotBlank() }?.hashCode()?.toString() ?: "unknown_series"
    }

    private fun showError(message: String) {
        binding.errorStateLayout.visibility = View.VISIBLE
        binding.episodesRecyclerView.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        
        showMessage(message)
    }

    private fun showMessage(message: String) {
        currentSnackbar?.dismiss()
        currentSnackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        currentSnackbar?.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentSnackbar?.dismiss()
        currentSnackbar = null
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        currentSnackbar?.dismiss()
    }
}