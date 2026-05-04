package com.lib_image_android.app.views

import android.Manifest
import android.R.attr.bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.afkanerd.lib_image_android.R
import com.afkanerd.lib_image_android.ui.viewModels.ImageViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.lib_image_android.app.theme.Lib_image_androidTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ImagePreview(
    navController: NavController,
    imageViewModel: ImageViewModel,
) {
    val inPreviewMode = LocalInspectionMode.current
    val processedImage by imageViewModel.processedImageUiState.collectAsState()
    val context = LocalContext.current

    val bitmap = BitmapFactory
        .decodeResource(context.resources, R.drawable._0241226_124819)

    val defaultPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

    BackHandler {
        navController.popBackStack()
    }

    val notificationFilter = "com.afkanerd.message_sent_broadcast"

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            imageViewModel.startWorkManager(
                context = context,
                version = 0x4,
                address = "",
                subscriptionId = -1,
                textLength = 10,
                formattedPayload = ByteArray(140*60),
                notificationFilter = notificationFilter
            )
        } else {
            // Permission denied, handle accordingly.
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {},
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.edit_image)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() } ) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            "")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !defaultPermission.status.isGranted
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            imageViewModel.startWorkManager(
                                context = context,
                                version = 0x4,
                                address = "",
                                subscriptionId = -1,
                                textLength = 10,
                                formattedPayload = ByteArray(140*60),
                                notificationFilter = notificationFilter
                            )
                        }
                    } ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            stringResource(R.string.apply))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                bitmap = if(inPreviewMode) bitmap.asImageBitmap() else
                    processedImage!!.image!!.asImageBitmap(),
                contentDescription = "Bitmap image",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(250.dp),
            )
        }
    }
}

@Preview
@Composable
fun ImagePreview_Preview() {
    Lib_image_androidTheme {
        ImagePreview(
            rememberNavController(),
            remember{ ImageViewModel() }
        )
    }
}
