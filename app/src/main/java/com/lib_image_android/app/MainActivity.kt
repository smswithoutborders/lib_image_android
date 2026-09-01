package com.lib_image_android.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.afkanerd.lib_image_android.ui.BindActivity
import com.afkanerd.lib_image_android.ui.ImageRender
import com.afkanerd.lib_image_android.ui.data.SmsWorkManager
import com.afkanerd.lib_image_android.ui.navigation.ImageRenderNav
import com.afkanerd.lib_image_android.ui.viewModels.ImageViewModel
import com.lib_image_android.app.navigation.ImagePreviewNav
import com.lib_image_android.app.navigation.ImageRenderHomeNav
import com.lib_image_android.app.theme.Lib_image_androidTheme
import com.lib_image_android.app.views.ImageMainView
import com.lib_image_android.app.views.ImagePreview
import kotlin.io.encoding.Base64

class MainActivity : BindActivity() {
    lateinit var navController: NavHostController

    val imageViewModel: ImageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setRemoteExecutionCallback { payload ->
            val intent = Intent("com.afkanerd.message_sent_broadcast").apply {
                putExtra(SmsWorkManager.ITP_TRANSMISSION_REQUEST, true)
            }
            sendBroadcast(intent)
        }

        setContent {
            navController = rememberNavController()

            Lib_image_androidTheme {
                NavHost(
                    navController = navController,
                    startDestination = ImageRenderHomeNav,
                ) {
                    composable<ImageRenderHomeNav>{
                        ImageMainView(
                            navController = navController,
                            imageViewModel = imageViewModel
                        )
                    }

                    composable<ImageRenderNav>{ backEntry ->
                        val imageRenderNav: ImageRenderNav = backEntry.toRoute()
                        ImageRender(
                            imageViewModel = imageViewModel,
                            uri = imageRenderNav.uri?.toUri(),
                            attachmentCounterCallback = { size ->
                                (size + 16) / 160
                            },
                            onApplyCallback = {
                                navController.navigate(ImagePreviewNav) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = true // Set to true to also remove the root screen
                                    }
                                }
                            }
                        )
                    }

                    composable<ImagePreviewNav>{
                        ImagePreview(
                            navController = navController,
                            imageService = imageTransmissionService,
                            imageViewModel = imageViewModel
                        )
                    }
                }
            }
        }
    }
}