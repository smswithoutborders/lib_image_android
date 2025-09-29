package com.afkanerd.lib_image_android.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.telephony.SmsManager
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.afkanerd.lib_image_android.R
import com.afkanerd.lib_image_android.ui.theme.Lib_image_androidTheme
import com.afkanerd.lib_image_android.ui.viewModels.ImageViewModel
import org.intellij.lang.annotations.JdkConstants

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImageRender(
    navController: NavController,
    imageViewModel: ImageViewModel
) {
    val context = LocalContext.current

    val processedImage by imageViewModel.processedImage.collectAsState()
    val compressionRatio by imageViewModel.compressionRatio.collectAsState()
    val resizeRatio by imageViewModel.resizeRatio.collectAsState()

    fun getSmsCount(): Int {
        return (if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context
            .getSystemService(SmsManager::class.java)
            .createForSubscriptionId(SmsManager.getDefaultSmsSubscriptionId()) else
            SmsManager.getSmsManagerForSubscriptionId(SmsManager
                .getDefaultSmsSubscriptionId()))
            .divideMessage(Base64
                .encodeToString(processedImage!!.rawBytes,
                    Base64.DEFAULT)).size
    }
//    var smsCount by remember{ mutableIntStateOf(getSmsCount()) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
                Row(
                    Modifier.padding(16.dp)
                ) {
                    FilledTonalButton(onClick = {

                    }, modifier = Modifier.weight(1f),) {
                        Text(
                            stringResource(R.string.reset),
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(onClick = {

                    }, modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.apply),
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.edit_image)) },
                navigationIcon = {
                    IconButton(onClick = {
                        TODO("Implement back")
                    }) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            "")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            Column(
                Modifier.padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        bitmap = processedImage?.image?.asImageBitmap() ?:
                        imageViewModel.originalBitmap!!.asImageBitmap(),
                        contentDescription = "Bitmap image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(250.dp),
                    )
                }

                Spacer(Modifier.padding(16.dp))

                Text(
                    stringResource(R.string.compression),
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(Modifier.padding(8.dp))

                Card(
                    colors = CardDefaults
                        .cardColors(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                stringResource(R.string.quality),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${compressionRatio}%",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(Modifier.padding(4.dp))

                        SliderImplementation {
                            imageViewModel.setCompressionRatio(100 - it.toInt())
                        }
                    }
                }

                Spacer(Modifier.padding(16.dp))

                Text(
                    stringResource(R.string.resize),
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(Modifier.padding(8.dp))

                Card( colors = CardDefaults
                    .cardColors(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                stringResource(R.string.size),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${resizeRatio}%",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(Modifier.padding(4.dp))

                        SliderImplementation {
                        }
                        Row {
                            Text(
                                stringResource(R.string._0),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                stringResource(R.string._100),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.padding(4.dp))
                        Text(
                            stringResource(R.string.aspect_ratio_is_locked_width_and_height_will_be_adjusted_proportionally),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
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

        ImageRender(
            rememberNavController(),
            remember{ ImageViewModel() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun SliderImplementation(
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
            thumb = {
                Box(
                    modifier = Modifier
                        .padding(0.dp)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            },
            steps = 9,
            valueRange = 0f..100f
        )
    }

}
