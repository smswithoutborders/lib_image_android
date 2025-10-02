package com.afkanerd.lib_image_android.extensions

fun Short.toByteArray(): ByteArray {
    return byteArrayOf(
        (this.toInt() shr 8).toByte(),   // high byte
        (this.toInt() and 0xFF).toByte() // low byte
    )
}
