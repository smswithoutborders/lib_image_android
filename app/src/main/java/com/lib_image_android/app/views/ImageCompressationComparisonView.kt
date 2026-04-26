package com.lib_image_android.app.views

import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.afkanerd.lib_image_android.ui.viewModels.ImageViewModel
import com.lib_image_android.app.navigation.ImageRenderNav


@Composable
fun mmsImagePicker(
    callback: (Uri) -> Unit
): ManagedActivityResultLauncher<Array<String>, Uri?> {
    // Registers a photo picker activity launcher in single-select mode.
    return rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            callback(uri)
        } else {
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageCompressionCompareRender(
    navController: NavController,
    imageViewModel: ImageViewModel,
) {
    val context = LocalContext.current
    val imagePicker = mmsImagePicker { uri ->
        val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flag)
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        navController.navigate(ImageRenderNav(
            uri = uri.toString(),
        ))
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = {
            imagePicker.launch(arrayOf("image/png", "image/jpg", "image/jpeg"))
        }, Modifier.size(150.dp)) {
            Icon(
                Icons.Default.AddPhotoAlternate,
                "",
                Modifier.fillMaxSize()
            )
        }

    }
}