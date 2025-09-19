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
    )

    fun compressImage(
        bitmap: Bitmap,
        compressionRatio: Int = 0,
        height: Int = -1,
        width: Int = -1,
        compressFormat: CompressFormat =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                CompressFormat.WEBP_LOSSY else CompressFormat.WEBP
    ): ProcessedImage? {
        val bitmap = resizeImage(bitmap, width, height)
        val byteArrayOutputStream = ByteArrayOutputStream()
        if(bitmap.compress(compressFormat, compressionRatio, byteArrayOutputStream)) {
            val image =  byteArrayToBitmap(
                byteArrayOutputStream.toByteArray(),
                height,
                width
            )
            return ProcessedImage(
                image,
                byteArrayOutputStream.size().toLong(),
                compressFormat.name
            )
        }
        return null
    }

    fun byteArrayToBitmap(
        byteArray: ByteArray,
        height: Int,
        width: Int,
    ): Bitmap {
        val options = BitmapFactory.Options()
//        options.inMutable = true
//        if(height > -1 && width > -1) {
//            options.outHeight = height.toInt()
//            options.outWidth = width.toInt()
//        }
        return BitmapFactory
            .decodeByteArray(byteArray, 0, byteArray.size, options)
    }


    fun resizeImage(
        bitmap: Bitmap,
        width: Int,
        height: Int
    ): Bitmap {
        return bitmap.scale(
            width,
            height,
            false
        )
    }

    fun resizeImage(
        bitmap: Bitmap,
        resValue: Int
    ): Bitmap {
        if(resValue < 1) return bitmap
        return bitmap.scale(
            bitmap.width / resValue,
            bitmap.height / resValue,
            false
        )
    }
}