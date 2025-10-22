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

    fun compressImage(
        bitmap: Bitmap,
        qualityRatio: Int,
        width: Int,
        height: Int,
        compressFormat: CompressFormat =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                CompressFormat.WEBP_LOSSY else CompressFormat.WEBP
    ): ProcessedImage? {
        val bitmap = resizeImage(
            bitmap,
            qualityRatio,
            width,
            height
        )
        val byteArrayOutputStream = ByteArrayOutputStream()
        if(bitmap.compress( compressFormat, qualityRatio, byteArrayOutputStream)) {
            val image =  byteArrayToBitmap( byteArrayOutputStream.toByteArray())
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
        ratio: Int,
        width: Int,
        height: Int
    ): Bitmap {
        return bitmap.scale(
            width / ratio,
            height / ratio,
            false
        )
    }

}