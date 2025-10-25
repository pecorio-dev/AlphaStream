package dev.pecorio.alphastream.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.pecorio.alphastream.databinding.ItemRecentSearchBinding

class RecentSearchesAdapter(
    private val onSearchClick: (String) -> Unit,
    private val onRemoveClick: (String) -> Unit
) : ListAdapter<String, RecentSearchesAdapter.RecentSearchViewHolder>(RecentSearchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentSearchViewHolder {
        val binding = ItemRecentSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecentSearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentSearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecentSearchViewHolder(
        private val binding: ItemRecentSearchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(searchQuery: String) {
            binding.apply {
                recentSearchText.text = searchQuery

                // Click to search again
                root.setOnClickListener {
                    onSearchClick(searchQuery)
                }

                // Click to remove from history
                removeRecentSearch.setOnClickListener {
                    onRemoveClick(searchQuery)
                }
            }
        }
    }
}

class RecentSearchDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }
}