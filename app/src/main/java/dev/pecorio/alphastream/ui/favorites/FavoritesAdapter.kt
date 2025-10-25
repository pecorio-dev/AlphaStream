package dev.pecorio.alphastream.ui.favorites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.data.model.Favorite
import dev.pecorio.alphastream.data.model.FavoriteType
import dev.pecorio.alphastream.databinding.ItemFavoriteBinding
import java.text.SimpleDateFormat
import java.util.*

class FavoritesAdapter(
    private val onItemClick: (Favorite) -> Unit,
    private val onRemoveClick: (Favorite) -> Unit,
    private val onPlayClick: (Favorite) -> Unit
) : ListAdapter<Favorite, FavoritesAdapter.FavoriteViewHolder>(FavoriteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemFavoriteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FavoriteViewHolder(
        private val binding: ItemFavoriteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(favorite: Favorite) {
            binding.apply {
                // Title
                favoriteTitle.text = favorite.title

                // Type badge
                typeBadge.text = when (favorite.type) {
                    FavoriteType.MOVIE -> "FILM"
                    FavoriteType.SERIES -> "SÉRIE"
                }

                // Description
                favoriteDescription.text = favorite.getDisplayDescription()

                // Genres
                favorite.getGenresString()?.let { genres ->
                    favoriteGenres.text = genres
                    favoriteGenres.visibility = View.VISIBLE
                } ?: run {
                    favoriteGenres.visibility = View.GONE
                }

                // Synopsis
                favorite.synopsis?.takeIf { it.isNotBlank() }?.let { synopsis ->
                    favoriteSynopsis.text = synopsis
                    favoriteSynopsis.visibility = View.VISIBLE
                } ?: run {
                    favoriteSynopsis.visibility = View.GONE
                }

                // Added date
                val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                val addedDateString = dateFormat.format(Date(favorite.addedAt))
                addedDate.text = "Ajouté le $addedDateString"

                // Poster image
                favorite.getDisplayImageUrl()?.let { imageUrl ->
                    Glide.with(itemView.context)
                        .load(imageUrl)
                        .apply(
                            RequestOptions()
                                .placeholder(when (favorite.type) {
                                    FavoriteType.MOVIE -> R.drawable.placeholder_movie
                                    FavoriteType.SERIES -> R.drawable.placeholder_series
                                })
                                .error(when (favorite.type) {
                                    FavoriteType.MOVIE -> R.drawable.placeholder_movie
                                    FavoriteType.SERIES -> R.drawable.placeholder_series
                                })
                                .centerCrop()
                        )
                        .into(favoritePoster)
                } ?: run {
                    favoritePoster.setImageResource(when (favorite.type) {
                        FavoriteType.MOVIE -> R.drawable.placeholder_movie
                        FavoriteType.SERIES -> R.drawable.placeholder_series
                    })
                }

                // Watch progress
                if (favorite.watchProgress > 0f) {
                    watchProgress.progress = (favorite.watchProgress * 100).toInt()
                    watchProgress.visibility = View.VISIBLE
                } else {
                    watchProgress.visibility = View.GONE
                }

                // Completed badge
                completedBadge.visibility = if (favorite.isCompleted) View.VISIBLE else View.GONE

                // Click listeners
                root.setOnClickListener {
                    onItemClick(favorite)
                }

                removeFavoriteButton.setOnClickListener {
                    onRemoveClick(favorite)
                }

                playButton.setOnClickListener {
                    onPlayClick(favorite)
                }
            }
        }
    }
}

class FavoriteDiffCallback : DiffUtil.ItemCallback<Favorite>() {
    override fun areItemsTheSame(oldItem: Favorite, newItem: Favorite): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Favorite, newItem: Favorite): Boolean {
        return oldItem == newItem
    }
}