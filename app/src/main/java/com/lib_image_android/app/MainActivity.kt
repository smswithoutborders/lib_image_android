package com.lib_image_android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lib_image_android.app.views.ImageMainView
import com.afkanerd.lib_image_android.ui.ImageRender
import com.afkanerd.lib_image_android.ui.viewModels.ImageViewModel
import com.lib_image_android.app.navigation.ImageRenderHomeNav
import com.lib_image_android.app.navigation.ImageRenderNav
import com.lib_image_android.app.theme.Lib_image_androidTheme
import kotlin.getValue

class MainActivity : ComponentActivity() {
    lateinit var navController: NavHostController

    val imageViewModel: ImageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                            navController = navController,
                            imageViewModel = imageViewModel,
                            uri = imageRenderNav.uri.toUri()
                        )
                    }
                }
            }
        }
    }
}