package ua.com.radiokot.camerapp.posters.domain

import androidx.annotation.ColorInt
import androidx.annotation.FloatRange

class StampPosterOptions(
    @FloatRange(from = 0.2, to = 2.0)
    val scale: Float,
    val withCaption: Boolean,
    val colors: Colors,
) {
    init {
        require(scale >= 0.2) {
            "A poster that small's fookin' useless, innit?"
        }
        require(scale <= 2) {
            "You off yer nut, mate?"
        }
    }

    class Colors(
        @ColorInt
        val paperBackground: Int,
        @ColorInt
        val paperBackgroundLine: Int,
        @ColorInt
        val stampShadow: Int,
        @ColorInt
        val caption: Int,
    )
}
