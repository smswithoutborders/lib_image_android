package com.afkanerd.lib_image_android.ui.extensions

import java.nio.ByteBuffer
import java.nio.ByteOrder

fun Short.toLittleEndianBytes(): ByteArray {
    return ByteBuffer.allocate(2)
        .order(ByteOrder.LITTLE_ENDIAN).putShort(this).array()
}
