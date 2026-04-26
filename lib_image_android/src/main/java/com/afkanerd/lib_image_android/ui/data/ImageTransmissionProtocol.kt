package com.afkanerd.smswithoutborders_libsmsmms.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkRequest
import coil3.network.NetworkRequest
import com.afkanerd.lib_image_android.ui.data.SmsWorkManager
import com.afkanerd.lib_image_android.ui.extensions.toLittleEndianBytes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.TimeUnit

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "itp_sessions")

@Serializable
data class ImageTransmissionProtocol(
    val version: Byte,
    val sessionId: Byte,
    val segNumber: Byte,
    val numberSegments: Byte,
    val imageLength: Byte, // only in first segment
    val textLength: Byte, // only in first segment
    val image: ByteArray,
    val text: ByteArray // follows std platform formatting
) {
    companion object {
        const val STANDARD_SEGMENT_SIZE = 153
        const val STANDARD_ENCODED_HEADER_SIZE = 12

        suspend fun startWorkManager(
            context: Context,
            formattedPayload: ByteArray,
            logo: Int,
            version: Byte,
            sessionId: Byte,
            imageLength: Short,
            textLength: Short,
            address: String,
            subscriptionId: Long,
        ): Operation {
            val constraints : Constraints = Constraints.Builder()
                .build();

            val workManager = WorkManager.getInstance(context)

            fun generateUuidFromLong(input: Long): UUID {
                // Generate a UUID from the long by using the input directly
                // for the most significant bits and setting the least significant bits to 0.
                val mostSigBits = input
                val leastSigBits = 0L // You can modify this if you want to use more of the long

                return UUID(mostSigBits, leastSigBits)
            }

            val remoteListenersListenerWorker = OneTimeWorkRequestBuilder<SmsWorkManager>()
                .setConstraints(constraints)
                .setId(generateUuidFromLong(System.currentTimeMillis()))
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .setInputData(Data.Builder()
                    .putInt(SmsWorkManager.ITP_SERVICE_ICON, logo)
                    .putByte(SmsWorkManager.ITP_VERSION, version)
                    .putByte(SmsWorkManager.ITP_SESSION_ID, sessionId)
                    .putByteArray(SmsWorkManager.ITP_IMAGE_LENGTH, imageLength.toLittleEndianBytes())
                    .putByteArray(SmsWorkManager.ITP_TEXT_LENGTH, textLength.toLittleEndianBytes())
                    .putString(SmsWorkManager.ITP_TRANSMISSION_ADDRESS, address)
                    .putLong(SmsWorkManager.ITP_TRANSMISSION_SUBSCRIPTION_ID, subscriptionId)
                    .build()
                )
                .addTag(SmsWorkManager.IMAGE_TRANSMISSION_WORK_MANAGER_TAG)
                .build();

            cacheImage(context, sessionId, formattedPayload)

            return workManager.enqueueUniqueWork(
                "${SmsWorkManager}.IMAGE_TRANSMISSION_WORK_MANAGER_TAG.${
                    System.currentTimeMillis()}",
                ExistingWorkPolicy.KEEP,
                remoteListenersListenerWorker
            )

        }

        suspend fun getTransmissionIndex(
            context: Context,
            sessionId: Byte,
        ) : Int? {
            val key = intPreferencesKey("session_index_$sessionId")
            return context.dataStore.data.firstOrNull()?.get(key)
        }

        suspend fun storeTransmissionSessionIndex(
            context: Context,
            sessionId: Byte,
            index: Int,
        ) {
            val key = intPreferencesKey("session_index_$sessionId")
            context.dataStore.edit { session ->
                session[key] = index
            }
        }

        suspend fun cacheImage(
            context: Context,
            sessionId: Byte,
            payload: ByteArray,
        ) {
            val key = byteArrayPreferencesKey("session_index_image_$sessionId")
            context.dataStore.edit { session ->
                session[key] = payload
            }
        }

        suspend fun getCacheImage(
            context: Context,
            sessionId: Byte,
        ) : ByteArray? {
            val key = byteArrayPreferencesKey("session_index_image_$sessionId")
            return context.dataStore.data.first()[key]
        }

        suspend fun clearImageCache(
            context: Context,
            sessionId: Byte,
        ) {
            val key = byteArrayPreferencesKey("session_index_image_$sessionId")
            context.dataStore.edit { it.remove(key) }
        }

        suspend fun getItpSession(context: Context) : Int {
            val sessionId = intPreferencesKey("session_id")
            context.dataStore.edit { session ->
                val currentSession = session[sessionId] ?: 0
                session[sessionId] = if(currentSession >=255) 0 else currentSession + 1
            }
            return context.dataStore.data.first()[sessionId]!!
        }

        /**
         * Message type
         * Character limit per message	Bytes for text	Bytes for UDH
         * Single SMS	160	140 bytes	0 bytes
         * Concatenated SMS	153	134 bytes	6 bytes
         */
        @Throws
        fun divideImagePayload(
            payload: ByteArray,
            version: Byte,
            sessionId: Byte,
            imageLength: ByteArray,
            textLength: ByteArray,
        ): MutableList<String> {
            var encodedPayload = payload
            val dividedImage = mutableListOf<String>()

            var segmentNumber: Byte = 0
            val numberOfSegments: Byte = 0
            do {
                var metaData: ByteArray = byteArrayOf(version, sessionId, segmentNumber)

                if(segmentNumber.toInt() == 0) {
                    metaData += byteArrayOf(numberOfSegments) + imageLength + textLength
                }

                val size = (STANDARD_SEGMENT_SIZE - STANDARD_ENCODED_HEADER_SIZE)
                    .coerceAtMost(encodedPayload.size)

                val buffer = Base64.encodeToString(metaData, Base64.NO_WRAP) +
                        String(encodedPayload.take(size).toByteArray(),
                    StandardCharsets.UTF_8)
                if(buffer.length > STANDARD_SEGMENT_SIZE) {
                    throw Exception("Buffer size > $STANDARD_SEGMENT_SIZE --> ${buffer.length}")
                }
                encodedPayload = encodedPayload.drop(size).toByteArray()

                segmentNumber = (segmentNumber.toInt() + 1).toByte()

                dividedImage.add(buffer)
            } while(encodedPayload.isNotEmpty())

            return dividedImage
        }

//        @Throws
//        fun extractImageContent(
//            payload: ByteArray,
//        ): ImageTransmissionProtocol? {
//            if(!payload.take(14).toByteArray().isHexBytes())
//                return null
//
//            var payload = payload
//            val firstSegmentMetaDataSize = 14
//            val laterSegmentMetaDataSize = 6
//
//            val listItp = mutableListOf<Pair<ImageTransmissionProtocol, String>>()
//
//            var segNumber = 0
//            do {
//                val size = (STANDARD_SEGMENT_SIZE -
//                        if(segNumber == 0) firstSegmentMetaDataSize else laterSegmentMetaDataSize)
//                    .coerceAtMost(payload.size)
//                val content = payload.take(size).also {
//                    payload = payload.drop(size).toByteArray()
//                }
//                parseImageContent(content.toByteArray())?.let {
//                    listItp.add(it)
//                }
//                segNumber += 1
//            } while(payload.isNotEmpty())
//
//            val version = listItp.first().first.version
//            val sessionId = listItp.first().first.sessionId
//            val numberSegments = listItp.first().first.numberSegments
//            val imageLength = listItp.first().first.imageLength
//            val textLength = listItp.first().first.textLength
//
//            if(listItp.size != numberSegments.toInt()) {
//                throw Exception("Composed list not equal expected segments: " +
//                        "${listItp.size} != $numberSegments")
//            }
//
//            var imagePayloadString = ""
//            listItp.forEach { imagePayloadString += it }
//
//            var imageTextPayload = Base64.decode(imagePayloadString, Base64.DEFAULT)
//
//            val image = imageTextPayload.take(imageLength.toInt()).also {
//                imageTextPayload = imageTextPayload.drop(imageLength.toInt()).toByteArray()
//            }
//            val text = imageTextPayload.take(textLength.toInt())
//
//            return ImageTransmissionProtocol(
//                version = version,
//                sessionId = sessionId,
//                segNumber = 0,
//                numberSegments = numberSegments,
//                imageLength = imageLength,
//                textLength = textLength,
//                image = image.toByteArray(),
//                text = text.toByteArray()
//            )
//        }

//        private fun parseImageContent(content: ByteArray): Pair<ImageTransmissionProtocol, String>? {
//            val headers = content.take(8).toByteArray()
//            var stringContent = String(headers, StandardCharsets.UTF_8)
//            val version = stringContent.take(2).toInt(16).also {
//                stringContent = stringContent.drop(2)
//            }
//            val sessionId = stringContent.take(2).toInt(16).also {
//                stringContent = stringContent.drop(2)
//            }
//
//            var segNumber = 0
//            var numberSegments = 0
//
//            val imageLength = if(segNumber == 0) stringContent.take(4).toUShort(16).toShort().also {
//                stringContent = stringContent.drop(4)
//            } else 0
//
//            val textLength = if(segNumber == 0) stringContent.take(4).toUShort(16).toShort().also {
//                stringContent = stringContent.drop(4)
//            } else 0
//
//            return Pair(ImageTransmissionProtocol(
//                version = version.toByte(),
//                sessionId = sessionId.toByte(),
//                segNumber = segNumber,
//                numberSegments = numberSegments,
//                imageLength = imageLength,
//                textLength = textLength,
//                image = byteArrayOf(),
//                text = byteArrayOf()
//            ), stringContent)
//        }


    }

}
