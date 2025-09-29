package com.afkanerd.lib_image_android.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.telephony.SmsManager
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.afkanerd.lib_image_android.R
import com.afkanerd.lib_image_android.ui.theme.Lib_image_androidTheme
import com.afkanerd.lib_image_android.ui.viewModels.ImageViewModel
import org.intellij.lang.annotations.JdkConstants

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageRender(
    imageViewModel: ImageViewModel,
) {
    val context = LocalContext.current
    var processedImage by remember{ mutableStateOf( imageViewModel.processedImage)}
    var image by remember{ mutableStateOf(processedImage!!.image) }
    var size by remember{ mutableLongStateOf(processedImage!!.size / 1000L) }
    var height by remember{ mutableIntStateOf(processedImage!!.image.height) }
    var width by remember{ mutableIntStateOf(processedImage!!.image.width) }
    fun getSmsCount(): Int {
        if(processedImage!!.rawBytes == null)
            return 0

        return (if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context
            .getSystemService(SmsManager::class.java)
            .createForSubscriptionId(SmsManager.getDefaultSmsSubscriptionId()) else
            SmsManager.getSmsManagerForSubscriptionId(SmsManager
                .getDefaultSmsSubscriptionId()))
            .divideMessage(Base64
                .encodeToString(processedImage!!.rawBytes,
                    Base64.DEFAULT)).size
    }
    var smsCount by remember{ mutableIntStateOf(getSmsCount()) }

    LaunchedEffect(processedImage,) {
        image = processedImage!!.image
        size = processedImage!!.size / 1000L
        height = image.height
        width = image.width
        smsCount = getSmsCount()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {},
        topBar = {}
    ) { innerPadding ->
        Box(Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            ) {
                SliderDimensions {
                    if(it < 1) {
                        imageViewModel.height= imageViewModel.processedImage!!.image.height
                        imageViewModel.width= imageViewModel.processedImage!!.image.width
                    }
                    else {
                        imageViewModel.height= (imageViewModel.processedImage!!.image.height / it)
                            .toInt()
                        imageViewModel.width= (imageViewModel.processedImage!!.image.width / it)
                            .toInt()
                    }
                    imageViewModel.compressImage(
                        imageViewModel.processedImage!!.image,
                    ).let { image ->
                        processedImage = image
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = "Bitmap image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(500.dp),
                    )

                    SliderCompression {
                        imageViewModel.compressionRatio = (100 - it.toInt())
                        imageViewModel.compressImage(
                            imageViewModel.processedImage!!.image,
                        ).let { image ->
                            processedImage = image
                        }
                    }

                    Column{
                        Text("type: ${processedImage!!.format}")
                        Text("size: $size KB")
                        Text("compression: ${imageViewModel.compressionRatio}")
                        Text("sms count: $smsCount")
                        Text("height: $height")
                        Text("width: $width")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ImageRenderPreview() {
    Lib_image_androidTheme {
        val context = LocalContext.current
        val bitmap = BitmapFactory.decodeResource(context.resources,
            R.drawable.pxl_20231020_104208875_portrait_2)
        val processedImage = ImageViewModel.ProcessedImage(
            image = bitmap,
            size = bitmap.allocationByteCount.toLong()
        )

        ImageRender(remember { ImageViewModel().apply {
            this.processedImage = processedImage
        } })
    }
}

@Preview
@Composable
fun SliderCompression(
    sliderChangedCallback: (Float) -> Unit = {},
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    Column {
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {
                sliderChangedCallback(sliderPosition)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            steps = 9,
            valueRange = 0f..100f
        )
    }

}

@Preview
@Composable
fun SliderDimensions(
    sliderChangedCallback: (Float) -> Unit = {},
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    Column {
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {
                sliderChangedCallback(sliderPosition)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            steps = 9,
            valueRange = 0f..100f,
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = 90f
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = constraints.minHeight,
                            maxWidth = (constraints.maxHeight / 1.5).toInt(),
                            minHeight = constraints.minWidth,
                            maxHeight = constraints.maxHeight,
                        )
                    )
                    layout(placeable.height, placeable.width) {
                        placeable.place(0, -placeable.height)
                    }
                }
        )
//        Text(text = sliderPosition.toString())
    }
}
