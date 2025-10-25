package dev.pecorio.alphastream.ui.series.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.data.model.Season
import dev.pecorio.alphastream.databinding.ItemSeasonBinding

class SeasonsAdapter(
    private val onSeasonClick: (Season) -> Unit
) : ListAdapter<Season, SeasonsAdapter.SeasonViewHolder>(SeasonDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeasonViewHolder {
        val binding = ItemSeasonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SeasonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeasonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SeasonViewHolder(
        private val binding: ItemSeasonBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(season: Season) {
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

                // Season poster supprimé - l'API ne fournit pas d'images pour les saisons
                // Plus d'affichage d'images placeholder

                // Season availability indicator
                seasonAvailabilityIndicator.setSeasonAvailability(season)
                
                // Click listener
                root.setOnClickListener {
                    onSeasonClick(season)
                }
            }
        }
    }
}

class SeasonDiffCallback : DiffUtil.ItemCallback<Season>() {
    override fun areItemsTheSame(oldItem: Season, newItem: Season): Boolean {
        return oldItem.number == newItem.number
    }

    override fun areContentsTheSame(oldItem: Season, newItem: Season): Boolean {
        return oldItem == newItem
    }
}