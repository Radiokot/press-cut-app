/* Copyright 2026 Oleg Koretsky

   This file is part of the Press-Cut,
   a digital postage stamp cutter Android app.

   Press-Cut is free software: you can redistribute it
   and/or modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation, either version 3 of the License,
   or (at your option) any later version.

   Press-Cut is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
   See the GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Press-Cut. If not, see <http://www.gnu.org/licenses/>.
*/

package ua.com.radiokot.camerapp.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity

@Composable
fun rememberSnapFlingBehavior(
    lazyListState: LazyListState,
    snapPosition: SnapPosition = SnapPosition.Center,
    snapAnimationSpec: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
    frictionMultiplier: Float = 1f,
): FlingBehavior {
    val snapLayoutInfoProvider = remember(lazyListState) {
        SnapLayoutInfoProvider(lazyListState, snapPosition)
    }
    val density = LocalDensity.current
    val decayAnimationSpec = remember(frictionMultiplier) {
        FloatExponentialDecaySpec(
            frictionMultiplier = frictionMultiplier,
        ).generateDecayAnimationSpec<Float>()
    }
    return remember(snapLayoutInfoProvider, decayAnimationSpec, density) {
        snapFlingBehavior(
            snapLayoutInfoProvider = snapLayoutInfoProvider,
            decayAnimationSpec = decayAnimationSpec,
            snapAnimationSpec = snapAnimationSpec,
        )
    }
}
