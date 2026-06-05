package com.afkanerd.lib_image_android.ui.viewModels

import android.R.attr.bitmap
import android.R.attr.height
import android.R.attr.version
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
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
            _smsCount.value = 0
            _size.value = byteArrayOutputStream.size()
            _processingImageUiState.value = false
        }
    }

    fun startWorkManager(
        context: Context,
        notificationFilter: String,
        payload: List<String>,
        logo: Int? = null,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ImageTransmissionNotification.createNotificationChannel(context)
        }
        viewModelScope.launch {
            val sessionId = getSessionId(context)
            setPayloadCache(context, sessionId, payload)

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
                        .putString(SmsWorkManager.ITP_NOTIFICATION_FILTER, notificationFilter)
                        .build()
                )
                .addTag(SmsWorkManager.IMAGE_TRANSMISSION_WORK_MANAGER_TAG)
                .build();

            _operationWorkManagerUiState.value = workManager.enqueueUniqueWork(
                "${SmsWorkManager}.IMAGE_TRANSMISSION_WORK_MANAGER_TAG.${
                    System.currentTimeMillis()
                }",
                ExistingWorkPolicy.KEEP,
                remoteListenersListenerWorker
            )

        }
    }

    private suspend fun setPayloadCacheInfo(
        context: Context,
        sessionId: UByte,
        payloadSize: Int,
    ) {
        val key = intPreferencesKey("session_payload_info_$sessionId")
        context.dataStore.edit { session ->
            session[key] = payloadSize
        }
    }

    suspend fun setPayloadCache(
        context: Context,
        sessionId: UByte,
        payload: List<String>,
    ) {
        setPayloadCacheInfo(context, sessionId, payload.size)
        val key = stringSetPreferencesKey("session_payload_$sessionId")
        context.dataStore.edit { session ->
            session[key] = payload.toSet()
        }
    }

    suspend fun getSessionId(context: Context): UByte {
        val sessionId = intPreferencesKey("session_id")
        context.dataStore.edit { session ->
            val currentSession = session[sessionId] ?: 0
            session[sessionId] = if (currentSession >= 255) 0 else currentSession + 1
        }
        return context.dataStore.data.first()[sessionId]!!.toUByte()
    }

    suspend fun getCurrentSessionId(context: Context): UByte {
        val sessionId = intPreferencesKey("session_id")
        return context.dataStore.data.first()[sessionId]!!.toUByte()
    }

    suspend fun getPayloadCacheInfo(context: Context, sessionId: UByte): Int? {
        val key = intPreferencesKey("session_payload_info_$sessionId")
        return context.dataStore.data.first()[key]
    }

    suspend fun getPayloadCache(context: Context, sessionId: UByte): String? {
        val key = stringSetPreferencesKey("session_payload_$sessionId")
        return context.dataStore.data.first()[key]?.elementAtOrNull(0)
    }

    suspend fun popPayloadCache(context: Context, sessionId: UByte) {
        val key = stringSetPreferencesKey("session_payload_$sessionId")
        context.dataStore.edit { session ->
            val payload = session[key]
            if(payload != null)
                session[key] = payload.drop(1).toSet()
        }
    }

    suspend fun incrementIndex(context: Context, sessionId: UByte) {
        val key = intPreferencesKey("session_index_image_$sessionId")
        context.dataStore.edit { session ->
            val currentSession = session[key]
            if(currentSession == null) {
                session[key] = 0
            } else {
                session[key] = getIndex(context, sessionId) + 1
            }
        }
    }

    suspend fun getIndex(context: Context, sessionId: UByte): Int {
        val key = intPreferencesKey("session_index_image_$sessionId")
        return context.dataStore.data.first()[key]!!
    }
}