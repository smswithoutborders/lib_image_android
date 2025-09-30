package com.afkanerd.lib_image_android.ui.navigation

import android.graphics.Bitmap
import kotlinx.serialization.Serializable

@Serializable
data object ImageRenderHomeNav

@Serializable
data class ImageRenderNav(
    var initialize: Boolean = true
)
