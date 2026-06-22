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

package ua.com.radiokot.camerapp.cut.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.asIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ua.com.radiokot.camerapp.intro.ui.PermissionsRoute
import ua.com.radiokot.camerapp.intro.ui.PermissionsScreenViewModel
import ua.com.radiokot.camerapp.intro.ui.permissionsDestination
import ua.com.radiokot.camerapp.ui.AppTheme
import ua.com.radiokot.camerapp.ui.LightAppColors
import ua.com.radiokot.camerapp.ui.LocalColors
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.util.lazyLogger

@Immutable
class NewStampActivity : ComponentActivity() {

    private val log by lazyLogger("NewStampActivity")

    private val permissionsScreenViewModel: PermissionsScreenViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(0, 0),
        )
        super.onCreate(savedInstanceState)

        val collectionId: String? = intent.getStringExtra(COLLECTION_ID_EXTRA)
        val showToastOnSave = intent.getBooleanExtra(SHOW_TOAST_ON_SAVE_EXTRA, true)

        val isPermissionActionRequired = permissionsScreenViewModel.isActionRequired

        log.debug {
            "onCreate(): permissions checked:" +
                    "\nisPermissionActionRequired=$isPermissionActionRequired"
        }

        if (isPermissionActionRequired) {
            log.info {
                "Permission action is required"
            }
        }

        setContent {
            AppTheme {
                SharedTransitionLayout(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    val colors = LocalColors.current

                    StampCutNavHost(
                        startWithPermissions = isPermissionActionRequired,
                        collectionId = collectionId,
                        onDidSave = {
                            if (showToastOnSave) {
                                showToast(
                                    context = this@NewStampActivity,
                                    text = "Stamp saved",
                                    length = Toast.LENGTH_SHORT,
                                    colors = colors,
                                )
                            }
                            finish()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            }
        }
    }

    companion object {
        private const val COLLECTION_ID_EXTRA = "collectionId"
        private const val SHOW_TOAST_ON_SAVE_EXTRA = "showToastOnSave"

        /**
         * @param collectionId ID of a specific collection to save the stamp into,
         * if not specified it's saved into the primary collection.
         */
        fun getBundle(
            collectionId: String?,
            showToastOnSave: Boolean,
        ) = Bundle().apply {
            putString(COLLECTION_ID_EXTRA, collectionId)
            putBoolean(SHOW_TOAST_ON_SAVE_EXTRA, showToastOnSave)
        }
    }
}

@Composable
private fun SharedTransitionScope.StampCutNavHost(
    modifier: Modifier = Modifier,
    startWithPermissions: Boolean,
    collectionId: String?,
    onDidSave: () -> Unit,
) {
    val navController = rememberNavController()
    var stampImageBitmapToSave by remember {
        mutableStateOf<Bitmap?>(null)
    }
    val paperBackgroundWithColorModifier =
        Modifier
            .paperBackground(
                drawBackgroundColor = true,
            )

    NavHost(
        navController = navController,
        startDestination =
            if (startWithPermissions)
                PermissionsRoute
            else
                CutDestination,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        modifier = modifier
    ) {
        permissionsDestination(
            onDone = {
                navController.navigate(
                    route = CutDestination,
                ) {
                    popUpTo(PermissionsRoute) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
            modifier = paperBackgroundWithColorModifier
        )

        composable(
            route = CutDestination,
        ) {
            val viewModel: StampCutScreenViewModel = koinViewModel()
            val surfaceRequest by viewModel.surfaceRequest.collectAsState()
            val cutImage by viewModel.cutImage.collectAsState()

            CompositionLocalProvider(
                LocalColors provides LightAppColors
            ) {
                StampCutScreen(
                    useCases = viewModel.useCases,
                    surfaceRequest = surfaceRequest,
                    cutImage = cutImage,
                    onCutAction = viewModel::onCutAction,
                    sharedTransitionScope = this@StampCutNavHost,
                    animatedVisibilityScope = this@composable,
                    modifier = Modifier
                        .fillMaxSize()
                )
            }

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {

                        is StampCutScreenViewModel.Event.DidCut -> {
                            stampImageBitmapToSave?.recycle()
                            stampImageBitmapToSave = event.stampImageBitmap

                            navController.navigate(
                                route = SaveDestination,
                            ) {
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }

            DisposableEffect(viewModel) {
                onDispose {
                    viewModel.onScreenDisposed()
                }
            }
        }

        composable(
            route = SaveDestination,
        ) {
            val viewModel: StampSaveScreenViewModel = koinViewModel {
                parametersOf(
                    StampSaveScreenViewModel.Parameters(
                        collectionId = collectionId,
                        stampImageBitmap =
                            stampImageBitmapToSave
                                ?: error("Can't open the save screen without the bitmap to save")
                    )
                )
            }
            val imageAdjustmentsControllerViewModel: ImageAdjustmentsControllerViewModel =
                viewModel.imageAdjustmentsControllerViewModel
            val isDiscardConfirmationRequired
                    by viewModel.isDiscardConfirmationRequired.collectAsState()

            StampSaveScreen(
                captionInputState = viewModel.captionInput,
                imageState = viewModel.previewStampImage.collectAsState(),
                onImagePreviewSizeChanged = viewModel::onPreviewSizeChanged,
                onSaveAction = viewModel::onSaveAction,
                adjustmentsControllerItems = imageAdjustmentsControllerViewModel.items,
                currentAdjustmentsControllerItemState =
                    imageAdjustmentsControllerViewModel.currentItem.collectAsState(),
                onCurrentAdjustmentsControllerItemChanged =
                    imageAdjustmentsControllerViewModel::onCurrentItemChanged,
                adjustmentsControllerValueState =
                    imageAdjustmentsControllerViewModel
                        .currentValue
                        .collectAsState()
                        .asIntState(),
                sharedTransitionScope = this@StampCutNavHost,
                animatedVisibilityScope = this@composable,
                onAdjustmentsControllerValueChanged =
                    imageAdjustmentsControllerViewModel::onValueChanged,
                modifier = Modifier
                    .paperBackground(
                        drawBackgroundColor = true,
                    )
                    .fillMaxSize(),
            )

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        StampSaveScreenViewModel.Event.DidSave -> {
                            onDidSave()
                        }
                    }
                }
            }

            BackHandler(
                enabled = isDiscardConfirmationRequired,
            ) {
                navController.navigate(
                    route = ConfirmDiscardDestination,
                ) {
                    launchSingleTop = true
                }
            }
        }

        dialog(
            route = ConfirmDiscardDestination,
        ) {
            StampDiscardConfirmationDialog(
                onConfirmDiscard = {
                    navController.popBackStack(
                        route = CutDestination,
                        inclusive = false,
                    )
                },
                onCancel = navController::navigateUp,
            )
        }
    }
}

private const val CutDestination = "cut"
private const val SaveDestination = "save"
private const val ConfirmDiscardDestination = "discardConfirmation"
