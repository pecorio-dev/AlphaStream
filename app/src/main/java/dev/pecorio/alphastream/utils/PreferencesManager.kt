package dev.pecorio.alphastream.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PREFS_NAME = "alphastream_preferences"
        
        // Theme preferences
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DYNAMIC_COLORS = "dynamic_colors"
        
        // Video preferences
        private const val KEY_DEFAULT_QUALITY = "default_quality"
        private const val KEY_AUTO_PLAY = "auto_play"
        private const val KEY_SKIP_INTRO = "skip_intro"
        
        // Cache preferences
        private const val KEY_CACHE_SIZE = "cache_size"
        private const val KEY_AUTO_CACHE_CLEANUP = "auto_cache_cleanup"
        private const val KEY_DOWNLOAD_OVER_WIFI_ONLY = "download_over_wifi_only"
        
        // Language preferences
        private const val KEY_LANGUAGE = "language"
        
        // Notification preferences
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_NEW_EPISODES_NOTIFICATIONS = "new_episodes_notifications"
        
        // Privacy preferences
        private const val KEY_ANALYTICS_ENABLED = "analytics_enabled"
        private const val KEY_CRASH_REPORTING = "crash_reporting"
        
        // First launch
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Theme Management
    var themeMode: Int
        get() = sharedPreferences.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = sharedPreferences.edit().putInt(KEY_THEME_MODE, value).apply()
    
    var isDynamicColorsEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_DYNAMIC_COLORS, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_DYNAMIC_COLORS, value).apply()
    
    // Video Settings
    var defaultVideoQuality: String
        get() = sharedPreferences.getString(KEY_DEFAULT_QUALITY, "auto") ?: "auto"
        set(value) = sharedPreferences.edit().putString(KEY_DEFAULT_QUALITY, value).apply()
    
    var isAutoPlayEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_AUTO_PLAY, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_AUTO_PLAY, value).apply()
    
    var isSkipIntroEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_SKIP_INTRO, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_SKIP_INTRO, value).apply()
    
    // Cache Settings
    var cacheSize: String
        get() = sharedPreferences.getString(KEY_CACHE_SIZE, "1GB") ?: "1GB"
        set(value) = sharedPreferences.edit().putString(KEY_CACHE_SIZE, value).apply()
    
    var isAutoCacheCleanupEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_AUTO_CACHE_CLEANUP, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_AUTO_CACHE_CLEANUP, value).apply()
    
    var isDownloadOverWifiOnly: Boolean
        get() = sharedPreferences.getBoolean(KEY_DOWNLOAD_OVER_WIFI_ONLY, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_DOWNLOAD_OVER_WIFI_ONLY, value).apply()
    
    // Language Settings
    var language: String
        get() = sharedPreferences.getString(KEY_LANGUAGE, "system") ?: "system"
        set(value) = sharedPreferences.edit().putString(KEY_LANGUAGE, value).apply()
    
    // Notification Settings
    var areNotificationsEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()
    
    var areNewEpisodesNotificationsEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_NEW_EPISODES_NOTIFICATIONS, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_NEW_EPISODES_NOTIFICATIONS, value).apply()
    
    // Privacy Settings
    var isAnalyticsEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_ANALYTICS_ENABLED, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_ANALYTICS_ENABLED, value).apply()
    
    var isCrashReportingEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_CRASH_REPORTING, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_CRASH_REPORTING, value).apply()
    
    // App State
    var isFirstLaunch: Boolean
        get() = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()
    
    var isOnboardingCompleted: Boolean
        get() = sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()
    
    /**
     * Apply theme changes immediately
     */
    fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }
    
    /**
     * Get theme mode as string for display
     */
    fun getThemeModeString(): String {
        return when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> "Clair"
            AppCompatDelegate.MODE_NIGHT_YES -> "Sombre"
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> "Système"
            else -> "Système"
        }
    }
    
    /**
     * Get video quality options
     */
    fun getVideoQualityOptions(): List<Pair<String, String>> {
        return listOf(
            "auto" to "Automatique",
            "4k" to "4K (2160p)",
            "1080p" to "Full HD (1080p)",
            "720p" to "HD (720p)",
            "480p" to "SD (480p)"
        )
    }
    
    /**
     * Get cache size options
     */
    fun getCacheSizeOptions(): List<Pair<String, String>> {
        return listOf(
            "500MB" to "500 MB",
            "1GB" to "1 GB",
            "2GB" to "2 GB",
            "5GB" to "5 GB",
            "10GB" to "10 GB"
        )
    }
    
    /**
     * Get language options
     */
    fun getLanguageOptions(): List<Pair<String, String>> {
        return listOf(
            "system" to "Système",
            "fr" to "Français",
            "en" to "English",
            "es" to "Español",
            "de" to "Deutsch"
        )
    }
    
    /**
     * Clear all preferences (reset to defaults)
     */
    fun clearAllPreferences() {
        sharedPreferences.edit().clear().apply()
    }
    
    /**
     * Clear cache-related preferences only
     */
    fun clearCachePreferences() {
        sharedPreferences.edit()
            .remove(KEY_CACHE_SIZE)
            .remove(KEY_AUTO_CACHE_CLEANUP)
            .apply()
    }
    
    /**
     * Export preferences as string (for backup)
     */
    fun exportPreferences(): String {
        val allPrefs = sharedPreferences.all
        return allPrefs.entries.joinToString("\n") { "${it.key}=${it.value}" }
    }
    
    /**
     * Get app version info
     */
    fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"
        } catch (e: Exception) {
            "Version inconnue"
        }
    }
    
    /**
     * Calculate cache usage (placeholder - would need actual implementation)
     */
    fun getCacheUsage(): String {
        return try {
            val cacheDir = context.cacheDir
            val size = calculateDirectorySize(cacheDir)
            formatFileSize(size)
        } catch (e: Exception) {
            "Inconnu"
        }
    }
    
    private fun calculateDirectorySize(directory: java.io.File): Long {
        var size = 0L
        if (directory.exists()) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }
    
    private fun formatFileSize(size: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        
        return when {
            size >= gb -> String.format("%.1f GB", size / gb)
            size >= mb -> String.format("%.1f MB", size / mb)
            size >= kb -> String.format("%.1f KB", size / kb)
            else -> "$size B"
        }
    }
    
    /**
     * Register preference change listener
     */
    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }
    
    /**
     * Unregister preference change listener
     */
    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}