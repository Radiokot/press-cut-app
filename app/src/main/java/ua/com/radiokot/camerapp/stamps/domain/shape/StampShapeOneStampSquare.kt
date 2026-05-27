package ua.com.radiokot.camerapp.stamps.domain.shape

import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.unit.IntSize

object StampShapeOneStampSquare : StampShape {

    override val name = "onestamp_square"

    override val path: List<PathNode> by lazy {
        PathBuilder().apply {
            verticalLineTo(100f)
            horizontalLineTo(100f)
            verticalLineTo(0f)
            close()
        }.nodes
    }

    override val size =
        IntSize(
            width = 100,
            height = 100,
        )
}
