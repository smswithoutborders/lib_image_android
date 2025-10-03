package com.afkanerd.lib_image_android.ui.viewModels

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import java.io.ByteArrayOutputStream
import java.io.IOException
import androidx.core.graphics.scale
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.afkanerd.lib_image_android.data.SmsWorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences

class ImageViewModel: ViewModel() {

    @Serializable
    data class ProcessedImage(
        @Transient
        var image: Bitmap? = null,
        var size: Long,
        var format: String = "raw",
        var rawBytes: ByteArray? = null,
    )

    var originalBitmap: Bitmap? = null
    private var _processedImage = MutableStateFlow<ProcessedImage?>(null)
    val processedImage: StateFlow<ProcessedImage?> = _processedImage.asStateFlow()

    private var _compressionRatio = MutableStateFlow<Int>(100)
    val compressionRatio = _compressionRatio.asStateFlow()

    private var _resizeRatio = MutableStateFlow<Int>(1)
    val resizeRatio = _resizeRatio.asStateFlow()

    fun reset() {
        _compressionRatio.value = 100
        _resizeRatio.value = 1
        _processedImage.value = null
    }

    fun initialize() {
        reset()
        _processedImage.value = compressImage(originalBitmap!!)
    }

    fun setResizeRatio(value: Int) {
        _resizeRatio.value = value
        _processedImage.value = compressImage(originalBitmap!!)
    }

    fun setCompressionRatio(value: Int) {
        _compressionRatio.value = value
        _processedImage.value = compressImage(originalBitmap!!)
    }

    private fun compressImage(
        bitmap: Bitmap,
        compressFormat: CompressFormat =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                CompressFormat.WEBP_LOSSY else CompressFormat.WEBP
    ): ProcessedImage? {
        val bitmap = resizeImage(bitmap)
        val byteArrayOutputStream = ByteArrayOutputStream()
        if(bitmap.compress(
                compressFormat,
                _compressionRatio.value,
                byteArrayOutputStream)
        ) {
            val image =  byteArrayToBitmap(
                byteArrayOutputStream.toByteArray(),
            )
            return ProcessedImage(
                image,
                byteArrayOutputStream.size().toLong(),
                compressFormat.name,
                rawBytes = byteArrayOutputStream.toByteArray()
            )
        }
        return null
    }

    fun byteArrayToBitmap(
        byteArray: ByteArray,
    ): Bitmap {
        val options = BitmapFactory.Options()
        return BitmapFactory
            .decodeByteArray(
                byteArray,
                0,
                byteArray.size,
                options
            )
    }

    fun resizeImage(
        bitmap: Bitmap,
    ): Bitmap {
        return bitmap.scale(
            originalBitmap!!.width / resizeRatio.value,
            originalBitmap!!.height / resizeRatio.value,
            false
        )
    }

    fun generateUuidFromLong(input: Long): UUID {
        // Generate a UUID from the long by using the input directly
        // for the most significant bits and setting the least significant bits to 0.
        val mostSigBits = input
        val leastSigBits = 0L // You can modify this if you want to use more of the long

        return UUID(mostSigBits, leastSigBits)
    }

    fun startWorkManager(
        context: Context,
        formattedPayload: ByteArray,
        logo: Int,
        version: Byte,
        sessionId: Byte,
        imageLength: ByteArray,
        textLength: ByteArray,
    ): Operation {
        val constraints : Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

        val workManager = WorkManager.getInstance(context)

        val remoteListenersListenerWorker = OneTimeWorkRequestBuilder<SmsWorkManager>()
            .setConstraints(constraints)
            .setId(generateUuidFromLong(System.currentTimeMillis()))
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .setInputData(Data.Builder()
                .putByteArray(SmsWorkManager.ITP_PAYLOAD, formattedPayload)
                .putInt(SmsWorkManager.ITP_SERVICE_ICON, logo)
                .putByte(SmsWorkManager.ITP_VERSION, version)
                .putByte(SmsWorkManager.ITP_SESSION_ID, sessionId)
                .putByteArray(SmsWorkManager.ITP_IMAGE_LENGTH, imageLength)
                .putByteArray(SmsWorkManager.ITP_TEXT_LENGTH, textLength)
                .build()
            )
            .addTag(SmsWorkManager.IMAGE_TRANSMISSION_WORK_MANAGER_TAG)
            .build();

        return workManager.enqueueUniqueWork(
            "$SmsWorkManager.IMAGE_TRANSMISSION_WORK_MANAGER_TAG.${
                System.currentTimeMillis()}",
            ExistingWorkPolicy.KEEP,
            remoteListenersListenerWorker
        )
    }

}