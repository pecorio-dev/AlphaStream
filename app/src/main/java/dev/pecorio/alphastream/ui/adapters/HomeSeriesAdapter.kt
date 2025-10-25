package dev.pecorio.alphastream.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.data.model.Series
import dev.pecorio.alphastream.databinding.ItemSeriesCardBinding

class HomeSeriesAdapter(
    private val onSeriesClick: (Series) -> Unit
) : ListAdapter<Series, HomeSeriesAdapter.SeriesViewHolder>(SeriesDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesViewHolder {
        val binding = ItemSeriesCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SeriesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeriesViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item)
        }
    }

    inner class SeriesViewHolder(
        private val binding: ItemSeriesCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(series: Series) {
            with(binding) {
                // Titre
                seriesTitle.text = series.getDisplayTitle()
                
                // Note
                series.getDisplayRating()?.let { rating ->
                    seriesRating.text = String.format("%.1f", rating)
                    seriesRating.visibility = android.view.View.VISIBLE
                } ?: run {
                    seriesRating.visibility = android.view.View.GONE
                }
                
                // Année
                series.getDisplayReleaseDate()?.let { date ->
                    val year = if (date.length >= 4) date.substring(0, 4) else date
                    seriesYear.text = year
                    seriesYear.visibility = android.view.View.VISIBLE
                } ?: run {
                    seriesYear.visibility = android.view.View.GONE
                }
                
                // Badge de qualité
                series.getQualityBadge()?.let { quality ->
                    seriesQuality.text = quality
                    seriesQuality.visibility = android.view.View.VISIBLE
                } ?: run {
                    seriesQuality.visibility = android.view.View.GONE
                }
                
                // Informations sur les saisons/épisodes avec null safety
                val seasonsCount = try { series.getTotalSeasons() } catch (e: Exception) { 0 }
                val episodesCount = try { series.getTotalEpisodes() } catch (e: Exception) { 0 }
                
                when {
                    seasonsCount > 0 && episodesCount > 0 -> {
                        seriesInfo.text = "$seasonsCount saison${if (seasonsCount > 1) "s" else ""} • $episodesCount épisode${if (episodesCount > 1) "s" else ""}"
                        seriesInfo.visibility = android.view.View.VISIBLE
                    }
                    seasonsCount > 0 -> {
                        seriesInfo.text = "$seasonsCount saison${if (seasonsCount > 1) "s" else ""}"
                        seriesInfo.visibility = android.view.View.VISIBLE
                    }
                    else -> {
                        seriesInfo.visibility = android.view.View.GONE
                    }
                }
                
                // Image with null safety
                val imageUrl = series.getDisplayImageUrl()
                if (!imageUrl.isNullOrBlank()) {
                    try {
                        Glide.with(seriesImage.context)
                            .load(imageUrl)
                            .apply(
                                RequestOptions()
                                    .placeholder(R.drawable.placeholder_series)
                                    .error(R.drawable.placeholder_series)
                                    .transform(RoundedCorners(24))
                            )
                            .into(seriesImage)
                    } catch (e: Exception) {
                        seriesImage.setImageResource(R.drawable.placeholder_series)
                    }
                } else {
                    seriesImage.setImageResource(R.drawable.placeholder_series)
                }
                
                // Gestion du clic
                root.setOnClickListener {
                    onSeriesClick(series)
                }
            }
        }
    }

    private class SeriesDiffCallback : DiffUtil.ItemCallback<Series>() {
        override fun areItemsTheSame(oldItem: Series, newItem: Series): Boolean {
            return oldItem.getSeriesId() == newItem.getSeriesId()
        }

        override fun areContentsTheSame(oldItem: Series, newItem: Series): Boolean {
            return oldItem == newItem
        }
    }
}