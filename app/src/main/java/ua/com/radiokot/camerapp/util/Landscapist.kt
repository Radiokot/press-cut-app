package ua.com.radiokot.camerapp.util

import androidx.compose.runtime.compositionLocalOf
import com.skydoves.landscapist.core.ImageRequest
import com.skydoves.landscapist.core.Landscapist
import com.skydoves.landscapist.core.LandscapistConfig
import com.skydoves.landscapist.core.decoder.DecodeResult
import com.skydoves.landscapist.core.decoder.ImageDecoder
import com.skydoves.landscapist.core.model.DataSource
import com.skydoves.landscapist.core.network.FetchResult
import com.skydoves.landscapist.core.network.ImageFetcher

class FileUriDecodingImageFetcher(
    private val decoder: ImageFileDecoder,
) : ImageFetcher {

    override suspend fun fetch(
        request: ImageRequest,
    ): FetchResult {
        val decoded = try {
            decoder(
                filePath =
                    (request.model as String)
                        .substring(URI_PATH_PREFIX.length),
                targetWidth = request.targetWidth,
                targetHeight = request.targetHeight,
            )
        } catch (e: Exception) {
            return FetchResult.Error(e)
        }

        return FetchResult.Decoded(
            image = decoded,
            width = decoded.width,
            height = decoded.height,
            dataSource = DataSource.DISK,
        )
    }

    override fun canHandle(model: Any?): Boolean {
        return model is String && model.startsWith(URI_PATH_PREFIX)
    }

    private companion object {
        private const val URI_PATH_PREFIX = "file://"
    }
}

class NoOpImageDecoder : ImageDecoder {
    override suspend fun decode(
        data: ByteArray,
        mimeType: String?,
        targetWidth: Int?,
        targetHeight: Int?,
        config: LandscapistConfig,
    ): DecodeResult {
        return DecodeResult.Error(
            IllegalStateException("No-op decoder can't decode")
        )
    }
}

val LocalLandscapist = compositionLocalOf<Landscapist?> { null }
