package ua.com.radiokot.camerapp.stamps.domain.shape

import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.unit.IntSize

object StampShapeA : StampShape {

    override val name = "a"

    // https://composables.com/svg-to-compose
    override val path: List<PathNode> = PathBuilder().apply {
        moveTo(100f, 121.1f)
        verticalLineTo(115f)
        curveToRelative(-4f, 0.4f, -4f, -6.7f, 0f, -6.2f)
        verticalLineToRelative(-6.2f)
        curveToRelative(-4.4f, 0f, -3.7f, -6.7f, 0f, -6.2f)
        verticalLineTo(90f)
        curveToRelative(-4.1f, 0.4f, -4f, -6.7f, 0f, -6.2f)
        verticalLineToRelative(-6.2f)
        curveToRelative(-3.8f, 0.4f, -4.4f, -6.2f, 0f, -6.2f)
        verticalLineToRelative(-6.2f)
        curveToRelative(-4f, 0.4f, -4f, -6.7f, 0f, -6.2f)
        verticalLineToRelative(-6.2f)
        curveToRelative(-4.4f, 0f, -3.7f, -6.7f, 0f, -6.2f)
        verticalLineToRelative(-6.2f)
        curveToRelative(-3.8f, 0.4f, -4.4f, -6.2f, 0f, -6.2f)
        verticalLineToRelative(-6.4f)
        curveToRelative(-4f, 0.5f, -4f, -6.7f, 0f, -6.2f)
        verticalLineToRelative(-6.2f)
        curveToRelative(-4f, 0.5f, -4f, -6.7f, 0f, -6.2f)
        verticalLineTo(3.1f)
        arcTo(3f, 3f, 0f, false, true, 96.9f, 0f)
        horizontalLineToRelative(-6.3f)
        curveToRelative(0f, 4.1f, -6.2f, 4.1f, -6.2f, 0f)
        horizontalLineToRelative(-6.3f)
        curveToRelative(0f, 4.1f, -6.2f, 4.1f, -6.2f, 0f)
        horizontalLineToRelative(-6.3f)
        curveToRelative(0f, 4.1f, -6.2f, 4.1f, -6.2f, 0f)
        horizontalLineToRelative(-6.3f)
        curveToRelative(0f, 4.1f, -6.2f, 4.1f, -6.2f, 0f)
        horizontalLineToRelative(-6.3f)
        curveToRelative(0f, 4.1f, -6.2f, 4.1f, -6.2f, 0f)
        horizontalLineToRelative(-6.3f)
        curveToRelative(0f, 4.1f, -6.2f, 4.1f, -6.2f, 0f)
        horizontalLineToRelative(-6.3f)
        curveToRelative(0f, 4.1f, -6.2f, 4.1f, -6.2f, 0f)
        horizontalLineTo(3.1f)
        arcTo(3f, 3f, 0f, false, true, 0f, 3.1f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        arcToRelative(3f, 3f, 0f, false, true, 3.1f, 3.1f)
        horizontalLineToRelative(6.3f)
        curveToRelative(0f, -4.1f, 6.2f, -4.1f, 6.2f, 0f)
        horizontalLineToRelative(6.3f)
        curveToRelative(0f, -4.1f, 6.2f, -4.1f, 6.2f, 0f)
        horizontalLineToRelative(6.3f)
        curveToRelative(0f, -4.1f, 6.2f, -4.1f, 6.2f, 0f)
        horizontalLineToRelative(6.3f)
        curveToRelative(0f, -4.1f, 6.2f, -4.1f, 6.2f, 0f)
        horizontalLineToRelative(6.3f)
        curveToRelative(0f, -4.1f, 6.2f, -4.1f, 6.2f, 0f)
        horizontalLineToRelative(6.3f)
        curveToRelative(0f, -4.1f, 6.2f, -4.1f, 6.2f, 0f)
        horizontalLineToRelative(6.3f)
        curveToRelative(0f, -4.1f, 6.2f, -4.1f, 6.2f, 0f)
        horizontalLineToRelative(6.3f)
        arcToRelative(3f, 3f, 0f, false, true, 3.1f, -2.9f)
    }.nodes

    override val size =
        IntSize(
            width = 100,
            height = 124,
        )
}

