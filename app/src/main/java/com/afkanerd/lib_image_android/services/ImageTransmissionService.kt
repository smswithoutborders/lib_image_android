package com.afkanerd.lib_image_android.services

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.work.ForegroundInfo
import com.afkanerd.lib_image_android.R
import com.afkanerd.lib_image_android.data.ImageTransmissionProtocol
import com.afkanerd.lib_image_android.data.SmsWorkManager
import com.afkanerd.lib_image_android.data.getItpSession
import com.afkanerd.lib_image_android.extensions.toByteArray
import com.afkanerd.lib_image_android.receivers.NotificationActionImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ImageTransmissionService : Service() {
    lateinit var dividedPayload: List<ByteArray>

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods.
        fun getService(): ImageTransmissionService = this@ImageTransmissionService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.hasExtra(SmsWorkManager.ITP_STOP_SERVICE) == true) {
            stopSelf()
            return START_NOT_STICKY
        }

        val icon = intent?.getIntExtra(SmsWorkManager.ITP_SERVICE_ICON, -1)
            ?: return START_NOT_STICKY

        val payload = intent.getByteArrayExtra(SmsWorkManager.ITP_PAYLOAD)
            ?: return START_NOT_STICKY

        val version = intent.getByteExtra( SmsWorkManager.ITP_VERSION, 0x0)

        val sessionId = intent.getByteExtra( SmsWorkManager.ITP_SESSION_ID, 0x0)

        val imageLength = intent.getShortExtra(SmsWorkManager.ITP_IMAGE_LENGTH,
            0)

        val textLength = intent.getShortExtra(SmsWorkManager.ITP_TEXT_LENGTH,
            0)

        try {
            dividedPayload = divideImagePayload(
                payload = payload,
                version = version,
                sessionId = sessionId,
                imageLength = imageLength,
                textLength = textLength,
            )
        } catch(e: Exception) {
            e.printStackTrace()
        }

        try {
            startForeground(1,
                createForegroundNotification(
                    this@ImageTransmissionService,
                    intent,
                    icon = icon,
                    maxProgress = dividedPayload.size
                ).notification
            )
        } catch(e: Exception) {
            e.printStackTrace()
        }
        return START_STICKY
    }

    @Throws
    private fun divideImagePayload(
        payload: ByteArray,
        version: Byte,
        sessionId: Byte,
        imageLength: Short,
        textLength: Short,
    ): MutableList<ByteArray> {
        val segmentSize: Int = 3
        var encodedPayload = payload
        val standardSegmentSize = 150 * segmentSize
        val dividedImage = mutableListOf<ByteArray>()

        var segmentNumber = 0
        do {
            var metaData = byteArrayOf(version, sessionId, 0,)
            if(segmentNumber == 0) { metaData +=
                imageLength.toByteArray() + textLength.toByteArray() }

            val size = (standardSegmentSize - metaData.size)
                .coerceAtMost(encodedPayload.size)
            val buffer = metaData +  encodedPayload.take(size).toByteArray()
            if(buffer.size > standardSegmentSize) {
                throw Exception("Buffer size > $standardSegmentSize")
            }
            encodedPayload = encodedPayload.drop(buffer.size).toByteArray()

            segmentNumber += 1
            if(segmentNumber >= 256 / 2) {
                throw Exception("Segment number > ${256 /2 }")
            }

            dividedImage.add(buffer)
        } while(encodedPayload.isNotEmpty())

        return dividedImage
    }

    private fun createForegroundNotification(
        context: Context,
        intent: Intent,
        icon: Int,
        maxProgress: Int,
        progress: Int = 0,
    ) : ForegroundInfo {
        val pendingIntent = PendingIntent
            .getActivity(context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE)

        val title = getString(R.string.sending_images)
        val description = getString(R.string.of_sent, progress, maxProgress)

        val builder = NotificationCompat.Builder(context, "0")
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(icon)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setContentIntent(pendingIntent)
            .setProgress(maxProgress, 0, false).apply {
                getActions(context).forEach {
                    this.addAction(it)
                }
            }

        val notification = builder.build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                0, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(0, notification)
        }
    }

    private fun getActions(context: Context) : List<NotificationCompat.Action> {
        val stopLabel = getString(R.string.stop)
        val pauseLabel = getString(R.string.pause)

        val stopPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            0, // Or a unique request code
            Intent(
                this,
                NotificationActionImpl::class.java
            ).apply {
                action = NotificationActionImpl.NOTIFICATION_STOP_ACTION_INTENT_ACTION
            },
            PendingIntent.FLAG_MUTABLE // Flags for the PendingIntent
        )

        val pausePendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            1, // Or a unique request code
            Intent(
                this,
                NotificationActionImpl::class.java
            ).apply {
                action = NotificationActionImpl.NOTIFICATION_PAUSE_ACTION_INTENT_ACTION
            },
            PendingIntent.FLAG_MUTABLE // Flags for the PendingIntent
        )

        return listOf(
            NotificationCompat.Action.Builder(
                null, // Icon for the reply button
                stopLabel, // Text for the reply button
                stopPendingIntent
            ).setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MUTE).build(),

            NotificationCompat.Action.Builder(
                null, // Icon for the reply button
                pauseLabel, // Text for the reply button
                pausePendingIntent
            ).setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MUTE).build(),
        )
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        super.onTimeout(startId, fgsType)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        TODO("update sending caches")
    }

}