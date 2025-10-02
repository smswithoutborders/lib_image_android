package com.afkanerd.lib_image_android.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
        val itpIsBase64 = inputData.getBoolean(ITP_PAYLOAD_BASE64, false)
        val itp = inputData.getString(ITP_PAYLOAD).apply {
            if(itpIsBase64) String(Base64.decode(this, Base64.DEFAULT))
        } ?: return Result.failure()

        val intent = Intent(
            applicationContext,
            ImageTransmissionService::class.java
        ).apply { putExtra(ITP_PAYLOAD, itp) }

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

        workValue.first { it != null }

        return workValue.value!!
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
        const val ITP_PAYLOAD_BASE64 = "ITP_PAYLOAD_BASE64"
        const val FORMATTED_SMS_PAYLOAD = "FORMATTED_SMS_PAYLOAD"
        const val IMAGE_TRANSMISSION_WORK_MANAGER_TAG = "IMAGE_TRANSMISSION_WORK_MANAGER_TAG"
    }

}