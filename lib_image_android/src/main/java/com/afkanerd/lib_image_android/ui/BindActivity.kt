package com.afkanerd.lib_image_android.ui

import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import com.afkanerd.lib_image_android.ui.services.ImageTransmissionService

open class BindActivity : ComponentActivity() {
    lateinit var imageTransmissionService: ImageTransmissionService
    private var imageServiceBound: Boolean = false

    /** Defines callbacks for service binding, passed to bindService().  */
    private val imageTransmissionServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            val binder = service as ImageTransmissionService.LocalBinder
            imageTransmissionService = binder.getService()
            imageServiceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            imageServiceBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, ImageTransmissionService::class.java).also { intent ->
            bindService(intent, imageTransmissionServiceConnection, BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(imageTransmissionServiceConnection)
        imageServiceBound = false
    }

}