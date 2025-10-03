package com.afkanerd.lib_image_android.receivers

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.afkanerd.lib_image_android.data.SmsWorkManager
import com.afkanerd.lib_image_android.services.ImageTransmissionService

class NotificationActionImpl: BroadcastReceiver() {
    companion object {
        const val NOTIFICATION_STOP_ACTION_INTENT_ACTION = "NOTIFICATION_STOP_ACTION_INTENT_ACTION"
        const val NOTIFICATION_PAUSE_ACTION_INTENT_ACTION = "NOTIFICATION_PAUSE_ACTION_INTENT_ACTION"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent == null) return

        when(intent.action) {
            NOTIFICATION_STOP_ACTION_INTENT_ACTION -> {
                context?.startService(Intent(context,
                    ImageTransmissionService::class.java).apply {
                        putExtra(SmsWorkManager.ITP_STOP_SERVICE, true)
                } )
            }
            NOTIFICATION_PAUSE_ACTION_INTENT_ACTION -> {
            }
        }
    }
}