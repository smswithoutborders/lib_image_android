package com.afkanerd.lib_image_android.ui.services

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ForegroundInfo
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.afkanerd.lib_image_android.R
import com.afkanerd.lib_image_android.ui.data.SmsWorkManager
import com.afkanerd.lib_image_android.ui.receivers.NotificationActionImpl
import com.afkanerd.lib_image_android.ui.viewModels.ImageViewModel
import com.afkanerd.smswithoutborders_libsmsmms.data.ImageTransmissionProtocol
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.collections.get
import kotlin.jvm.java
import kotlin.uuid.Uuid

class ImageTransmissionService : Service() {
    val imageViewModel = ImageViewModel()
    private lateinit var dividedMessages: MutableList<String>
    private lateinit var messageStateChangedBroadcast: BroadcastReceiver
    private var notificationId: Int = -1

    private lateinit var notificationManager: NotificationManager
    private lateinit var workState: WorkInfo.State

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Implement binder")
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE)
        as NotificationManager
        notificationId = getString(
            R.string.foreground_service_image_transmission_notification_id).toInt()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val address  = intent?.getStringExtra(SmsWorkManager.ITP_TRANSMISSION_ADDRESS)
            ?: return START_NOT_STICKY

        val version  = intent.getByteExtra(SmsWorkManager.ITP_VERSION, -1)
            .also { if(it.toInt() == -1) return START_NOT_STICKY }

        val imageLength  = intent.getByteArrayExtra(SmsWorkManager.ITP_IMAGE_LENGTH).also {
            if(it == null) return START_NOT_STICKY }

        val textLength  = intent.getByteArrayExtra(SmsWorkManager.ITP_TEXT_LENGTH).also {
            if(it == null) return START_NOT_STICKY }

        val sessionId  = intent.getByteExtra(SmsWorkManager.ITP_SESSION_ID, -1)

        val icon = intent.getIntExtra(SmsWorkManager.ITP_SERVICE_ICON, -1)
        val subscriptionId = intent.getLongExtra(
            SmsWorkManager
            .ITP_TRANSMISSION_SUBSCRIPTION_ID, -1)

        val workId = intent.getStringExtra(SmsWorkManager.ITP_WORK_MANAGER_UUID)

        CoroutineScope(Dispatchers.Default).launch {
            val workIdUuid = UUID.fromString(workId)
            val workManager = WorkManager.getInstance(applicationContext)
            workManager
                .getWorkInfoByIdFlow(workIdUuid)
                .collect { workInfo ->
                    workState = workInfo!!.state

                    when(workInfo.state) {
                        WorkInfo.State.ENQUEUED -> {
                            val transmissionIndex = imageViewModel
                                .getTransmissionIndex(applicationContext, sessionId) ?: 0
                            val notification = createForegroundNotification(
                                intent,
                                icon = icon,
                                progress = transmissionIndex,
                                maxProgress = dividedMessages.size,
                                isQueue = true
                            ).notification

                            notificationManager.notify(notificationId, notification)
                        }
                        WorkInfo.State.CANCELLED,
                        WorkInfo.State.SUCCEEDED -> {
//                            if(::messageStateChangedBroadcast.isInitialized) {
//                                try {
//                                    unregisterReceiver(messageStateChangedBroadcast)
//                                } catch(e: IllegalArgumentException) {
//                                    e.printStackTrace()
//                                }
//                            }
                            stopSelf()
                        }
                        else -> {}
                    }
                }
        }

        // This is the continuation
        if(!::messageStateChangedBroadcast.isInitialized) {
            handleBroadcast(
                sessionId = sessionId,
                address = address,
                subscriptionId = subscriptionId,
                icon = icon,
            )
        }

        CoroutineScope(Dispatchers.Default).launch {
            val payload = imageViewModel.getCacheImage(applicationContext, sessionId)
                ?: return@launch

            dividedMessages = imageViewModel.divideImagePayload(
                payload,
                version,
                sessionId,
                imageLength!!,
                textLength!!
            ).apply {
                val header = Base64.decode(this[0].take(12), Base64.DEFAULT)
                header[3] = this.size.toByte()
                this[0] = this[0].replaceRange(0, 12,
                    Base64.encodeToString(header, Base64.DEFAULT))
            }
            val transmissionIndex = imageViewModel
                .getTransmissionIndex(applicationContext, sessionId) ?: 0

            val notification = createForegroundNotification(
                intent,
                icon = icon,
                progress = transmissionIndex,
                maxProgress = dividedMessages.size,
            ).notification

            startForeground(notificationId, notification)

            // This is the beginning
//            TODO("Implement the required callback function here")
        }
        return START_STICKY
    }

    private fun createForegroundNotification() {
        with(NotificationManagerCompat.from(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling ActivityCompat#requestPermissions here
                // to request the missing permissions, and then overriding
                // public fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                //                                        grantResults: IntArray)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.

                return@with
            }
            // notificationId is a unique int for each notification that you must define.
        }
    }

    private fun createForegroundNotification(
        intent: Intent,
        icon: Int,
        maxProgress: Int,
        progress: Int = 0,
        isRetry: Boolean = false,
        isQueue: Boolean = false
    ) : ForegroundInfo {
        val icon = if(icon == -1) R.drawable.ic_launcher_foreground else icon
        val progress = progress + 1
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            isRetry -> getString(R.string.sending_stop)
            isQueue -> getString(R.string.queued_for_sending)
            else -> getString(R.string.sending_images)
        }

        val description = when {
            isRetry -> getString(R.string.waiting_to_send_of, progress,
                maxProgress)
            isQueue -> getString(R.string.will_resume_sending_shortly)
            else -> getString(R.string.of_sent, progress, maxProgress)
        }

        val builder = NotificationCompat.Builder(applicationContext,
            getString(R.string.foreground_service_image_transmission_channel_id))
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(icon)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setContentIntent(pendingIntent)
            .setProgress(maxProgress, progress, isQueue).apply {
                if(!isQueue) {
                    getActions(applicationContext, isRetry).forEach {
                        this.addAction(it)
                    }
                }
            }

        val notification = builder.build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                notificationId,
                notification
            )
        }
    }

    private fun getActions(
        context: Context,
        isRetry: Boolean
    ) : List<NotificationCompat.Action> {
        val stopLabel = getString(R.string.stop)
        val pauseLabel = getString(R.string.pause)
        val retryLabel = getString(R.string.retry)

        val notifications = mutableListOf<NotificationCompat.Action>()

        if(isRetry) {
            val retryPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                context,
                2, // Or a unique request code
                Intent(
                    this,
                    NotificationActionImpl::class.java
                ).apply {
                    action = NotificationActionImpl.NOTIFICATION_RETRY_ACTION_INTENT_ACTION
                },
                PendingIntent.FLAG_MUTABLE // Flags for the PendingIntent
            )
            notifications.add(
                NotificationCompat.Action.Builder(
                    null, // Icon for the reply button
                    retryLabel, // Text for the reply button
                    retryPendingIntent
                ).build(),
            )
        } else {
            val stopPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                context,
                0, // Or a unique request code
                Intent(
                    this,
                    NotificationActionImpl::class.java
                ).apply {
                    action = NotificationActionImpl.NOTIFICATION_STOP_ACTION_INTENT_ACTION
                },
                PendingIntent.FLAG_MUTABLE // Flags for the PendingIntent
            )

            val pausePendingIntent: PendingIntent = PendingIntent.getBroadcast(
                context,
                1, // Or a unique request code
                Intent(
                    this,
                    NotificationActionImpl::class.java
                ).apply {
                    action = NotificationActionImpl.NOTIFICATION_PAUSE_ACTION_INTENT_ACTION
                },
                PendingIntent.FLAG_MUTABLE // Flags for the PendingIntent
            )
            notifications.add(
                NotificationCompat.Action.Builder(
                    null, // Icon for the reply button
                    stopLabel, // Text for the reply button
                    stopPendingIntent
                ).build()
            )

            notifications.add(
                NotificationCompat.Action.Builder(
                    null, // Icon for the reply button
                    pauseLabel, // Text for the reply button
                    pausePendingIntent
                ).build(),
            )
        }
        return notifications
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        super.onTimeout(startId, fgsType)
        stopSelf()
    }

    /**
     * This is an indicator for iterating through the transmissions
     */
    private fun handleBroadcast(
        sessionId: Byte,
        icon: Int,
        address: String,
        subscriptionId: Long,
    ) {
        val intentFilter = IntentFilter()
        intentFilter.addAction("") // TODO("This used to listen for incoming sms")
        messageStateChangedBroadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != null &&
                    intentFilter.hasAction(intent.action) &&
                    intent.hasExtra(SmsWorkManager.ITP_TRANSMISSION_REQUEST)
                ) {
                    if (resultCode == Activity.RESULT_OK) {
                        CoroutineScope(Dispatchers.Default).launch {
                            var transmissionIndex = imageViewModel
                                .getTransmissionIndex(applicationContext, sessionId) ?: 0

                            transmissionIndex += 1
                            if(transmissionIndex >= dividedMessages.size) {
                                sendBroadcast(Intent(SmsWorkManager.ITP_SERVICE_COMPLETION))
                                return@launch
                            }

                            imageViewModel.storeTransmissionSessionIndex(
                                context = applicationContext,
                                sessionId = sessionId,
                                index = transmissionIndex
                            )
                            Thread.sleep(5000)

                            when(workState) {
                                WorkInfo.State.ENQUEUED,
                                WorkInfo.State.RUNNING -> {
                                    println("Sending new message: $transmissionIndex")

                                    TODO("Implement the required callback function here")

                                    val notification = createForegroundNotification(
                                        intent,
                                        icon = icon,
                                        progress = transmissionIndex,
                                        maxProgress = dividedMessages.size,
                                    ).notification

                                    notificationManager.notify(notificationId, notification)
                                }
                                else -> {}
                            }
                        }
                    } else {
                        CoroutineScope(Dispatchers.Default).launch {
                            val transmissionIndex = imageViewModel
                                .getTransmissionIndex(applicationContext, sessionId) ?: return@launch

                            val notification = createForegroundNotification(
                                intent,
                                icon = icon,
                                progress = transmissionIndex,
                                maxProgress = dividedMessages.size,
                                isRetry = true
                            ).notification

                            notificationManager.notify(notificationId, notification)
                        }
                    }
                }
            }
        }

        ContextCompat.registerReceiver(
            this,
            messageStateChangedBroadcast,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onDestroy() {
        if(::messageStateChangedBroadcast.isInitialized) {
            unregisterReceiver(messageStateChangedBroadcast)
        }
        super.onDestroy()
    }


}