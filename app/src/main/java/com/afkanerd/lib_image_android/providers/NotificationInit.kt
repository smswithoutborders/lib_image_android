package com.afkanerd.lib_image_android.providers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import androidx.startup.Initializer
import androidx.work.WorkManagerInitializer
import com.afkanerd.lib_image_android.R
import java.util.prefs.Preferences

class NotificationInit : Initializer<NotificationManager> {
    companion object {
        const val TRANSMISSION_SERVICE_NOTIF_ID = "0"
    }

    override fun create(context: Context): NotificationManager {
        val notificationManager: NotificationManager =
            context.getSystemService( NotificationManager::class.java )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context, notificationManager)
        }
        return notificationManager
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> {
        return listOf(WorkManagerInitializer::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        context: Context,
        notificationManager: NotificationManager
    ) {
        createNotificationChannelIncomingMessage(context, notificationManager)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannelIncomingMessage(
        context: Context,
        notificationManager: NotificationManager
    ) {
        val importance = NotificationManager.IMPORTANCE_HIGH

        val channel = NotificationChannel(
            TRANSMISSION_SERVICE_NOTIF_ID,
            context.getString(R.string.image_transmission_service),
            importance
        )
        channel.enableLights(true)
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

        notificationManager.createNotificationChannel(channel)
    }
}