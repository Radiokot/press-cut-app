package ua.com.radiokot.camerapp.util

import kotlin.math.max

fun calculateBitmapSampleSize(
    originalWidth: Int,
    originalHeight: Int,
    maxWidth: Int,
    maxHeight: Int,
): Int {
    var sampleSize = 1

    while (
        originalWidth / (sampleSize * 2) >= maxWidth &&
        originalHeight / (sampleSize * 2) >= maxHeight
    ) {
        sampleSize *= 2
    }

    return max(1, sampleSize)
}
