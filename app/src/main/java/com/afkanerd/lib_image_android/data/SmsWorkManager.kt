package com.afkanerd.lib_image_android.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.afkanerd.lib_image_android.extensions.toIntLittleEndian
import com.afkanerd.lib_image_android.extensions.toShortLittleEndian
import com.afkanerd.lib_image_android.services.ImageTransmissionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

class SmsWorkManager(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams ) {

    private lateinit var messageStateChangedBroadcast: BroadcastReceiver

    val workValue = MutableStateFlow<Result?>(null)

    override suspend fun doWork(): Result {
        val itp = inputData.getByteArray(ITP_PAYLOAD)
            ?:
            return Result.failure(
                Data.Builder().putString("reason", "ITP_PAYLOAD null").build())

        val icon = inputData.getInt(ITP_SERVICE_ICON, -1).also {
            if(it == -1)
                return Result.failure(
                    Data.Builder().putString("reason", "ITP_SERVICE_ICON null").build())
        }

        val version = inputData.getByte(ITP_VERSION, -1).also {
            if(it.toInt() == -1)
                return Result.failure(
                    Data.Builder().putString("reason", "ITP_VERSION null").build())
        }

        val sessionId = inputData.getByte(ITP_SESSION_ID, -1).also {
            if(it.toInt() == -1)
                return Result.failure(
                    Data.Builder().putString("reason", "ITP_SESSION_ID null").build())
        }

        val imageLength = inputData.getByteArray(ITP_IMAGE_LENGTH).also {
            if(it == null)
                return Result.failure(
                    Data.Builder().putString("reason", "ITP_IMAGE_LENGTH null").build())
        }

        val textLength = inputData.getByteArray(ITP_TEXT_LENGTH).also {
            if(it == null)
                return Result.failure(
                    Data.Builder().putString("reason", "ITP_TEXT_LENGTH null").build())
        }

        val intent = Intent(
            applicationContext,
            ImageTransmissionService::class.java
        ).apply {
            putExtra(ITP_PAYLOAD, itp)
            putExtra(ITP_SERVICE_ICON, icon)
            putExtra(ITP_VERSION, version)
            putExtra(ITP_SESSION_ID, sessionId)
            putExtra(ITP_IMAGE_LENGTH, imageLength!!.toShortLittleEndian())
            putExtra(ITP_TEXT_LENGTH, textLength!!.toShortLittleEndian())
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
        } catch(e: Exception) {
            e.printStackTrace()
        }

        handleBroadcast()

//        workValue.first { it != null }
//
//        return workValue.value!!
        return Result.success()
    }

    /**
     *  Multiple incoming broadcast are expected at this point.
     *  Next message should only begin going out if previous one sends.
     */
    private fun handleBroadcast() {
        val action = "com.afkanerd.deku.SMS_SENT_BROADCAST_INTENT"
        val intentFilter = IntentFilter()
        intentFilter.addAction(action)
        messageStateChangedBroadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != null && intentFilter.hasAction(intent.action)) {
                }
            }
        }
        ContextCompat.registerReceiver(
            applicationContext,
            messageStateChangedBroadcast,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    companion object {
        const val ITP_PAYLOAD = "ITP_PAYLOAD"
        const val ITP_VERSION = "ITP_VERSION"
        const val ITP_SESSION_ID = "ITP_SESSION_ID"
        const val ITP_IMAGE_LENGTH = "ITP_IMAGE_LENGTH"
        const val ITP_TEXT_LENGTH = "ITP_TEXT_LENGTH"
        const val ITP_SERVICE_ICON = "ITP_SERVICE_ICON"
        const val IMAGE_TRANSMISSION_WORK_MANAGER_TAG = "IMAGE_TRANSMISSION_WORK_MANAGER_TAG"
    }

}