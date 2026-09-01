package com.afkanerd.lib_image_android.ui.data

import android.R.attr.version
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.afkanerd.lib_image_android.ui.services.ImageTransmissionService
import com.afkanerd.lib_image_android.ui.viewModels.ImageViewModel
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.jvm.java

class SmsWorkManager(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams ) {
    override suspend fun doWork(): Result = suspendCancellableCoroutine { cont ->
        val icon = inputData.getInt(ITP_SERVICE_ICON, -1)

        val notificationFilter = inputData.getString(ITP_NOTIFICATION_FILTER).also {
            if(it == null) {
                cont.resume(
                    Result.failure(
                        Data.Builder().putString("reason", "ITP_NOTIFICATION_FILTER null")
                            .build()
                    )
                )
                return@suspendCancellableCoroutine
            }
        }

        val intent = Intent(
            applicationContext,
            ImageTransmissionService::class.java
        ).apply {
            putExtra(ITP_SERVICE_ICON, icon)
            putExtra(ITP_WORK_MANAGER_UUID, id.toString())
            putExtra(ITP_NOTIFICATION_FILTER, notificationFilter)
        }

        registerReceivers(cont)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    private lateinit var completedSendingBroadcast: BroadcastReceiver
    private lateinit var retrySendingBroadcast: BroadcastReceiver
    fun registerReceivers( cont: CancellableContinuation<Result>) {
        val imageViewModel = ImageViewModel()

        val filter = IntentFilter(ITP_SERVICE_COMPLETION)
        completedSendingBroadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                try {
                    applicationContext.unregisterReceiver(this)
                    applicationContext.unregisterReceiver(retrySendingBroadcast)
                    CoroutineScope(Dispatchers.Default).launch {
                        val sessionId = imageViewModel.getCurrentSessionId(applicationContext)
                        imageViewModel.clearPayload(applicationContext, sessionId)
                        cont.resume(Result.success())
                    }
                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        ContextCompat.registerReceiver(
            applicationContext,
            completedSendingBroadcast,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        val filterRetry = IntentFilter(ITP_RETRY_SERVICE)
        retrySendingBroadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                try {
                    applicationContext.unregisterReceiver(this)
                    applicationContext.unregisterReceiver(completedSendingBroadcast)
                    cont.resume(Result.retry())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        ContextCompat.registerReceiver(
            applicationContext,
            retrySendingBroadcast,
            filterRetry,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    companion object {
        const val ITP_WORK_MANAGER_UUID = "ITP_WORK_MANAGER_UUID"
        const val ITP_SERVICE_ICON = "ITP_SERVICE_ICON"
        const val ITP_NOTIFICATION_FILTER = "ITP_NOTIFICATION_FILTER"
        const val IMAGE_TRANSMISSION_WORK_MANAGER_TAG = "IMAGE_TRANSMISSION_WORK_MANAGER_TAG"
        const val ITP_SERVICE_COMPLETION = "ITP_IS_SUCCESS"
        const val ITP_RETRY_SERVICE = "ITP_RETRY_SERVICE"
        const val ITP_TRANSMISSION_REQUEST = "ITP_TRANSMISSION_REQUEST"
    }

}