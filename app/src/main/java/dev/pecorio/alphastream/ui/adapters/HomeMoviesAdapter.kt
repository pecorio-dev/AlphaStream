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
import dev.pecorio.alphastream.databinding.ItemMovieCardBinding

class HomeMoviesAdapter(
    private val onMovieClick: (Movie) -> Unit
) : ListAdapter<Movie, HomeMoviesAdapter.MovieViewHolder>(MovieDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieCardBinding.inflate(
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
        private val binding: ItemMovieCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            with(binding) {
                // Titre
                movieTitle.text = movie.getDisplayTitle()
                
                // Note
                movie.getDisplayRating()?.let { rating ->
                    movieRating.text = String.format("%.1f", rating)
                    movieRating.visibility = android.view.View.VISIBLE
                } ?: run {
                    movieRating.visibility = android.view.View.GONE
                }
                
                // Année
                movie.getDisplayReleaseDate()?.let { date ->
                    val year = if (date.length >= 4) date.substring(0, 4) else date
                    movieYear.text = year
                    movieYear.visibility = android.view.View.VISIBLE
                } ?: run {
                    movieYear.visibility = android.view.View.GONE
                }
                
                // Badge de qualité
                movie.getQualityBadge()?.let { quality ->
                    movieQuality.text = quality
                    movieQuality.visibility = android.view.View.VISIBLE
                } ?: run {
                    movieQuality.visibility = android.view.View.GONE
                }
                
                // Image
                loadMovieImage(movie.getDisplayImageUrl())
                
                // Gestion du clic
                root.setOnClickListener {
                    onMovieClick(movie)
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
                Glide.with(binding.movieImage.context)
                    .load(validUrl)
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.placeholder_movie)
                            .error(R.drawable.placeholder_movie)
                            .fallback(R.drawable.placeholder_movie)
                            .transform(RoundedCorners(24))
                            .timeout(10000)
                    )
                    .into(binding.movieImage)
            } else {
                binding.movieImage.setImageResource(R.drawable.placeholder_movie)
            }
        }
    }

    private class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem.title == newItem.title && oldItem.tmdbId == newItem.tmdbId
        }

        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem == newItem
        }
    }
}