package dev.pecorio.alphastream.ui.series

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.data.model.Series
import dev.pecorio.alphastream.databinding.ItemSeriesGridBinding

class SeriesGridAdapter(
    private val onSeriesClick: (Series) -> Unit,
    private val onFavoriteClick: (Series) -> Unit
) : ListAdapter<Series, SeriesGridAdapter.SeriesViewHolder>(SeriesDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesViewHolder {
        val binding = ItemSeriesGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SeriesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeriesViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SeriesViewHolder(
        private val binding: ItemSeriesGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(series: Series) {
            binding.apply {
                // Title
                seriesTitle.text = series.getDisplayTitle()

                // Year
                series.getDisplayReleaseDate()?.let { date ->
                    val year = if (date.length >= 4) date.substring(0, 4) else date
                    seriesYear.text = year
                    seriesYear.visibility = View.VISIBLE
                } ?: run {
                    seriesYear.visibility = View.GONE
                }

                // Genre
                series.getFormattedGenres()?.let { genres ->
                    seriesGenre.text = genres.split(", ").firstOrNull() ?: "Genre"
                    seriesGenre.visibility = View.VISIBLE
                } ?: run {
                    seriesGenre.text = "Genre"
                    seriesGenre.visibility = View.VISIBLE
                }

                // Rating
                series.getDisplayRating()?.let { rating ->
                    seriesRating.text = String.format("%.1f", rating)
                    ratingContainer.visibility = View.VISIBLE
                } ?: run {
                    ratingContainer.visibility = View.GONE
                }

                // Quality
                series.getQualityBadge()?.let { quality ->
                    seriesQuality.text = quality
                    seriesQuality.visibility = View.VISIBLE
                } ?: run {
                    seriesQuality.visibility = View.GONE
                }

                // Status
                series.status?.let { status ->
                    seriesStatus.text = when (status.lowercase()) {
                        "ended", "completed" -> "TERMINÉE"
                        "continuing", "ongoing" -> "EN COURS"
                        "canceled", "cancelled" -> "ANNULÉE"
                        else -> status.uppercase()
                    }
                    seriesStatus.visibility = View.VISIBLE
                } ?: run {
                    seriesStatus.visibility = View.GONE
                }

                // Seasons info
                series.seasons?.let { seasons ->
                    val seasonCount = seasons.size
                    val episodeCount = seasons.sumOf { it.episodes?.size ?: 0 }
                    
                    val seasonsText = when {
                        seasonCount > 1 && episodeCount > 0 -> "$seasonCount saisons • $episodeCount épisodes"
                        seasonCount > 1 -> "$seasonCount saisons"
                        episodeCount > 0 -> "$episodeCount épisodes"
                        else -> null
                    }
                    
                    seasonsText?.let {
                        seriesSeasons.text = it
                        seriesSeasons.visibility = View.VISIBLE
                    } ?: run {
                        seriesSeasons.visibility = View.GONE
                    }
                } ?: run {
                    seriesSeasons.visibility = View.GONE
                }

                // Load poster image
                loadSeriesImage(series.getDisplayImageUrl())

                // Data completeness indicator
                dataCompletenessIndicator.setSeries(series)
                
                // Click listeners
                root.setOnClickListener {
                    onSeriesClick(series)
                }

                // Show play overlay on hover/focus (for better UX)
                root.setOnFocusChangeListener { _, hasFocus ->
                    // Utiliser post pour éviter requestLayout() pendant le layout
                    root.post {
                        playOverlay.visibility = if (hasFocus) View.VISIBLE else View.GONE
                    }
                }

                // Long click for favorites (alternative to dedicated button)
                root.setOnLongClickListener {
                    onFavoriteClick(series)
                    true
                }
            }
        }
        
        private fun loadSeriesImage(imageUrl: String?) {
            // Vérifier si l'URL est valide avant de charger
            val validUrl = imageUrl?.takeIf { 
                it.isNotBlank() && 
                (it.startsWith("http://") || it.startsWith("https://")) &&
                !it.contains("localhost") &&
                !it.contains("127.0.0.1")
            }
            
            if (validUrl != null) {
                Glide.with(itemView.context)
                    .load(validUrl)
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.placeholder_series)
                            .error(R.drawable.placeholder_series)
                            .fallback(R.drawable.placeholder_series)
                            .centerCrop()
                            .timeout(10000)
                    )
                    .into(binding.seriesPoster)
            } else {
                binding.seriesPoster.setImageResource(R.drawable.placeholder_series)
            }
        }
    }
}

class SeriesDiffCallback : DiffUtil.ItemCallback<Series>() {
    override fun areItemsTheSame(oldItem: Series, newItem: Series): Boolean {
        return oldItem.id == newItem.id && oldItem.title == newItem.title
    }

    override fun areContentsTheSame(oldItem: Series, newItem: Series): Boolean {
        return oldItem == newItem
    }
}