package com.afkanerd.lib_image_android.services

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.work.ForegroundInfo
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
        val formattedPayload = intent!!
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


}