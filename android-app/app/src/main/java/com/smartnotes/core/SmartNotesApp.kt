package com.smartnotes.core

import android.app.Application
import com.smartnotes.ui.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartNotesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize locale from saved preference
        val lang = LocaleHelper.getSavedLocale(this)
        LocaleHelper.updateContextLocale(this, lang)
        // Create notification channels
        NotificationHelper.createChannels(this)
    }
}
