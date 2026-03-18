package com.smartnotes.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.smartnotes.R

object NotificationHelper {

    private const val CHANNEL_REMINDERS = "channel_reminders"

    /**
     * Create notification channels required by the app.
     * Should be called once from Application.onCreate().
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.notification_channel_reminders),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_reminders_desc)
                enableVibration(true)
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(reminderChannel)
        }
    }

    /**
     * The reminder channel ID for posting notifications.
     */
    fun getReminderChannelId(): String = CHANNEL_REMINDERS

    /**
     * Check whether the POST_NOTIFICATIONS permission must be requested at runtime.
     * Only true on Android 13+ (API 33+).
     */
    fun needsNotificationPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
