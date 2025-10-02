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
import com.afkanerd.lib_image_android.extensions.toByteArray
import kotlinx.serialization.json.Json

class ImageTransmissionService : Service() {
    lateinit var dividedPayload: List<ByteArray>

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1,
            createForegroundNotification(this, intent!!).notification)

        val formattedPayload = intent
            .getByteArrayExtra(SmsWorkManager.FORMATTED_SMS_PAYLOAD)
            ?: return START_NOT_STICKY

        val itp = intent.getStringExtra(SmsWorkManager.Companion.ITP_PAYLOAD).let {
            if(it == null) return START_NOT_STICKY
            Json.Default.decodeFromString<ImageTransmissionProtocol>(it)
        }

        try {
            dividedPayload = divideImagePayload(
                formattedPayload,
                itp
            )
        } catch(e: Exception) {
            e.printStackTrace()
        }

        while(true) {
            Thread.sleep(5000)
        }
        return START_STICKY
    }

    @Throws
    private fun divideImagePayload(
        payload: ByteArray,
        imageTransmissionProtocol: ImageTransmissionProtocol,
    ): MutableList<ByteArray> {
        val segmentSize: Int = 3
        var encodedPayload = payload
        val standardSegmentSize = 150 * segmentSize
        val dividedImage = mutableListOf<ByteArray>()

        var segmentNumber = 0
        do {
            var metaData = byteArrayOf(
                imageTransmissionProtocol.version,
                imageTransmissionProtocol.sessionId,
                imageTransmissionProtocol.getSegNumberNumberSegment(segmentNumber),
            )
            if(segmentNumber == 0) {
                metaData +=
                    imageTransmissionProtocol.imageLength.toByteArray() +
                            imageTransmissionProtocol.textLength.toByteArray()
            }

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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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