package dev.pecorio.alphastream.ui.tv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.data.model.Movie
import dev.pecorio.alphastream.data.model.Series
import dev.pecorio.alphastream.databinding.ItemTvContentBinding
import dev.pecorio.alphastream.utils.ImageUrlHelper

/**
 * Adapter pour afficher le contenu (films/séries) dans une interface TV
 */
class TVContentAdapter(
    private val onItemClick: (Any) -> Unit,
    private val onItemFocus: (Any) -> Unit = {}
) : ListAdapter<Any, TVContentAdapter.TVContentViewHolder>(TVContentDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TVContentViewHolder {
        val binding = ItemTvContentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TVContentViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: TVContentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class TVContentViewHolder(
        private val binding: ItemTvContentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        init {
            // Configuration TV
            binding.root.apply {
                isFocusable = true
                isFocusableInTouchMode = false
                isClickable = true
                
                setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClick(getItem(position))
                    }
                }
                
                setOnFocusChangeListener { _, hasFocus ->
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION && hasFocus) {
                        onItemFocus(getItem(position))
                    }
                }
            }
        }
        
        fun bind(item: Any) {
            when (item) {
                is Movie -> bindMovie(item)
                is Series -> bindSeries(item)
            }
        }
        
        private fun bindMovie(movie: Movie) {
            binding.apply {
                // Titre - utiliser le titre principal, pas le titre TMDB
                contentTitle.text = movie.title
                
                // Sous-titre avec année et qualité
                val subtitle = buildString {
                    movie.getDisplayReleaseDate()?.let { date ->
                        if (date.length >= 4) {
                            append(date.substring(0, 4))
                        }
                    }
                    movie.getQualityBadge()?.let { quality ->
                        if (isNotEmpty()) append(" • ")
                        append(quality)
                    }
                }
                contentSubtitle.text = subtitle.ifEmpty { "Film" }
                contentSubtitle.visibility = View.VISIBLE
                
                // Description
                val description = movie.getDisplayOverview()
                if (!description.isNullOrBlank()) {
                    contentDescription.text = description
                    contentDescription.visibility = View.VISIBLE
                } else {
                    contentDescription.visibility = View.GONE
                }
                
                // Note
                movie.getDisplayRating()?.let { rating ->
                    contentRating.text = String.format("%.1f", rating)
                    contentRating.visibility = View.VISIBLE
                    ratingIcon.visibility = View.VISIBLE
                } ?: run {
                    contentRating.visibility = View.GONE
                    ratingIcon.visibility = View.GONE
                }
                
                // Genres
                movie.getFormattedGenres()?.let { genres ->
                    contentGenres.text = genres
                    contentGenres.visibility = View.VISIBLE
                } ?: run {
                    contentGenres.visibility = View.GONE
                }
                
                // Image
                loadImage(movie.getDisplayImageUrl())
                
                // Type indicator
                contentTypeIndicator.text = "FILM"
                contentTypeIndicator.setBackgroundResource(R.drawable.bg_movie_indicator)
            }
        }
        
        private fun bindSeries(series: Series) {
            binding.apply {
                // Titre - utiliser le titre principal, pas le titre TMDB
                contentTitle.text = series.title
                
                // Sous-titre avec saisons/épisodes
                val subtitle = series.getDisplayDescription()
                contentSubtitle.text = subtitle
                contentSubtitle.visibility = View.VISIBLE
                
                // Description
                val description = series.getDisplayOverview()
                if (!description.isNullOrBlank()) {
                    contentDescription.text = description
                    contentDescription.visibility = View.VISIBLE
                } else {
                    contentDescription.visibility = View.GONE
                }
                
                // Note
                series.getDisplayRating()?.let { rating ->
                    contentRating.text = String.format("%.1f", rating)
                    contentRating.visibility = View.VISIBLE
                    ratingIcon.visibility = View.VISIBLE
                } ?: run {
                    contentRating.visibility = View.GONE
                    ratingIcon.visibility = View.GONE
                }
                
                // Genres
                series.getFormattedGenres()?.let { genres ->
                    contentGenres.text = genres
                    contentGenres.visibility = View.VISIBLE
                } ?: run {
                    contentGenres.visibility = View.GONE
                }
                
                // Image
                loadImage(series.getDisplayImageUrl())
                
                // Type indicator
                contentTypeIndicator.text = "SÉRIE"
                contentTypeIndicator.setBackgroundResource(R.drawable.bg_series_indicator)
            }
        }
        
        private fun loadImage(imageUrl: String?) {
            // Utiliser la validation de ImageUrlHelper
            val validUrl = if (ImageUrlHelper.isValidImageUrl(imageUrl)) imageUrl else null
            
            Glide.with(binding.contentPoster.context)
                .load(validUrl)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.placeholder_movie)
                        .error(R.drawable.placeholder_movie)
                        .fallback(R.drawable.placeholder_movie)
                        .centerCrop()
                        .timeout(10000) // 10 secondes timeout
                )
                .into(binding.contentPoster)
        }
    }
}

class TVContentDiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is Movie && newItem is Movie -> oldItem.getMovieId() == newItem.getMovieId()
            oldItem is Series && newItem is Series -> oldItem.getSeriesId() == newItem.getSeriesId()
            else -> false
        }
    }
    
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is Movie && newItem is Movie -> oldItem == newItem
            oldItem is Series && newItem is Series -> oldItem == newItem
            else -> false
        }
    }
}