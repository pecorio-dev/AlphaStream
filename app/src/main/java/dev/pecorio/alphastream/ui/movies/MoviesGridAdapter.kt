package dev.pecorio.alphastream.ui.movies

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
import dev.pecorio.alphastream.databinding.ItemMovieGridBinding

class MoviesGridAdapter(
    private val onMovieClick: (Movie) -> Unit,
    private val onFavoriteClick: (Movie) -> Unit
) : ListAdapter<Movie, MoviesGridAdapter.MovieViewHolder>(MovieDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MovieViewHolder(
        private val binding: ItemMovieGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            binding.apply {
                // Title
                movieTitle.text = movie.getDisplayTitle()

                // Year
                movie.getDisplayReleaseDate()?.let { date ->
                    val year = if (date.length >= 4) date.substring(0, 4) else date
                    movieYear.text = year
                    movieYear.visibility = View.VISIBLE
                } ?: run {
                    movieYear.visibility = View.GONE
                }

                // Genre
                movie.getFormattedGenres()?.let { genres ->
                    movieGenre.text = genres.split(", ").firstOrNull() ?: "Genre"
                    movieGenre.visibility = View.VISIBLE
                } ?: run {
                    movieGenre.text = "Genre"
                    movieGenre.visibility = View.VISIBLE
                }

                // Rating
                movie.getDisplayRating()?.let { rating ->
                    movieRating.text = String.format("%.1f", rating)
                    ratingContainer.visibility = View.VISIBLE
                } ?: run {
                    ratingContainer.visibility = View.GONE
                }

                // Quality
                movie.getQualityBadge()?.let { quality ->
                    movieQuality.text = quality
                    movieQuality.visibility = View.VISIBLE
                } ?: run {
                    movieQuality.visibility = View.GONE
                }

                // Duration (if available from synopsis or other fields)
                // For now, hide it as it's not available in the API
                movieDuration.visibility = View.GONE

                // Load poster image
                loadMovieImage(movie.getDisplayImageUrl())
                
                // Click listeners
                root.setOnClickListener {
                    onMovieClick(movie)
                }
                
                root.setOnLongClickListener {
                    onFavoriteClick(movie)
                    true
                }
            }
        }
        
        private fun loadMovieImage(imageUrl: String?) {
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
                            .placeholder(R.drawable.placeholder_movie)
                            .error(R.drawable.placeholder_movie)
                            .fallback(R.drawable.placeholder_movie)
                            .centerCrop()
                            .timeout(10000)
                    )
                    .into(binding.moviePoster)
            } else {
                binding.moviePoster.setImageResource(R.drawable.placeholder_movie)
            }
        }
        
        init {
            binding.apply {
                // Click listeners will be set in bind method
                root.setOnFocusChangeListener { _, hasFocus ->
                    // Utiliser post pour éviter requestLayout() pendant le layout
                    root.post {
                        playOverlay.visibility = if (hasFocus) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }
}

class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
    override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
        return oldItem.title == newItem.title && oldItem.tmdbId == newItem.tmdbId
    }

    override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
        return oldItem == newItem
    }
}