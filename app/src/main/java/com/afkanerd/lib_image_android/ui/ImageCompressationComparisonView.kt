package com.afkanerd.lib_image_android.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.ImageButton
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.afkanerd.lib_image_android.R
import com.afkanerd.lib_image_android.ui.components.ImageRender
import com.afkanerd.lib_image_android.ui.navigation.ImageRenderNav
import com.afkanerd.lib_image_android.ui.theme.Lib_image_androidTheme
import com.afkanerd.lib_image_android.ui.viewModels.ImageViewModel


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
        navController.navigate(ImageRenderNav)
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

@Preview(showBackground = true)
@Composable
fun ImageRenderPreview() {
    Lib_image_androidTheme {
        val context = LocalContext.current
//        val bitmap = BitmapFactory.decodeResource(context.resources,
//            R.drawable.)

        ImageCompressionCompareRender(
            rememberNavController(),
            remember{ ImageViewModel() },
        )
    }
}

