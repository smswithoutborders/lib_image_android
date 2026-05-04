package com.afkanerd.smswithoutborders_libsmsmms.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkRequest
import coil3.network.NetworkRequest
import com.afkanerd.lib_image_android.ui.data.SmsWorkManager
import com.afkanerd.lib_image_android.ui.extensions.toLittleEndianBytes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.TimeUnit

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "itp_sessions")

@Serializable
data class ImageTransmissionProtocol(
    val version: Byte,
    val sessionId: Byte,
    val segNumber: Byte,
    val numberSegments: Byte,
    val imageLength: Byte, // only in first segment
    val textLength: Byte, // only in first segment
    val image: ByteArray,
    val text: ByteArray // follows std platform formatting
) {
}
