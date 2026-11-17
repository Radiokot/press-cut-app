package ua.com.radiokot.camerapp.util

import android.graphics.Bitmap

fun interface ImageFileDecoder {
    operator fun invoke(
        filePath: String,
        targetWidth: Int?,
        targetHeight: Int?,
    ): Bitmap
}

