package com.afkanerd.lib_image_android.ui.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.afkanerd.lib_image_android.R

object Notifications {

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createImageTransmissionChannel(
        context: Context,
        notificationManager: NotificationManager
    ) {
        val importance = NotificationManager.IMPORTANCE_LOW

        val channel = NotificationChannel(
            context.getString(R.string.foreground_service_image_transmission_channel_id),
            context.getString(R.string.image_transmission_service),
            importance
        )
        channel.description = context.getString(R.string.foreground_service_image_transmission_descriptions)
        channel.lockscreenVisibility = Notification.DEFAULT_ALL

        notificationManager.createNotificationChannel(channel)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannelFailedMessages(
        context: Context,
        notificationManager: NotificationManager
    ) {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(
            context.getString(R.string.foreground_service_failed_channel_id),
            context.getString(R.string.foreground_service_failed_channel_name),
            importance
        )
        channel.description = context.getString(R.string.message_failed_notifications_descriptions)
        channel.lockscreenVisibility = Notification.DEFAULT_ALL
        notificationManager.createNotificationChannel(channel)
    }

}