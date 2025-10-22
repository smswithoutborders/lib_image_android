package com.afkanerd.lib_image_android.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.telephony.SmsManager
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.afkanerd.lib_image_android.R
import com.afkanerd.lib_image_android.ui.ImageMainView
import com.afkanerd.lib_image_android.ui.theme.Lib_image_androidTheme
import com.afkanerd.lib_image_android.ui.viewModels.ImageViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.intellij.lang.annotations.JdkConstants

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImageRender(
    navController: NavController,
    bitmap: Bitmap,
    imageViewModel: ImageViewModel,
    maxNumberSms: Int = 64,
    smsCountPaddingValue: Int = 0,
    backActionCallback: () -> Unit = { navController.popBackStack() },
    onFinishCallback: () -> Unit,
) {
    val context = LocalContext.current
    val inPreviewMode = LocalInspectionMode.current

    val processedImage by remember{ mutableStateOf<ImageViewModel.ProcessedImage?>(null) }

    var smsCount by remember{ mutableIntStateOf(0) }
    var size by remember{ mutableIntStateOf(0) }

    var showQualitySlider by remember{ mutableStateOf(false ) }
    var showResizeSlider by remember{ mutableStateOf(false ) }

    var qualityRatio by remember{ mutableFloatStateOf(100f ) }
    var resizeRatio by remember{ mutableFloatStateOf(0f ) }

    LaunchedEffect(qualityRatio, resizeRatio) {
        imageViewModel.compressImage(
            bitmap,
            qualityRatio.toInt(),
            (bitmap.width / resizeRatio).toInt(),
            (bitmap.height / resizeRatio).toInt(),
        )
    }

    fun getSmsCount(): Int {
        if(processedImage == null)
            return 0

        val subId = SmsManager.getDefaultSmsSubscriptionId()
        return (if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context
            .getSystemService(SmsManager::class.java)
            .createForSubscriptionId(subId) else
            SmsManager.getSmsManagerForSubscriptionId(subId))
            .divideMessage(Base64.encodeToString(processedImage!!.rawBytes,
                Base64.DEFAULT)).size + smsCountPaddingValue
    }

    LaunchedEffect(processedImage) {
        smsCount = getSmsCount()
        size = processedImage?.rawBytes?.size ?: 0
    }

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
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            "")
                    }
                },
                actions = {
                    IconButton(onClick = onFinishCallback ) {
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
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Bitmap image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(250.dp),
                    )
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
                            (processedImage?.image?.width ?: bitmap.width).toString(),
                        )

                        ImageInfo(
                            stringResource(R.string.height),
                            (processedImage?.image?.height ?: bitmap.height).toString(),
                        )

                        ImageInfo(
                            stringResource(R.string.size),
                            stringResource(R.string.kb, size / 1000)
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
                            SliderImplementation(stringResource(R.string.quality)) {
                                qualityRatio = 100 - it
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
                                if(showQualitySlider) Icons.Default.ArrowDropUp else
                                    Icons.Default.ArrowDropDown,
                                "Drop down"
                            )
                        }
                    }

                    if(showResizeSlider || inPreviewMode) {
                        Spacer(Modifier.padding(8.dp))

                        Card( colors = CardDefaults
                            .cardColors(MaterialTheme.colorScheme.surfaceContainer),
                        ) {
                            Column {
                                SliderImplementation(stringResource(R.string.size)) {
                                    resizeRatio = if(it < 1) 1f else it
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
    sliderFinishedChangedCallback: (Float) -> Unit = {},
) {
    var sliderPosition by remember{ mutableFloatStateOf(0f) }
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
            steps = 100,
            valueRange = 0f..100f
        )

        Spacer(Modifier.padding(4.dp))

        OutlinedTextField(
            value = textValue,
            singleLine = true,
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

@Preview(showBackground = true)
@Composable
fun ImageRenderPreview() {
    Lib_image_androidTheme {
        val context = LocalContext.current
        val bitmap = BitmapFactory.decodeResource(context.resources,
            R.drawable._0241226_124819)

        val viewModel = remember{ ImageViewModel() }

        ImageRender(
            rememberNavController(),
            bitmap,
            viewModel
        ) {}
    }
}

