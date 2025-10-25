package dev.pecorio.alphastream.ui.episodes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.data.model.Episode
import dev.pecorio.alphastream.databinding.ItemEpisodeBinding

class EpisodesAdapter(
    private val onEpisodeClick: (Episode) -> Unit
) : ListAdapter<Episode, EpisodesAdapter.EpisodeViewHolder>(EpisodeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val binding = ItemEpisodeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EpisodeViewHolder(
        private val binding: ItemEpisodeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(episode: Episode) {
            binding.apply {
                // Episode number and title
                episodeNumber.text = "Épisode ${episode.getEpisodeNumber()}"
                episodeTitle.text = episode.getDisplayTitle()

                // Episode overview
                episode.overview?.takeIf { it.isNotBlank() }?.let { overview ->
                    episodeOverview.text = overview
                    episodeOverview.visibility = View.VISIBLE
                } ?: run {
                    episodeOverview.visibility = View.GONE
                }

                // Runtime
                episode.getFormattedRuntime()?.let { runtime ->
                    episodeRuntime.text = runtime
                    episodeRuntime.visibility = View.VISIBLE
                } ?: run {
                    episodeRuntime.visibility = View.GONE
                }

                // Air date
                episode.airDate?.takeIf { it.isNotBlank() }?.let { airDate ->
                    episodeAirDate.text = airDate
                    episodeAirDate.visibility = View.VISIBLE
                } ?: run {
                    episodeAirDate.visibility = View.GONE
                }

                // Streaming availability
                val hasStreaming = episode.hasStreamingLinks()
                val serverCount = episode.getAvailableServersCount()
                
                if (hasStreaming) {
                    streamingStatus.text = when {
                        serverCount > 1 -> "$serverCount serveurs disponibles"
                        serverCount == 1 -> "1 serveur disponible"
                        else -> "Disponible"
                    }
                    streamingStatus.setTextColor(
                        itemView.context.getColor(R.color.accent_green)
                    )
                    playButton.visibility = View.VISIBLE
                } else {
                    streamingStatus.text = "Non disponible"
                    streamingStatus.setTextColor(
                        itemView.context.getColor(R.color.error)
                    )
                    playButton.visibility = View.GONE
                }

                // Episode availability indicator
                episodeAvailabilityIndicator.setEpisodeAvailability(episode)

                // Click listeners
                root.setOnClickListener {
                    if (hasStreaming) {
                        onEpisodeClick(episode)
                    }
                }

                playButton.setOnClickListener {
                    onEpisodeClick(episode)
                }

                // Visual feedback for clickable state
                root.isClickable = hasStreaming
                root.alpha = if (hasStreaming) 1.0f else 0.6f
            }
        }
    }
}

class EpisodeDiffCallback : DiffUtil.ItemCallback<Episode>() {
    override fun areItemsTheSame(oldItem: Episode, newItem: Episode): Boolean {
        return oldItem.getEpisodeId() == newItem.getEpisodeId()
    }

    override fun areContentsTheSame(oldItem: Episode, newItem: Episode): Boolean {
        return oldItem == newItem
    }
}