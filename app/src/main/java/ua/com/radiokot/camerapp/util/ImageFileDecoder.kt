package ua.com.radiokot.camerapp.util

import android.graphics.Bitmap

interface ImageFileDecoder {
    operator fun invoke(
        filePath: String,
        targetWidth: Int? = null,
        targetHeight: Int? = null,
        maxWidth: Int? = null,
        maxHeight: Int? = null,
    ): Bitmap
}

