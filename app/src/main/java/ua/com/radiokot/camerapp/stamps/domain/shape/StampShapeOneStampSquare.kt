package ua.com.radiokot.camerapp.stamps.domain.shape

import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.unit.IntSize

object StampShapeOneStampSquare : StampShape {

    override val name = "onestamp_square"

    override val path: List<PathNode> by lazy {
        PathBuilder().apply {
            moveTo(97f, 0f)
            curveTo(97f, 1.65685f, 98.3431f, 3f, 100f, 3f)
            verticalLineTo(7f)
            curveTo(96.1309f, 7f, 96.1309f, 13f, 100f, 13f)
            verticalLineTo(17f)
            curveTo(96.1309f, 17f, 96.1309f, 23f, 100f, 23f)
            verticalLineTo(27f)
            curveTo(96.1309f, 27f, 96.1309f, 33f, 100f, 33f)
            verticalLineTo(37f)
            curveTo(96.1309f, 37f, 96.1309f, 43f, 100f, 43f)
            verticalLineTo(47f)
            curveTo(96.1309f, 47f, 96.1309f, 53f, 100f, 53f)
            verticalLineTo(57f)
            curveTo(96.1309f, 57f, 96.1309f, 63f, 100f, 63f)
            verticalLineTo(67f)
            curveTo(96.1309f, 67f, 96.1309f, 73f, 100f, 73f)
            verticalLineTo(77f)
            curveTo(96.1309f, 77f, 96.1309f, 83f, 100f, 83f)
            verticalLineTo(87f)
            curveTo(96.1309f, 87f, 96.1309f, 93f, 100f, 93f)
            verticalLineTo(97f)
            curveTo(98.3431f, 97f, 97f, 98.3431f, 97f, 100f)
            horizontalLineTo(93f)
            curveTo(93f, 96.1309f, 87f, 96.1309f, 87f, 100f)
            horizontalLineTo(83f)
            curveTo(83f, 96.1309f, 77f, 96.1309f, 77f, 100f)
            horizontalLineTo(73f)
            curveTo(73f, 96.1309f, 67f, 96.1309f, 67f, 100f)
            horizontalLineTo(63f)
            curveTo(63f, 96.1309f, 57f, 96.1309f, 57f, 100f)
            horizontalLineTo(53f)
            curveTo(53f, 96.1309f, 47f, 96.1309f, 47f, 100f)
            horizontalLineTo(43f)
            curveTo(43f, 96.1309f, 37f, 96.1309f, 37f, 100f)
            horizontalLineTo(33f)
            curveTo(33f, 96.1309f, 27f, 96.1309f, 27f, 100f)
            horizontalLineTo(23f)
            curveTo(23f, 96.1309f, 17f, 96.1309f, 17f, 100f)
            horizontalLineTo(13f)
            curveTo(13f, 96.1309f, 7f, 96.1309f, 7f, 100f)
            horizontalLineTo(3f)
            curveTo(3f, 98.3431f, 1.65685f, 97f, 0f, 97f)
            verticalLineTo(93f)
            curveTo(3.86908f, 93f, 3.86908f, 87f, 0f, 87f)
            verticalLineTo(83f)
            curveTo(3.86908f, 83f, 3.86908f, 77f, 0f, 77f)
            verticalLineTo(73f)
            curveTo(3.86908f, 73f, 3.86908f, 67f, 0f, 67f)
            verticalLineTo(63f)
            curveTo(3.86908f, 63f, 3.86908f, 57f, 0f, 57f)
            verticalLineTo(53f)
            curveTo(3.86908f, 53f, 3.86908f, 47f, 0f, 47f)
            verticalLineTo(43f)
            curveTo(3.86908f, 43f, 3.86908f, 37f, 0f, 37f)
            verticalLineTo(33f)
            curveTo(3.86908f, 33f, 3.86908f, 27f, 0f, 27f)
            verticalLineTo(23f)
            curveTo(3.86908f, 23f, 3.86908f, 17f, 0f, 17f)
            verticalLineTo(13f)
            curveTo(3.86908f, 13f, 3.86908f, 7f, 0f, 7f)
            verticalLineTo(3f)
            curveTo(1.65685f, 3f, 3f, 1.65685f, 3f, 0f)
            horizontalLineTo(7f)
            curveTo(7f, 3.86908f, 13f, 3.86908f, 13f, 0f)
            horizontalLineTo(17f)
            curveTo(17f, 3.86908f, 23f, 3.86908f, 23f, 0f)
            horizontalLineTo(27f)
            curveTo(27f, 3.86908f, 33f, 3.86908f, 33f, 0f)
            horizontalLineTo(37f)
            curveTo(37f, 3.86908f, 43f, 3.86908f, 43f, 0f)
            horizontalLineTo(47f)
            curveTo(47f, 3.86908f, 53f, 3.86908f, 53f, 0f)
            horizontalLineTo(57f)
            curveTo(57f, 3.86908f, 63f, 3.86908f, 63f, 0f)
            horizontalLineTo(67f)
            curveTo(67f, 3.86908f, 73f, 3.86908f, 73f, 0f)
            horizontalLineTo(77f)
            curveTo(77f, 3.86908f, 83f, 3.86908f, 83f, 0f)
            horizontalLineTo(87f)
            curveTo(87f, 3.86908f, 93f, 3.86908f, 93f, 0f)
            horizontalLineTo(97f)
            close()
        }.nodes
    }

    override val size =
        IntSize(
            width = 100,
            height = 100,
        )
}
