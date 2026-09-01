package com.afkanerd.lib_image_android.ui.services

import android.R.attr.description
import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat.stopForeground
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.work.ForegroundInfo
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.afkanerd.lib_image_android.R
import com.afkanerd.lib_image_android.ui.data.SmsWorkManager
import com.afkanerd.lib_image_android.ui.receivers.NotificationActionImpl
import com.afkanerd.lib_image_android.ui.viewModels.ImageViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.jvm.java

class ImageTransmissionService : Service() {
    lateinit var workManager: WorkManager
    val imageViewModel = ImageViewModel()
    var messageStateChangedBroadcast: BroadcastReceiver? = null
    private var notificationId: Int = -1
    private var icon: Int = -1

    private lateinit var notificationManager: NotificationManager
    private lateinit var workState: WorkInfo.State

    private var runtimeExecution: ((String) -> Unit)? = null

    // Binder given to clients.
    private val binder = LocalBinder()

   var payloadSize: Int = 0

    fun setRemoteExecutionCallback(callback: (String) -> Unit) {
        runtimeExecution = callback
    }

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods.
        fun getService(): ImageTransmissionService = this@ImageTransmissionService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }


    override fun onCreate() {
        super.onCreate()
        workManager  = WorkManager.getInstance(applicationContext)
        notificationManager = getSystemService(NOTIFICATION_SERVICE)
        as NotificationManager
        notificationId = getString(
            R.string.foreground_service_image_transmission_notification_id).toInt()

    }

    private suspend fun workflowWatch(
        intent: Intent,
        workId: String?,
    ) {
        workManager.getWorkInfoByIdFlow(UUID.fromString(workId))
            .collect { workInfo ->
                workState = workInfo!!.state
                val sessionId = imageViewModel.getCurrentSessionId(applicationContext)

                when(workInfo.state) {
                    WorkInfo.State.ENQUEUED -> {
                        val notification = createForegroundNotification(
                            intent,
                            icon = icon,
                            progress = imageViewModel.getIndex(applicationContext, sessionId),
                            maxProgress = payloadSize,
                            isQueue = true,
                            sessionId = sessionId
                        ).notification

                        notificationManager.notify(notificationId, notification)
                    }
                    WorkInfo.State.CANCELLED,
                    WorkInfo.State.SUCCEEDED -> {
                        stopSelf()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                    else -> {}
                }
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        icon = intent.getIntExtra(SmsWorkManager.ITP_SERVICE_ICON, -1)

        val notification = createStartingNotification(
            intent,
            icon = icon,
        ).notification

        startForeground(notificationId, notification)

        val notificationFilters = intent
            .getStringExtra(SmsWorkManager.ITP_NOTIFICATION_FILTER) ?:
            throw Exception("Failed to find ${SmsWorkManager.ITP_NOTIFICATION_FILTER}")

        val workId = intent.getStringExtra(SmsWorkManager.ITP_WORK_MANAGER_UUID)

        // This is the continuation
        if(messageStateChangedBroadcast == null) {
            handleBroadcast(notificationFilters = notificationFilters)
        }

        CoroutineScope(Dispatchers.Default).launch {
            workflowWatch(
                intent = intent,
                workId = workId,
            )
        }

        CoroutineScope(Dispatchers.Default).launch {
            val sessionId = imageViewModel.getCurrentSessionId(applicationContext)
            val info = imageViewModel.getPayloadCacheInfo(applicationContext, sessionId)
            if(info == null) {
                stopSelf()
                return@launch
            }
            payloadSize = info
            executePayload(intent, sessionId, false)
        }

        return START_STICKY
    }

    private fun createForegroundNotification(
        intent: Intent,
        icon: Int,
        maxProgress: Int,
        sessionId: UByte,
        progress: Int = 0,
        isRetry: Boolean = false,
        isQueue: Boolean = false,
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
            else -> getString(R.string.sending_content, sessionId.toInt())
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

    private fun createStartingNotification(
        intent: Intent,
        icon: Int,
    ): ForegroundInfo {
        val icon = if(icon == -1) R.drawable.ic_launcher_foreground else icon
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val title = getString(R.string.preparing_for_transmission)

        val description = getString(R.string.your_attachment_would_begin_sending_soon)

        val builder = NotificationCompat.Builder(
            applicationContext,
            getString(R.string.foreground_service_image_transmission_channel_id))
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)

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
    private fun handleBroadcast(notificationFilters: String,) {
        val intentFilter = IntentFilter()
        intentFilter.addAction(notificationFilters)
        messageStateChangedBroadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != null &&
                    intentFilter.hasAction(intent.action) &&
                    intent.hasExtra(SmsWorkManager.ITP_TRANSMISSION_REQUEST)
                ) {
                    CoroutineScope(Dispatchers.Default).launch {
                        val sessionId = imageViewModel.getCurrentSessionId(applicationContext)

                        when(resultCode) {
                            Activity.RESULT_OK -> {
                                imageViewModel.popPayloadCache(applicationContext, sessionId)
                                imageViewModel.incrementIndex(applicationContext, sessionId)

                                Thread.sleep((5..10).random() * 1000L)
                                when (workState) {
                                    WorkInfo.State.RUNNING ->
                                        executePayload(intent, sessionId, false)
                                    else -> {}
                                }
                            } else -> executePayload(intent, sessionId, true)
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

    private suspend fun executePayload(
        intent: Intent,
        sessionId: UByte,
        isRetry: Boolean,
    ) {
        val payload = imageViewModel.getPayloadCache(applicationContext, sessionId)
        if (payload.isNullOrEmpty()) {
            sendBroadcast(Intent(SmsWorkManager.ITP_SERVICE_COMPLETION))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        runtimeExecution?.invoke(payload)

        val notification = createForegroundNotification(
            intent,
            icon = icon,
            progress = imageViewModel.getIndex(applicationContext, sessionId),
            maxProgress = payloadSize,
            sessionId = sessionId,
            isRetry = isRetry
        ).notification

        notificationManager.notify(notificationId, notification)
    }

    override fun onDestroy() {
        if(messageStateChangedBroadcast != null) {
            try {
                unregisterReceiver(messageStateChangedBroadcast)
                messageStateChangedBroadcast = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        super.onDestroy()
    }
}