package com.afkanerd.lib_image_android.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.afkanerd.lib_image_android.R
import com.afkanerd.lib_image_android.ui.components.ImageRender
import com.afkanerd.lib_image_android.ui.navigation.ImageRenderNav
import com.afkanerd.lib_image_android.ui.theme.Lib_image_androidTheme
import com.afkanerd.lib_image_android.ui.viewModels.ImageViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageCompressionCompareRender(
    navController: NavController,
    imageViewModel: ImageViewModel,
    bitmap: Bitmap,
) {
    val originalImage = ImageViewModel.ProcessedImage(
        image = bitmap,
        size = bitmap.allocationByteCount.toLong()
    )

    imageViewModel.height = bitmap.height
    imageViewModel.width = bitmap.width

    val imageViewModelCompression = remember{ ImageViewModel() }
    imageViewModelCompression.height = bitmap.height
    imageViewModelCompression.width = bitmap.width
    imageViewModelCompression.compressionRatio = 0

    val webpCompressed = imageViewModelCompression.compressImage(bitmap,)

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        maxItemsInEachRow = 2
    ) {
        ImageView( originalImage ) {
            imageViewModel.processedImage = originalImage
            navController.navigate(ImageRenderNav)
        }

        webpCompressed?.let {
            ImageView( processedImage = it) {
                imageViewModel.processedImage = webpCompressed
                navController.navigate(ImageRenderNav)
            }
        }
    }
}

@Composable
fun ImageView(
    processedImage: ImageViewModel.ProcessedImage,
    onClickCallback: (() -> Unit)
) {
    Column(
        modifier = Modifier
            .clickable {
                onClickCallback.invoke()
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            bitmap = processedImage.image.asImageBitmap(),
            contentDescription = "Bitmap image",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(150.dp)
        )
        Text("type: ${processedImage.format }")
        Text("size: ${processedImage.size} KB")
    }
}

@Preview(showBackground = true)
@Composable
fun ImageRenderPreview() {
    Lib_image_androidTheme {
        val context = LocalContext.current
        val bitmap = BitmapFactory.decodeResource(context.resources,
            R.drawable.pxl_20231020_104208875_portrait_2)

        ImageCompressionCompareRender(
            rememberNavController(),
            remember{ ImageViewModel() },
            bitmap
        )
    }
}

