package com.afkanerd.lib_image_android.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.afkanerd.lib_image_android.R
import com.afkanerd.lib_image_android.ui.viewModels.ImageViewModel

@Composable
fun ImageMainView(
    navController: NavController,
    imageViewModel: ImageViewModel,
) {
    val context = LocalContext.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {},
        topBar = {}
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            val bitmap = BitmapFactory.decodeResource(context.resources,
                R.drawable.pxl_20231020_104208875_portrait_2)
            ImageCompressionCompareRender(navController, imageViewModel, bitmap)
        }
    }
}