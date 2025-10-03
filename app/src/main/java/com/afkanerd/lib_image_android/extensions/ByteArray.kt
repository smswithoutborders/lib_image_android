package com.afkanerd.lib_image_android.extensions

fun ByteArray.toIntLittleEndian(): Int {
    var result = 0
    for (i in this.indices) {
        result = result or (this[i].toInt() shl 8 * i)
    }
    return result
}

fun ByteArray.toShortLittleEndian(
    offset: Int = 0
): Short {
    require(this.size >= offset + 2) { "ByteArray must contain at least two bytes from the specified offset." }

    val byte1 = this[offset].toInt()
    val byte2 = this[offset + 1].toInt()

    return ((byte2 and 0xFF) shl 8 or (byte1 and 0xFF)).toShort()
}
