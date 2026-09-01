package com.afkanerd.lib_image_android.ui.viewModels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.afkanerd.lib_image_android.ui.data.ImageTransmissionNotification
import com.afkanerd.lib_image_android.ui.data.SmsWorkManager
import com.afkanerd.lib_image_android.ui.extensions.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID
import java.util.concurrent.TimeUnit


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "itp_sessions")
class ImageViewModel: ViewModel() {
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

    private val _lowerBoundRange = MutableStateFlow(0f)
    val lowerBoundRange: StateFlow<Float> = _lowerBoundRange

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

    private var smsCounterCallback: ((payloadSize: Int) -> Int)? = null

    fun attachmentCounterCallback(smsCounterCallback: (payloadSize: Int) -> Int) {
        this.smsCounterCallback = smsCounterCallback
    }

    fun setUri(context: Context, value: Uri) {
        viewModelScope.launch {
            uri = value
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, uri!!))
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri!!)
            }

            val width = (bitmap.width / _resizeRatio.value).toInt()
            val height = (bitmap.height / _resizeRatio.value).toInt()
            _lowerBoundRange.value = (if(width > height) width else height) / 16f // why later
            compressImage(context)
        }
    }

    fun setQuality(context: Context, value: Float) {
        _qualityRatio.value = value
        compressImage(context)
    }

    fun setResizeRatio(context: Context, value: Float) {
        _resizeRatio.value = if(value < 1) 1f else value
        compressImage(context)
    }

    private fun compressImage( context: Context, ) {
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

            // Does not to send the contents, just the size in emptied bytes
            val payloadSize = _processedImage.value?.rawBytes?.size ?: 0
            smsCounterCallback?.let {
                _smsCount.value = smsCounterCallback!!.invoke(payloadSize)
            }
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
                session[key] = 1
            } else {
                session[key] = getIndex(context, sessionId) + 1
            }
        }
    }

    suspend fun getIndex(context: Context, sessionId: UByte): Int {
        val key = intPreferencesKey("session_index_image_$sessionId")
        return context.dataStore.data.first()[key] ?: 0
    }

    private suspend fun removeIndex(context: Context, sessionId: UByte) {
        val key = intPreferencesKey("session_index_image_$sessionId")
        context.dataStore.edit { session ->
            session.remove(key)
        }
    }

    private suspend fun removePayloadCacheInfo(
        context: Context,
        sessionId: UByte,
    ) {
        val key = intPreferencesKey("session_payload_info_$sessionId")
        context.dataStore.edit { session ->
            session.remove(key)
        }
    }

    private suspend fun removePayloadCache(
        context: Context,
        sessionId: UByte,
    ) {
        removePayloadCacheInfo(context, sessionId)
        val key = stringSetPreferencesKey("session_payload_$sessionId")
        context.dataStore.edit { session ->
            session.remove(key)
        }
    }

    suspend fun clearPayload(context: Context, sessionId: UByte) {
        removePayloadCache(context, sessionId)
        removeIndex(context, sessionId)
        removePayloadCacheInfo(context, sessionId)
    }
}