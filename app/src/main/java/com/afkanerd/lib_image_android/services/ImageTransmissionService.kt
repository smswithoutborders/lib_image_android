package com.afkanerd.lib_image_android.services

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ImageTransmissionService : Service() {
    lateinit var dividedPayload: List<ByteArray>

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val icon = intent?.getIntExtra(SmsWorkManager.ITP_SERVICE_ICON, -1)
            ?: return START_NOT_STICKY

        startForeground(1,
            createForegroundNotification(
                this,
                intent,
                icon = icon
            ).notification
        )

        val payload = intent.getByteArrayExtra(SmsWorkManager.ITP_PAYLOAD)
            ?: return START_NOT_STICKY

        val version = intent.getByteExtra( SmsWorkManager.ITP_VERSION, 0x0)

        val sessionId = intent.getByteExtra( SmsWorkManager.ITP_SESSION_ID, 0x0)

        val imageLength = intent.getShortExtra(SmsWorkManager.ITP_IMAGE_LENGTH,
            0)

        val textLength = intent.getShortExtra(SmsWorkManager.ITP_TEXT_LENGTH,
            0)

        CoroutineScope(Dispatchers.Default).launch {
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
    ) : ForegroundInfo {
        val pendingIntent = PendingIntent
            .getActivity(context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE)

        val title = "Long running..."
        val description = ""

        val builder = NotificationCompat.Builder(context, "0")
            .setContentTitle(title)
            .setContentText("Status")
            .setSmallIcon(icon)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setContentIntent(pendingIntent)
            .setProgress(100, 50, false)

//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
//            builder.setStyle(NotificationCompat.ProgressStyle())
//        } else {
//            builder.setStyle(NotificationCompat.BigTextStyle().bigText(description))
//        }

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
}