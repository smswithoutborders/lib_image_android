package com.afkanerd.lib_image_android.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data class ImageRenderNav(
    val uri: String,
    val smsCountPadding: Int = 0,
)

