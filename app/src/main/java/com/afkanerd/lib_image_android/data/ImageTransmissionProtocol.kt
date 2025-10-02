package com.afkanerd.lib_image_android.data

import android.content.Context
import android.util.Base64
import kotlinx.serialization.Serializable

@Serializable
data class ImageTransmissionProtocol(
    val version: Byte = 0x4,
    val sessionId: Byte,
    val segNumber: Int, // nibble
    val numberSegments: Int, // nibble
    val imageLength: Short, // only in first segment
    val textLength: Short, // only in first segment
    val image: ByteArray,
    val text: ByteArray // follows std platform formatting
) {
    fun getSegNumberNumberSegment(segmentNumber: Int): Byte {
        val hi = (segmentNumber and 0x0F) shl 4
        val low = (numberSegments and 0x0F)
        return (hi or low).toByte()
    }
}

fun Short.toByteArray(): ByteArray {
    return byteArrayOf(
        (this.toInt() shr 8).toByte(),   // high byte
        (this.toInt() and 0xFF).toByte() // low byte
    )
}
