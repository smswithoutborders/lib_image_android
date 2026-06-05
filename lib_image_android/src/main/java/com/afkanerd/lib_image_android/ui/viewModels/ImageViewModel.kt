package com.afkanerd.lib_image_android.ui.viewModels

import android.R.attr.bitmap
import android.R.attr.height
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.telephony.SmsManager
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.afkanerd.lib_image_android.R
import com.afkanerd.lib_image_android.ui.data.ImageTransmissionNotification
import com.afkanerd.lib_image_android.ui.data.SmsWorkManager
import com.afkanerd.lib_image_android.ui.extensions.toBitmap
import com.afkanerd.lib_image_android.ui.extensions.toLittleEndianBytes
import com.afkanerd.lib_image_android.ui.services.ImageTransmissionService
import com.afkanerd.smswithoutborders_libsmsmms.data.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.TimeUnit

class ImageViewModel: ViewModel() {

    private val STANDARD_SEGMENT_SIZE = 153
    private val STANDARD_ENCODED_HEADER_SIZE = 12

    @Serializable
    data class ProcessedImage(
        @Transient
        var image: Bitmap? = null,
        var uri: String,
        var format: String = "raw",
        var rawBytes: ByteArray? = null,
    )


    private val _processingImageUiState = MutableStateFlow(false)
    val processingImageUiState: StateFlow<Boolean> = _processingImageUiState

    private val _processedImage = MutableStateFlow<ProcessedImage?>(null)
    val processedImage: StateFlow<ProcessedImage?> = _processedImage

    private val _smsCount = MutableStateFlow(0)
    val smsCount: StateFlow<Int> = _smsCount


    private val _size = MutableStateFlow(0)
    val size: StateFlow<Int> = _size

    private val _operationWorkManagerUiState = MutableStateFlow<Operation?>(null)
    val operationWorkManagerUiState: StateFlow<Operation?> = _operationWorkManagerUiState

    private val _qualityRatio = MutableStateFlow(100f)
    val qualityRatio: StateFlow<Float> = _qualityRatio


    private val _resizeRatio = MutableStateFlow(1f)
    val resizeRatio: StateFlow<Float> = _resizeRatio

    private var uri: Uri? = null

    fun reset() {
        uri = null
        _resizeRatio.value = 1f
        _qualityRatio.value = 100f
        _size.value = 0
        _smsCount.value = 0
        _processedImage.value = null
        _operationWorkManagerUiState.value = null
        _processingImageUiState.value = false
    }

    fun setUri(context: Context, value: Uri) {
        uri = value
        compressImage(context)
    }

    fun setQuality(context: Context, value: Float) {
        _qualityRatio.value = value
        compressImage(context)
    }

    fun setResizeRatio(context: Context, value: Float) {
        _resizeRatio.value = if(value < 1) 1f else value
        compressImage(context)
    }

    private fun compressImage(context: Context) {
        _processingImageUiState.value = true
        viewModelScope.launch(Dispatchers.Default) {
            val compressFormat: CompressFormat = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                CompressFormat.WEBP_LOSSY else CompressFormat.WEBP

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, uri!!))
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri!!)
            }

            val width = (bitmap.width / _resizeRatio.value).toInt()
            val height = (bitmap.height / _resizeRatio.value).toInt()

            val scaledBitmap = bitmap.scale(width, height, false )
            val byteArrayOutputStream = ByteArrayOutputStream()
            if(scaledBitmap.compress(
                    compressFormat,
                    _qualityRatio.value.toInt(),
                    byteArrayOutputStream)
            ) {
                val image = byteArrayOutputStream.toByteArray().toBitmap()
                _processedImage.value = ProcessedImage(
                    image = image,
                    uri = uri.toString(),
                    rawBytes = byteArrayOutputStream.toByteArray(),
                    format = compressFormat.name
                )
            }
            _smsCount.value = getSmsCount(context)
            _size.value = byteArrayOutputStream.size()
            _processingImageUiState.value = false
        }
    }

    fun startWorkManager(
        context: Context,
        version: Byte,
        textLength: Short,
        address: String,
        formattedPayload: ByteArray,
        notificationFilter: String,
        subscriptionId: Long? = -1,
        logo: Int? = null,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ImageTransmissionNotification.createNotificationChannel(context)
        }

        viewModelScope.launch {
            val constraints: Constraints = Constraints.Builder()
                .build();

            val workManager = WorkManager.getInstance(context)

            fun generateUuidFromLong(input: Long): UUID {
                // Generate a UUID from the long by using the input directly
                // for the most significant bits and setting the least significant bits to 0.
                val mostSigBits = input
                val leastSigBits = 0L // You can modify this if you want to use more of the long

                return UUID(mostSigBits, leastSigBits)
            }

            val sessionId = getItpSession(context = context).toByte()
            val imageLength = _size.value.toShort()

            val remoteListenersListenerWorker = OneTimeWorkRequestBuilder<SmsWorkManager>()
                .setConstraints(constraints)
                .setId(generateUuidFromLong(System.currentTimeMillis()))
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .setInputData(
                    Data.Builder()
//                    .putInt(SmsWorkManager.ITP_SERVICE_ICON, logo)
                        .putByte(SmsWorkManager.ITP_VERSION, version)
                        .putByte(SmsWorkManager.ITP_SESSION_ID, sessionId)
                        .putByteArray(
                            SmsWorkManager.ITP_IMAGE_LENGTH,
                            imageLength.toLittleEndianBytes()
                        )
                        .putByteArray(
                            SmsWorkManager.ITP_TEXT_LENGTH,
                            textLength.toLittleEndianBytes()
                        )
                        .putString(SmsWorkManager.ITP_TRANSMISSION_ADDRESS, address)
                        .putString(SmsWorkManager.ITP_NOTIFICATION_FILTER, notificationFilter)
//                    .putLong(SmsWorkManager.ITP_TRANSMISSION_SUBSCRIPTION_ID, subscriptionId)
                        .build()
                )
                .addTag(SmsWorkManager.IMAGE_TRANSMISSION_WORK_MANAGER_TAG)
                .build();

            cacheImage(context, sessionId, formattedPayload)

            _operationWorkManagerUiState.value = workManager.enqueueUniqueWork(
                "${SmsWorkManager}.IMAGE_TRANSMISSION_WORK_MANAGER_TAG.${
                    System.currentTimeMillis()
                }",
                ExistingWorkPolicy.KEEP,
                remoteListenersListenerWorker
            )

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
    ): ByteArray? {
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

    suspend fun getItpSession(context: Context): Int {
        val sessionId = intPreferencesKey("session_id")
        context.dataStore.edit { session ->
            val currentSession = session[sessionId] ?: 0
            session[sessionId] = if (currentSession >= 255) 0 else currentSession + 1
        }
        return context.dataStore.data.first()[sessionId]!!
    }


    suspend fun getTransmissionIndex(
        context: Context,
        sessionId: Byte,
    ): Int? {
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

            if (segmentNumber.toInt() == 0) {
                metaData += byteArrayOf(numberOfSegments) + imageLength + textLength
            }

            val size = (STANDARD_SEGMENT_SIZE - STANDARD_ENCODED_HEADER_SIZE)
                .coerceAtMost(encodedPayload.size)

            val buffer = Base64.encodeToString(metaData, Base64.NO_WRAP) +
                    String(
                        encodedPayload.take(size).toByteArray(),
                        StandardCharsets.UTF_8
                    )
            if (buffer.length > STANDARD_SEGMENT_SIZE) {
                throw Exception("Buffer size > $STANDARD_SEGMENT_SIZE --> ${buffer.length}")
            }
            encodedPayload = encodedPayload.drop(size).toByteArray()

            segmentNumber = (segmentNumber.toInt() + 1).toByte()

            dividedImage.add(buffer)
        } while (encodedPayload.isNotEmpty())

        return dividedImage
    }

    private fun getSmsCount(
        context: Context,
        smsCountPaddingValue: Int = 0
    ): Int {
        val subId = SmsManager.getDefaultSmsSubscriptionId()
        return (if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context
            .getSystemService(SmsManager::class.java)
            .createForSubscriptionId(subId) else
            SmsManager.getSmsManagerForSubscriptionId(subId))
            .divideMessage(Base64.encodeToString(_processedImage.value?.rawBytes,
                Base64.DEFAULT)).size + smsCountPaddingValue
    }


}