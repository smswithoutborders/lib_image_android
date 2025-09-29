package com.afkanerd.lib_image_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.afkanerd.lib_image_android.ui.navigation.ImageRenderHomeNav
import com.afkanerd.lib_image_android.ui.theme.Lib_image_androidTheme
import com.afkanerd.lib_image_android.ui.viewModels.ImageViewModel
import androidx.navigation.compose.rememberNavController
import com.afkanerd.lib_image_android.ui.ImageMainView
import com.afkanerd.lib_image_android.ui.components.ImageRender
import com.afkanerd.lib_image_android.ui.navigation.ImageRenderNav

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

                    composable<ImageRenderNav>{
                        ImageRender(
                            navController = navController,
                            imageViewModel = imageViewModel
                        )
                    }
                }
            }
        }
    }
}

