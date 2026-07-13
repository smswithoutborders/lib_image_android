package com.afkanerd.lib_image_android.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.afkanerd.lib_image_android.R
import com.afkanerd.lib_image_android.ui.viewModels.ImageViewModel


/**
 * imageViewModel: Used to manage the image states.
 * uri: The Uri for the image to be edited.
 * maxNumberSms: The maximum amount of SMS count that should be achieved while editing the images.
 * smsCountPaddingValue: This values is added to the amount of SMS count (in cases of encryption of overhead you can use this value to add to the number of SMS counts).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImageRender(
    navController: NavController,
    imageViewModel: ImageViewModel,
    uri: Uri? = null,
    attachmentCounterCallback: (payloadSize: Int) -> Int,
    backActionCallback: () -> Unit = { navController.popBackStack() },
    onApplyCallback: () -> Unit,
) {
    val context = LocalContext.current
    val inPreviewMode = LocalInspectionMode.current
    if(uri != null) {
        imageViewModel.setUri(context, uri)
    }

    LaunchedEffect(attachmentCounterCallback) {
        imageViewModel.attachmentCounterCallback(attachmentCounterCallback)
    }

    val processing by imageViewModel.processingImageUiState.collectAsState()
    val processedImage by imageViewModel.processedImage.collectAsState()

    val smsCount by imageViewModel.smsCount.collectAsState()
    val size by imageViewModel.size.collectAsState()
    val lowerBoundRange by imageViewModel.lowerBoundRange.collectAsState()

    var showQualitySlider by remember{ mutableStateOf(false ) }
    var showResizeSlider by remember{ mutableStateOf(false ) }

    val qualityRatio by imageViewModel.qualityRatio.collectAsState()
    val resizeRatio by imageViewModel.resizeRatio.collectAsState()

    BackHandler {
        navController.popBackStack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.edit_image)) },
                navigationIcon = {
                    IconButton(onClick = backActionCallback ) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack,
                            "")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onApplyCallback()
                    } ) {
                        Icon(Icons.Default.Check,
                            stringResource(R.string.apply))
                    }
                }
            )
        },
    ) { innerPadding ->
        Box(Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            if(processing || inPreviewMode) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
                    .animateContentSize(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    processedImage?.let {
                        Image(
                            bitmap = processedImage!!.image!!.asImageBitmap(),
                            contentDescription = "Bitmap image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(250.dp),
                        )
                    }
                }

                Spacer(Modifier.padding(4.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    FlowRow(maxItemsInEachRow = 4) {
                        ImageInfo(
                            stringResource(R.string.sms_est),
                            smsCount.toString()
                        )

                        ImageInfo(
                            stringResource(R.string.width),
                            (processedImage?.image?.width).toString(),
                        )

                        ImageInfo(
                            stringResource(R.string.height),
                            (processedImage?.image?.height).toString(),
                        )

                        ImageInfo(
                            stringResource(R.string.size),
                            stringResource(
                                if(size < 1000) R.string.bytes else R.string.kb,
                                if(size < 1000) size else size / 1000
                            )
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.compression),
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {
                            showQualitySlider = !showQualitySlider
                        }) {
                            Icon(
                                if(showQualitySlider) Icons.Default.ArrowDropUp else
                                    Icons.Default.ArrowDropDown,
                                "Drop down"
                            )
                        }
                    }

                    if(showQualitySlider || inPreviewMode) {
                        Spacer(Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults
                                .cardColors(MaterialTheme.colorScheme.surfaceContainer),
                        ) {
                            SliderImplementation(
                                stringResource(R.string.quality),
                                sliderPosition = qualityRatio,
                            ) {
                                imageViewModel.setQuality(context, it)
                            }
                        }
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.resize),
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {
                            showResizeSlider = !showResizeSlider
                        }) {
                            Icon(
                                if(showResizeSlider) Icons.Default.ArrowDropUp else
                                    Icons.Default.ArrowDropDown,
                                "Drop down"
                            )
                        }
                    }

                    if(showResizeSlider || inPreviewMode) {
                        Spacer(Modifier.padding(8.dp))

                        Card(
                            colors = CardDefaults
                                .cardColors(MaterialTheme.colorScheme.surfaceContainer),
                        ) {
                            Column {
                                SliderImplementation(
                                    stringResource(R.string.size),
                                    valueRangeUpper = lowerBoundRange,
                                    sliderPosition = resizeRatio
                                ) {
                                    imageViewModel.setResizeRatio(context,it)
                                }
                                Spacer(Modifier.padding(4.dp))
                                Text(
                                    stringResource(R.string.aspect_ratio_is_locked_width_and_height_will_be_adjusted_proportionally),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ImageInfo(
    title: String = "Width",
    value: String = "1344px",
) {
    Column( Modifier.padding(8.dp) ) {
        Card {
            Column(
                Modifier.padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 10.sp
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun SliderImplementation(
    label: String = "",
    valueRangeUpper: Float = 100f,
    sliderPosition: Float = 100f,
    sliderFinishedChangedCallback: (Float) -> Unit = {},
) {
    var sliderPosition by remember{ mutableFloatStateOf(sliderPosition) }
    var textValue by remember{ mutableStateOf(sliderPosition.toString()) }

    Column(Modifier.padding(16.dp)) {
        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it

                textValue = it.toString()
            },
            onValueChangeFinished = {
                sliderFinishedChangedCallback(sliderPosition)
            },
//            thumb = {
//                Box(
//                    modifier = Modifier
//                        .padding(0.dp)
//                        .size(24.dp)
//                        .background(MaterialTheme.colorScheme.primary, CircleShape),
//                )
//            },
            steps = 10,
            valueRange = 0f..valueRangeUpper
        )

        Spacer(Modifier.padding(4.dp))

        OutlinedTextField(
            value = textValue,
            singleLine = true,
            enabled = false,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    textValue.toFloatOrNull()?.let { pValue ->
                        sliderPosition = pValue
                        sliderFinishedChangedCallback(sliderPosition)
                    }
                }
            ),
            onValueChange = {
                textValue = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
        )
    }
}

@Preview
@Composable
fun ImageMainView() {
    val context = LocalContext.current
    val uri = "android.resource://${context.packageName}/${R.drawable._0241226_124819}".toUri()
    ImageRender(
        navController = rememberNavController(),
        imageViewModel = remember { ImageViewModel() },
        uri = uri,
        attachmentCounterCallback = { 0 },
        onApplyCallback = {},
    )
}
