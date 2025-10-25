package dev.pecorio.alphastream

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.pecorio.alphastream.utils.PreferencesManager
import javax.inject.Inject

@HiltAndroidApp
class AlphaStreamApplication : Application() {
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate() {
        super.onCreate()
        
        // Apply saved theme on app startup
        preferencesManager.applyTheme()
    }
}