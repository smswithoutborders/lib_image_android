package com.afkanerd.lib_image_android.extensions

import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes

fun Context.getUriForDrawable(@DrawableRes drawableId: Int): Uri {
    // 1. Get the package name of your application
    val packageName = packageName

    // 2. Build the URI
    // The path segments are typically "resource" and the resource ID
    val uri = Uri.Builder()
        .scheme("android.resource") // The scheme is always "android.resource"
        .authority(packageName)     // The authority is the package name
        .path(drawableId.toString()) // The path is the resource ID
        .build()

    return uri
}