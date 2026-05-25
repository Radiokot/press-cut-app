package ua.com.radiokot.camerapp.util

import androidx.compose.ui.unit.IntSize
import com.skydoves.landscapist.components.ImagePluginComponent
import com.skydoves.landscapist.core.ImageRequest
import com.skydoves.landscapist.core.network.FetchResult
import com.skydoves.landscapist.core.network.ImageFetcher

fun noProgressive(
    size: IntSize,
) =
    fun ImageRequest.Builder.() {
        size(
            width = size.width,
            height = size.height,
        )
        progressiveEnabled(false)
    }

val EmptyImageComponent = ImagePluginComponent()

private class ImageFetcherWithoutMimeType(
    private val delegate: ImageFetcher,
) : ImageFetcher by delegate {

    override suspend fun fetch(request: ImageRequest): FetchResult {
        val result = delegate.fetch(request)
        return if (result is FetchResult.Success)
            result.copy(mimeType = null)
        else
            result
    }
}

fun ImageFetcher.withoutMimeTypes(): ImageFetcher =
    ImageFetcherWithoutMimeType(
        delegate = this,
    )
