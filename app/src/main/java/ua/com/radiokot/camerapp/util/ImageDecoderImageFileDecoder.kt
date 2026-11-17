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
    ): Bitmap {
        val source = ImageDecoder.createSource(File(filePath))
        return ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_HARDWARE
            if (targetWidth != null && targetHeight != null) {
                decoder.setTargetSize(targetWidth, targetHeight)
            }
        }
    }
}
