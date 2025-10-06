package com.afkanerd.lib_image_android.ui.viewModels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.os.Build
import androidx.lifecycle.ViewModel
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID
import java.util.concurrent.TimeUnit

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

}