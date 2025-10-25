package dev.pecorio.alphastream.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.data.model.SearchResult
import dev.pecorio.alphastream.databinding.ItemSearchResultBinding

class SearchResultsAdapter(
    private val onItemClick: (SearchResult) -> Unit
) : ListAdapter<SearchResult, SearchResultsAdapter.SearchResultViewHolder>(SearchResultDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SearchResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item)
        }
    }

    inner class SearchResultViewHolder(
        private val binding: ItemSearchResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(searchResult: SearchResult) {
            binding.apply {
                // Title
                searchResultTitle.text = searchResult.getDisplayTitle()

                // Type badge
                searchResultType.text = when (searchResult.type) {
                    "movie" -> "FILM"
                    "series" -> "SÉRIE"
                    else -> "CONTENU"
                }

                // Rating
                searchResult.getDisplayRating()?.let { rating ->
                    searchResultRating.text = String.format("%.1f", rating)
                } ?: run {
                    searchResultRating.text = "N/A"
                }

                // Year
                searchResult.getDisplayReleaseDate()?.let { date ->
                    val year = if (date.length >= 4) date.substring(0, 4) else date
                    searchResultYear.text = year
                } ?: run {
                    searchResultYear.text = "N/A"
                }

                // Genres
                val genres = searchResult.getFormattedGenres()
                searchResultGenres.text = if (!genres.isNullOrBlank()) {
                    genres
                } else {
                    "Genres non spécifiés"
                }

                // Synopsis
                val synopsis = searchResult.getDisplayOverview()
                searchResultSynopsis.text = if (!synopsis.isNullOrBlank()) {
                    synopsis
                } else {
                    "Aucun synopsis disponible."
                }

                // Load poster image with null safety
                val imageUrl = searchResult.getDisplayImageUrl()
                if (!imageUrl.isNullOrBlank()) {
                    Glide.with(itemView.context)
                        .load(imageUrl)
                        .apply(
                            RequestOptions()
                                .placeholder(R.drawable.placeholder_movie)
                                .error(R.drawable.placeholder_movie)
                        )
                        .into(searchResultPoster)
                } else {
                    searchResultPoster.setImageResource(R.drawable.placeholder_movie)
                }

                // Click listener
                root.setOnClickListener {
                    onItemClick(searchResult)
                }
            }
        }
    }
}

class SearchResultDiffCallback : DiffUtil.ItemCallback<SearchResult>() {
    override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
        return oldItem.title == newItem.title && oldItem.type == newItem.type
    }

    override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
        return oldItem == newItem
    }
}