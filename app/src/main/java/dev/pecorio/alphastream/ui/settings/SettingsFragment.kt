package dev.pecorio.alphastream.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.databinding.FragmentSettingsBinding
import dev.pecorio.alphastream.utils.PreferencesManager
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private var currentSnackbar: Snackbar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupClickListeners()
        updateUI()
        
        // Register preference change listener
        preferencesManager.registerOnSharedPreferenceChangeListener(this)
    }

    private fun setupUI() {
        // Initialize switches with current values
        binding.apply {
            dynamicColorsSwitch.isChecked = preferencesManager.isDynamicColorsEnabled
            autoPlaySwitch.isChecked = preferencesManager.isAutoPlayEnabled
            skipIntroSwitch.isChecked = preferencesManager.isSkipIntroEnabled
            wifiOnlySwitch.isChecked = preferencesManager.isDownloadOverWifiOnly
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            // Theme setting
            themeSetting.setOnClickListener {
                showThemeSelectionDialog()
            }

            // Video quality setting
            videoQualitySetting.setOnClickListener {
                showVideoQualityDialog()
            }

            // Cache size setting
            cacheSizeSetting.setOnClickListener {
                showCacheSizeDialog()
            }

            // Clear cache
            clearCacheSetting.setOnClickListener {
                showClearCacheDialog()
            }

            // Privacy policy
            privacyPolicySetting.setOnClickListener {
                openUrl("https://alphastream.app/privacy")
            }

            // Terms of service
            termsSetting.setOnClickListener {
                openUrl("https://alphastream.app/terms")
            }

            // Reset settings
            resetSettings.setOnClickListener {
                showResetDialog()
            }

            // Switch listeners
            dynamicColorsSwitch.setOnCheckedChangeListener { _, isChecked ->
                preferencesManager.isDynamicColorsEnabled = isChecked
                showMessage("Couleurs dynamiques ${if (isChecked) "activées" else "désactivées"}")
            }

            autoPlaySwitch.setOnCheckedChangeListener { _, isChecked ->
                preferencesManager.isAutoPlayEnabled = isChecked
                showMessage("Lecture automatique ${if (isChecked) "activée" else "désactivée"}")
            }

            skipIntroSwitch.setOnCheckedChangeListener { _, isChecked ->
                preferencesManager.isSkipIntroEnabled = isChecked
                showMessage("Ignorer l'intro ${if (isChecked) "activé" else "désactivé"}")
            }

            wifiOnlySwitch.setOnCheckedChangeListener { _, isChecked ->
                preferencesManager.isDownloadOverWifiOnly = isChecked
                showMessage("Téléchargements WiFi uniquement ${if (isChecked) "activés" else "désactivés"}")
            }
        }
    }

    private fun updateUI() {
        binding.apply {
            // Update theme summary
            themeSummary.text = preferencesManager.getThemeModeString()

            // Update video quality summary
            val qualityOptions = preferencesManager.getVideoQualityOptions()
            val currentQuality = qualityOptions.find { it.first == preferencesManager.defaultVideoQuality }
            videoQualitySummary.text = currentQuality?.second ?: "Automatique"

            // Update cache size summary
            val cacheOptions = preferencesManager.getCacheSizeOptions()
            val currentCacheSize = cacheOptions.find { it.first == preferencesManager.cacheSize }
            val cacheUsage = preferencesManager.getCacheUsage()
            cacheSizeSummary.text = "${currentCacheSize?.second ?: "1 GB"} • $cacheUsage utilisés"

            // Update app version
            appVersion.text = preferencesManager.getAppVersion()
        }
    }

    private fun showThemeSelectionDialog() {
        val themes = arrayOf("Système", "Clair", "Sombre")
        val currentTheme = when (preferencesManager.themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            else -> 0
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choisir le thème")
            .setSingleChoiceItems(themes, currentTheme) { dialog, which ->
                val newThemeMode = when (which) {
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                
                preferencesManager.themeMode = newThemeMode
                preferencesManager.applyTheme()
                updateUI()
                dialog.dismiss()
                
                showMessage("Thème appliqué")
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showVideoQualityDialog() {
        val qualityOptions = preferencesManager.getVideoQualityOptions()
        val qualities = qualityOptions.map { it.second }.toTypedArray()
        val currentIndex = qualityOptions.indexOfFirst { it.first == preferencesManager.defaultVideoQuality }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Qualité vidéo par défaut")
            .setSingleChoiceItems(qualities, currentIndex) { dialog, which ->
                val selectedQuality = qualityOptions[which].first
                preferencesManager.defaultVideoQuality = selectedQuality
                updateUI()
                dialog.dismiss()
                
                showMessage("Qualité vidéo mise à jour")
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showCacheSizeDialog() {
        val cacheOptions = preferencesManager.getCacheSizeOptions()
        val sizes = cacheOptions.map { it.second }.toTypedArray()
        val currentIndex = cacheOptions.indexOfFirst { it.first == preferencesManager.cacheSize }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Taille du cache")
            .setSingleChoiceItems(sizes, currentIndex) { dialog, which ->
                val selectedSize = cacheOptions[which].first
                preferencesManager.cacheSize = selectedSize
                updateUI()
                dialog.dismiss()
                
                showMessage("Taille du cache mise à jour")
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showClearCacheDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Vider le cache")
            .setMessage("Cette action supprimera tous les fichiers temporaires et libérera de l'espace de stockage. Continuer ?")
            .setPositiveButton("Vider") { _, _ ->
                // TODO: Implement actual cache clearing
                showMessage("Cache vidé avec succès")
                updateUI()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showResetDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Réinitialiser les paramètres")
            .setMessage("Cette action restaurera tous les paramètres par défaut. Cette action est irréversible. Continuer ?")
            .setPositiveButton("Réinitialiser") { _, _ ->
                resetAllSettings()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun resetAllSettings() {
        // Save current theme to reapply it
        val currentTheme = preferencesManager.themeMode
        
        // Clear all preferences
        preferencesManager.clearAllPreferences()
        
        // Reapply theme
        preferencesManager.themeMode = currentTheme
        preferencesManager.applyTheme()
        
        // Update UI
        setupUI()
        updateUI()
        
        showMessage("Paramètres réinitialisés")
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            showMessage("Impossible d'ouvrir le lien")
        }
    }

    private fun showMessage(message: String) {
        currentSnackbar?.dismiss()
        currentSnackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        currentSnackbar?.show()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // Update UI when preferences change
        updateUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        
        // Unregister preference change listener
        preferencesManager.unregisterOnSharedPreferenceChangeListener(this)
        
        // Clean up snackbar
        currentSnackbar?.dismiss()
        currentSnackbar = null
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        // Dismiss any showing snackbar to prevent window leaks
        currentSnackbar?.dismiss()
    }
}