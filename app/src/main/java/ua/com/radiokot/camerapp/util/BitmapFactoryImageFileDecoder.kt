package ua.com.radiokot.camerapp.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.max

class BitmapFactoryImageFileDecoder : ImageFileDecoder {

    override fun invoke(
        filePath: String,
        targetWidth: Int?,
        targetHeight: Int?,
    ): Bitmap {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, boundsOptions)

        val originalWidth = boundsOptions.outWidth
        val originalHeight = boundsOptions.outHeight

        if (originalWidth <= 0 || originalHeight <= 0) {
            error("Failed to decode image dimensions")
        }

        val sampleSize = calculateSampleSize(
            originalWidth = originalWidth,
            originalHeight = originalHeight,
            targetWidth = targetWidth ?: originalWidth,
            targetHeight = targetHeight ?: originalHeight,
        )

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.HARDWARE
        }

        return BitmapFactory.decodeFile(filePath, decodeOptions)
    }

    private fun calculateSampleSize(
        originalWidth: Int,
        originalHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
    ): Int {
        var sampleSize = 1

        while (
            originalWidth / (sampleSize * 2) >= targetWidth &&
            originalHeight / (sampleSize * 2) >= targetHeight
        ) {
            sampleSize *= 2
        }

        return max(1, sampleSize)
    }
}
