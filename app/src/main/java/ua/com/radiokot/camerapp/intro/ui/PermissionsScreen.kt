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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableSet
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.ui.LeTextButton
import ua.com.radiokot.camerapp.ui.Vignette
import ua.com.radiokot.camerapp.ui.podkovaFamily

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
        text = "Press-cut needs a few permissions to run",
        style = TextStyle(
            fontFamily = podkovaFamily,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
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
        text = "Grant permissions",
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
            colorFilter = ColorFilter.tint(Color(0xFFB9AC8C)),
            modifier = Modifier
                .padding(
                    top = 4.dp,
                    end = 24.dp,
                )
                .size(32.dp)
        )

        Column {
            BasicText(
                text = name,
                style = PermissionNameTextStyle,
                modifier = Modifier
                    .fillMaxWidth()
            )
            BasicText(
                text = description,
                style = PermissionDescriptionTextStyle,
                modifier = Modifier
                    .padding(
                        top = 2.dp,
                    )
                    .fillMaxWidth()
            )
        }
    }
}

private val PermissionNameTextStyle =
    TextStyle(
        fontFamily = podkovaFamily,
        fontSize = 20.sp,
    )
private val PermissionDescriptionTextStyle =
    TextStyle(
        fontFamily = podkovaFamily,
        fontSize = 16.sp,
        color = Color(0xff7e7a74),
    )

@SuppressLint("InlinedApi")
@Preview
@Composable
private fun PermissionsScreenPreview(

) {
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
    )
}
