package dev.pecorio.alphastream.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.data.model.WatchProgress
import dev.pecorio.alphastream.databinding.DialogResumeWatchingBinding

class ResumeWatchingDialog : DialogFragment() {
    
    private var _binding: DialogResumeWatchingBinding? = null
    private val binding get() = _binding!!
    
    private var watchProgress: WatchProgress? = null
    private var onResumeClicked: ((WatchProgress) -> Unit)? = null
    private var onStartOverClicked: (() -> Unit)? = null
    
    companion object {
        fun newInstance(
            watchProgress: WatchProgress,
            onResumeClicked: (WatchProgress) -> Unit,
            onStartOverClicked: () -> Unit
        ): ResumeWatchingDialog {
            return ResumeWatchingDialog().apply {
                this.watchProgress = watchProgress
                this.onResumeClicked = onResumeClicked
                this.onStartOverClicked = onStartOverClicked
            }
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogResumeWatchingBinding.inflate(LayoutInflater.from(requireContext()))
        
        setupContent()
        
        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setCancelable(true)
            .create()
    }
    
    private fun setupContent() {
        val progress = watchProgress ?: return
        
        // Title and subtitle
        binding.contentTitle.text = progress.title
        if (progress.subtitle != null) {
            binding.contentSubtitle.text = progress.subtitle
            binding.contentSubtitle.visibility = View.VISIBLE
        } else {
            binding.contentSubtitle.visibility = View.GONE
        }
        
        // Progress info
        binding.progressText.text = "Reprendre à ${progress.getFormattedCurrentPosition()}"
        binding.timeRemaining.text = "Il reste ${progress.getFormattedTimeRemaining()}"
        
        // Progress bar
        binding.progressBar.progress = (progress.progressPercent * 100).toInt()
        binding.progressPercentage.text = "${(progress.progressPercent * 100).toInt()}%"
        
        // Image
        progress.imageUrl?.let { imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.placeholder_movie)
                        .error(R.drawable.placeholder_movie)
                        .centerCrop()
                )
                .into(binding.contentImage)
        } ?: run {
            binding.contentImage.setImageResource(
                if (progress.contentType == "movie") R.drawable.placeholder_movie 
                else R.drawable.placeholder_series
            )
        }
        
        // Content type icon
        binding.contentTypeIcon.setImageResource(
            if (progress.contentType == "movie") R.drawable.ic_movie
            else R.drawable.ic_tv
        )
        
        // Buttons
        binding.resumeButton.setOnClickListener {
            onResumeClicked?.invoke(progress)
            dismiss()
        }
        
        binding.startOverButton.setOnClickListener {
            onStartOverClicked?.invoke()
            dismiss()
        }
        
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}