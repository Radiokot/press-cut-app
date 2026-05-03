package ua.com.radiokot.camerapp.ui

import com.skydoves.landscapist.core.ImageRequest

fun ImageRequest.Builder.noProgressive() {
    progressiveEnabled(false)
}
