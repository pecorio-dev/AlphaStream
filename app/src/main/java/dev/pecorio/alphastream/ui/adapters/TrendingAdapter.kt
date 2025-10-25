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
import dev.pecorio.alphastream.data.model.Movie
import dev.pecorio.alphastream.data.model.Series
import dev.pecorio.alphastream.databinding.ItemTrendingContentBinding

class TrendingAdapter(
    private val onItemClick: (Any) -> Unit
) : ListAdapter<Any, TrendingAdapter.TrendingViewHolder>(TrendingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrendingViewHolder {
        val binding = ItemTrendingContentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TrendingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrendingViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item)
        }
    }

    inner class TrendingViewHolder(
        private val binding: ItemTrendingContentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Any) {
            with(binding) {
                try {
                    when (item) {
                        is Movie -> bindMovie(item)
                        is Series -> bindSeries(item)
                        else -> {
                            // Handle unknown item type gracefully
                            trendingTitle.text = "Contenu inconnu"
                            trendingRating.text = "N/A"
                            trendingYear.text = ""
                            trendingQuality.visibility = android.view.View.GONE
                            trendingImage.setImageResource(R.drawable.placeholder_trending)
                        }
                    }
                    
                    // Gestion du clic
                    root.setOnClickListener {
                        onItemClick(item)
                    }
                    
                    // Animation d'entrée avec effet de parallaxe (avec protection)
                    try {
                        root.alpha = 0f
                        root.translationX = 200f
                        root.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(400)
                            .setStartDelay((bindingAdapterPosition * 100).toLong())
                            .start()
                    } catch (e: Exception) {
                        // Fallback: just show the item without animation
                        root.alpha = 1f
                        root.translationX = 0f
                    }
                } catch (e: Exception) {
                    // Fallback for any binding errors
                    trendingTitle.text = "Erreur de chargement"
                    trendingRating.text = "N/A"
                    trendingYear.text = ""
                    trendingQuality.visibility = android.view.View.GONE
                    trendingImage.setImageResource(R.drawable.placeholder_trending)
                }
            }
        }
        
        private fun bindMovie(movie: Movie) {
            with(binding) {
                // Titre
                trendingTitle.text = movie.getDisplayTitle()
                
                // Note
                movie.getDisplayRating()?.let { rating ->
                    trendingRating.text = String.format("%.1f", rating)
                } ?: run {
                    trendingRating.text = "N/A"
                }
                
                // Année
                movie.getDisplayReleaseDate()?.let { date ->
                    val year = if (date.length >= 4) date.substring(0, 4) else date
                    trendingYear.text = year
                } ?: run {
                    trendingYear.text = ""
                }
                
                // Badge de qualité
                movie.getQualityBadge()?.let { quality ->
                    trendingQuality.text = quality
                    trendingQuality.visibility = android.view.View.VISIBLE
                } ?: run {
                    trendingQuality.visibility = android.view.View.GONE
                }
                
                // Image with null safety
                val imageUrl = movie.getDisplayImageUrl()
                if (!imageUrl.isNullOrBlank()) {
                    try {
                        Glide.with(trendingImage.context)
                            .load(imageUrl)
                            .apply(
                                RequestOptions()
                                    .placeholder(R.drawable.placeholder_trending)
                                    .error(R.drawable.placeholder_trending)
                                    .transform(RoundedCorners(32))
                            )
                            .into(trendingImage)
                    } catch (e: Exception) {
                        trendingImage.setImageResource(R.drawable.placeholder_trending)
                    }
                } else {
                    trendingImage.setImageResource(R.drawable.placeholder_trending)
                }
            }
        }
        
        private fun bindSeries(series: Series) {
            with(binding) {
                // Titre
                trendingTitle.text = series.title
                
                // Note
                series.rating?.let { rating ->
                    trendingRating.text = String.format("%.1f", rating)
                } ?: run {
                    trendingRating.text = "N/A"
                }
                
                // Année
                series.releaseDate?.let { date ->
                    val year = if (date.length >= 4) date.substring(0, 4) else date
                    trendingYear.text = year
                } ?: run {
                    trendingYear.text = ""
                }
                
                // Badge de qualité
                series.getQualityBadge()?.let { quality ->
                    trendingQuality.text = quality
                    trendingQuality.visibility = android.view.View.VISIBLE
                } ?: run {
                    trendingQuality.visibility = android.view.View.GONE
                }
                
                // Image with null safety
                val imageUrl = series.getDisplayImageUrl()
                if (!imageUrl.isNullOrBlank()) {
                    try {
                        Glide.with(trendingImage.context)
                            .load(imageUrl)
                            .apply(
                                RequestOptions()
                                    .placeholder(R.drawable.placeholder_trending)
                                    .error(R.drawable.placeholder_trending)
                                    .transform(RoundedCorners(32))
                            )
                            .into(trendingImage)
                    } catch (e: Exception) {
                        trendingImage.setImageResource(R.drawable.placeholder_trending)
                    }
                } else {
                    trendingImage.setImageResource(R.drawable.placeholder_trending)
                }
            }
        }
    }

    private class TrendingDiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is Movie && newItem is Movie -> {
                    oldItem.title == newItem.title && oldItem.tmdbId == newItem.tmdbId
                }
                oldItem is Series && newItem is Series -> {
                    oldItem.id == newItem.id
                }
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return oldItem == newItem
        }
    }
}