package com.lib_image_android.app.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.afkanerd.lib_image_android.ui.viewModels.ImageViewModel
import com.lib_image_android.app.theme.Lib_image_androidTheme

@Composable
fun ImageMainView(
    navController: NavController,
    imageViewModel: ImageViewModel,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {},
        topBar = {}
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            ImageCompressionCompareRender(
                navController,
                imageViewModel
            )
        }
    }
}

@Preview
@Composable
fun ImageMainView_Prevew() {
    Lib_image_androidTheme() {
        ImageMainView(rememberNavController(),
            remember{ ImageViewModel() })
    }
}
