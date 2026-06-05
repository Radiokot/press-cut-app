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

package ua.com.radiokot.camerapp.intro.ui

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableSet
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.ui.AppTheme
import ua.com.radiokot.camerapp.ui.LeTextButton
import ua.com.radiokot.camerapp.ui.LocalColors
import ua.com.radiokot.camerapp.ui.PodkovaFamily
import ua.com.radiokot.camerapp.ui.Vignette
import ua.com.radiokot.camerapp.ui.paperBackground

@Composable
fun PermissionsScreen(
    modifier: Modifier = Modifier,
    permissions: ImmutableList<String>,
    isDocumentTreeAccessRequired: Boolean,
    onGrantAction: () -> Unit,
) = Column(
    modifier = modifier
        .safeContentPadding()
        .padding(24.dp),
) {
    val permissionSet = remember(permissions) {
        permissions.toImmutableSet()
    }

    BasicText(
        text = "Press-Cut needs a few permissions to run",
        style = TextStyle(
            fontFamily = PodkovaFamily,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            color = LocalColors.current.textPrimary,
        ),
        modifier = Modifier
            .fillMaxWidth()
    )

    Vignette(
        modifier = Modifier
            .padding(
                vertical = 32.dp,
            )
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(
                state = rememberScrollState(),
                overscrollEffect = null,
            )
    ) {
        if (Manifest.permission.CAMERA in permissionSet) {
            PermissionItem(
                icon = painterResource(R.drawable.viewfinder_by_gregor_cresnar_from_noun_project),
                name = "Camera",
                description = "Stamps are created from from what your camera sees",
            )
        }

        if (Manifest.permission.WRITE_EXTERNAL_STORAGE in permissionSet
            || Manifest.permission.READ_EXTERNAL_STORAGE in permissionSet
        ) {
            Spacer(
                modifier = Modifier
                    .height(24.dp)
            )

            PermissionItem(
                icon = painterResource(R.drawable.layers_by_gregor_cresnar_from_noun_project),
                name = "Read and write external storage",
                description = "Stamps are stored in the \"Pictures\" folder, " +
                        " the permission is needed to access it. " +
                        "The app won't read your other files",
            )
        }

        if (Manifest.permission.READ_MEDIA_IMAGES in permissionSet) {
            Spacer(
                modifier = Modifier
                    .height(24.dp)
            )

            PermissionItem(
                icon = painterResource(R.drawable.photo_by_gregor_cresnar_from_noun_project),
                name = "Read images",
                description = "Stamps are stored in the \"Pictures\" folder, " +
                        " the permission is needed to access it. " +
                        "The app won't read your photos",
            )
        }

        if (isDocumentTreeAccessRequired) {
            Spacer(
                modifier = Modifier
                    .height(24.dp)
            )

            PermissionItem(
                icon = painterResource(R.drawable.layers_by_gregor_cresnar_from_noun_project),
                name = "Manage the stamps folder",
                description = "This one is necessary if you reinstall the app " +
                        "or add stamps to the folder manually. " +
                        "It's just better granted in advance",
            )
        }
    }

    Spacer(
        modifier = Modifier
            .height(24.dp)
    )

    LeTextButton(
        text = "Grant Permissions",
        onClick = onGrantAction,
    )
}

@Composable
private fun PermissionItem(
    icon: Painter,
    name: String,
    description: String,
) {
    Row {
        Image(
            painter = icon,
            contentDescription = "Icon",
            colorFilter = ColorFilter.tint(LocalColors.current.standaloneIcon),
            modifier = Modifier
                .padding(
                    top = 4.dp,
                    end = 24.dp,
                )
                .size(32.dp)
        )

        Column {
            val colors = LocalColors.current
            BasicText(
                text = name,
                style = TextStyle(
                    fontFamily = PodkovaFamily,
                    fontSize = 20.sp,
                    color = colors.textPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
            BasicText(
                text = description,
                style = TextStyle(
                    fontFamily = PodkovaFamily,
                    fontSize = 16.sp,
                    color = colors.textSecondary,
                ),
                modifier = Modifier
                    .padding(
                        top = 2.dp,
                    )
                    .fillMaxWidth()
            )
        }
    }
}

@SuppressLint("InlinedApi")
@PreviewLightDark
@Composable
private fun PermissionsScreenPreview() {
    AppTheme {

        PermissionsScreen(
            permissions =
                persistentListOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_MEDIA_IMAGES,
                ),
            isDocumentTreeAccessRequired = true,
            onGrantAction = {},
            modifier = Modifier
                .fillMaxSize()
                .paperBackground(
                    drawBackgroundColor = true,
                )
        )
    }
}
