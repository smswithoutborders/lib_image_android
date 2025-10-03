package com.afkanerd.lib_image_android.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "itp_sessions")

@Serializable
data class ImageTransmissionProtocol(
    val version: Byte,
    val sessionId: Byte,
    val segNumber: Int = -1, // nibble
    val numberSegments: Int = -1, // nibble
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

suspend fun Context.getItpSession() : Int {
    val sessionId = intPreferencesKey("session_id")
    dataStore.edit { session ->
        val currentSession = session[sessionId] ?: 0
        session[sessionId] = if(currentSession >=255) 0 else currentSession + 1
    }
    return dataStore.data.first()[sessionId]!!
}

fun Short.toByteArray(): ByteArray {
    return byteArrayOf(
        (this.toInt() shr 8).toByte(),   // high byte
        (this.toInt() and 0xFF).toByte() // low byte
    )
}
