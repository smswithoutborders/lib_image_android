package com.afkanerd.lib_image_android.ui.viewModels

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.io.ByteArrayOutputStream
import java.io.IOException
import androidx.core.graphics.scale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class ImageViewModel: ViewModel() {
    data class ProcessedImage(
        var image: Bitmap,
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