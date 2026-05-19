package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntSize
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.core.ImageRequest
import ua.com.radiokot.camerapp.util.noProgressive

@Immutable
class StampImageLoadingOptions(
    imageSize: IntSize,
) {
    val imageOptions: ImageOptions =
        ImageOptions(
            requestSize = imageSize,
        )

    val requestBuilder: ImageRequest.Builder.() -> Unit =
        noProgressive(
            size = imageSize,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StampImageLoadingOptions) return false

        if (imageOptions != other.imageOptions) return false

        return true
    }

    override fun hashCode(): Int {
        return imageOptions.hashCode()
    }
}
