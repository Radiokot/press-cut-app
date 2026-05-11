package ua.com.radiokot.camerapp.io

import com.skydoves.landscapist.core.Landscapist
import com.skydoves.landscapist.core.LandscapistConfig
import com.skydoves.landscapist.core.fetcher.UriFetcher
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.camerapp.util.withoutMimeTypes

val ioModule = module {

    single<Landscapist> {
        val config = LandscapistConfig(
            diskCacheSize = 0L,
        )

        Landscapist.Builder()
            .config(config)
            .fetcher(
                // 1. Network fetcher is not needed at all;
                // 2. Erasing mime types prevents unnecessary checks
                //    and enables hardware bitmap config.
                UriFetcher(
                    networkFetcher = null,
                ).withoutMimeTypes()
            )
            .build()
    } bind Landscapist::class
}
