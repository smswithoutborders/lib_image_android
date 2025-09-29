package com.afkanerd.lib_image_android.ui.viewModels

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.os.Build
import androidx.lifecycle.ViewModel
import java.io.ByteArrayOutputStream
import java.io.IOException
import androidx.core.graphics.scale


class ImageViewModel: ViewModel() {
    var processedImage: ProcessedImage? = null

    data class ProcessedImage(
        var image: Bitmap,
        var size: Long,
        var format: String = "raw",
        var rawBytes: ByteArray? = null,
    )

    var compressionRatio: Int = 100
    var height: Int = -1
    var width: Int = -1

    fun compressImage(
        bitmap: Bitmap,
        compressFormat: CompressFormat =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                CompressFormat.WEBP_LOSSY else CompressFormat.WEBP
    ): ProcessedImage? {
        val bitmap = resizeImage(bitmap)
        val byteArrayOutputStream = ByteArrayOutputStream()
        if(bitmap.compress(compressFormat, compressionRatio, byteArrayOutputStream)) {
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
            width,
            height,
            false
        )
    }
}