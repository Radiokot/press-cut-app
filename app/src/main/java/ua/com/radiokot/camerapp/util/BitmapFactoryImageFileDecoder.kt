package ua.com.radiokot.camerapp.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.min

class BitmapFactoryImageFileDecoder : ImageFileDecoder {

    override fun invoke(
        filePath: String,
        targetWidth: Int?,
        targetHeight: Int?,
        maxWidth: Int?,
        maxHeight: Int?
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

        val sampleSize = calculateBitmapSampleSize(
            originalWidth = originalWidth,
            originalHeight = originalHeight,
            maxWidth = min(
                targetWidth ?: originalWidth,
                maxWidth ?: originalWidth,
            ),
            maxHeight = min(
                targetHeight ?: originalHeight,
                maxHeight ?: originalHeight,
            ),
        )

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.HARDWARE
        }

        return BitmapFactory.decodeFile(filePath, decodeOptions)
    }
}
