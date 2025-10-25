package dev.pecorio.alphastream.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.data.model.Series
import dev.pecorio.alphastream.databinding.ViewDataCompletenessIndicatorBinding

class DataCompletenessView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewDataCompletenessIndicatorBinding

    init {
        binding = ViewDataCompletenessIndicatorBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
    }

    /**
     * Configure l'affichage pour une série
     */
    fun setSeries(series: Series) {
        val missingFields = series.getMissingDataFields()
        val completenessIndicator = series.getDataCompletenessIndicator()
        val score = series.calculateCompletenessScore()

        // Texte de complétude
        binding.completenessText.text = completenessIndicator

        // Icône et couleur selon le score
        when {
            score >= 80 -> {
                binding.completenessIcon.setImageResource(R.drawable.ic_check)
                binding.completenessIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.accent_green)
                )
                binding.completenessText.setTextColor(
                    ContextCompat.getColor(context, R.color.accent_green)
                )
            }
            score >= 60 -> {
                binding.completenessIcon.setImageResource(R.drawable.ic_info)
                binding.completenessIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.accent_orange)
                )
                binding.completenessText.setTextColor(
                    ContextCompat.getColor(context, R.color.accent_orange)
                )
            }
            else -> {
                binding.completenessIcon.setImageResource(R.drawable.ic_warning)
                binding.completenessIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.error)
                )
                binding.completenessText.setTextColor(
                    ContextCompat.getColor(context, R.color.error)
                )
            }
        }

        // Compteur de données manquantes
        if (missingFields.isNotEmpty()) {
            binding.missingCount.text = missingFields.size.toString()
            binding.missingCount.visibility = VISIBLE
            
            // Tooltip avec les champs manquants
            val tooltip = "Données manquantes: ${missingFields.joinToString(", ")}"
            binding.root.contentDescription = tooltip
        } else {
            binding.missingCount.visibility = GONE
            binding.root.contentDescription = "Toutes les données sont disponibles"
        }
    }

    /**
     * Configure l'affichage pour un épisode
     */
    fun setEpisodeAvailability(episode: dev.pecorio.alphastream.data.model.Episode) {
        val serverCount = episode.getAvailableServersCount()
        val availabilityStatus = episode.getAvailabilityStatus()

        binding.completenessText.text = availabilityStatus

        when {
            serverCount >= 3 -> {
                binding.completenessIcon.setImageResource(R.drawable.ic_check)
                binding.completenessIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.accent_green)
                )
                binding.completenessText.setTextColor(
                    ContextCompat.getColor(context, R.color.accent_green)
                )
            }
            serverCount >= 1 -> {
                binding.completenessIcon.setImageResource(R.drawable.ic_info)
                binding.completenessIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.accent_orange)
                )
                binding.completenessText.setTextColor(
                    ContextCompat.getColor(context, R.color.accent_orange)
                )
            }
            else -> {
                binding.completenessIcon.setImageResource(R.drawable.ic_warning)
                binding.completenessIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.error)
                )
                binding.completenessText.setTextColor(
                    ContextCompat.getColor(context, R.color.error)
                )
            }
        }

        binding.missingCount.visibility = GONE
        
        // Tooltip avec les serveurs disponibles
        val servers = episode.getAvailableServers()
        val tooltip = if (servers.isNotEmpty()) {
            "Serveurs disponibles: ${servers.joinToString(", ")}"
        } else {
            "Aucun serveur disponible"
        }
        binding.root.contentDescription = tooltip
    }

    /**
     * Configure l'affichage pour une saison
     */
    fun setSeasonAvailability(season: dev.pecorio.alphastream.data.model.Season) {
        val availabilityStatus = season.getAvailabilityStatus()
        val episodeCount = season.getEpisodeCount()
        val availableEpisodes = season.episodes?.count { it.hasStreamingLinks() } ?: 0

        binding.completenessText.text = availabilityStatus

        when {
            availableEpisodes == episodeCount && episodeCount > 0 -> {
                binding.completenessIcon.setImageResource(R.drawable.ic_check)
                binding.completenessIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.accent_green)
                )
                binding.completenessText.setTextColor(
                    ContextCompat.getColor(context, R.color.accent_green)
                )
            }
            availableEpisodes > 0 -> {
                binding.completenessIcon.setImageResource(R.drawable.ic_info)
                binding.completenessIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.accent_orange)
                )
                binding.completenessText.setTextColor(
                    ContextCompat.getColor(context, R.color.accent_orange)
                )
            }
            else -> {
                binding.completenessIcon.setImageResource(R.drawable.ic_warning)
                binding.completenessIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.error)
                )
                binding.completenessText.setTextColor(
                    ContextCompat.getColor(context, R.color.error)
                )
            }
        }

        // Afficher le nombre d'épisodes manquants
        val missingEpisodes = episodeCount - availableEpisodes
        if (missingEpisodes > 0) {
            binding.missingCount.text = missingEpisodes.toString()
            binding.missingCount.visibility = VISIBLE
        } else {
            binding.missingCount.visibility = GONE
        }

        binding.root.contentDescription = "Saison ${season.getSeasonNumber()}: $availabilityStatus"
    }
}