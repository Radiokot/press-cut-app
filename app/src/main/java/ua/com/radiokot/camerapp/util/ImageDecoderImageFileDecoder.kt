package ua.com.radiokot.camerapp.util

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File

@RequiresApi(Build.VERSION_CODES.P)
class ImageDecoderImageFileDecoder : ImageFileDecoder {

    @RequiresApi(Build.VERSION_CODES.P)
    override fun invoke(
        filePath: String,
        targetWidth: Int?,
        targetHeight: Int?,
        maxWidth: Int?,
        maxHeight: Int?,
    ): Bitmap {
        val source = ImageDecoder.createSource(File(filePath))
        return ImageDecoder.decodeBitmap(source) { decoder, imageInfo, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_HARDWARE
            if (targetWidth != null && targetHeight != null) {
                decoder.setTargetSize(targetWidth, targetHeight)
            } else if (maxWidth != null && maxHeight != null) {
                decoder.setTargetSampleSize(
                    calculateBitmapSampleSize(
                        originalWidth = imageInfo.size.width,
                        originalHeight = imageInfo.size.height,
                        maxWidth = maxWidth,
                        maxHeight = maxHeight,
                    )
                )
            }
        }
    }
}
