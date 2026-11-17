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

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.savedstate.serialization.saved
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ua.com.radiokot.camerapp.adjustments.ui.ImageAdjustmentsControllerViewModel
import ua.com.radiokot.camerapp.cut.domain.StampCutCameraImageSource
import ua.com.radiokot.camerapp.discardchanges.ui.ConfirmDiscardChangesContract
import ua.com.radiokot.camerapp.discardchanges.ui.confirmDiscardChangesDestination
import ua.com.radiokot.camerapp.intro.ui.PermissionsRoute
import ua.com.radiokot.camerapp.intro.ui.PermissionsScreenViewModel
import ua.com.radiokot.camerapp.intro.ui.permissionsDestination
import ua.com.radiokot.camerapp.ui.AppTheme
import ua.com.radiokot.camerapp.ui.LightAppColors
import ua.com.radiokot.camerapp.ui.LocalColors
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.util.fadeOutExit
import ua.com.radiokot.camerapp.util.keepWhile
import ua.com.radiokot.camerapp.util.lazyLogger

@Immutable
class NewStampActivity : ComponentActivity() {

    private val log by lazyLogger("NewStampActivity")
    private val sharedViewModel: NewStampViewModel by viewModel()
    private val permissionsScreenViewModel: PermissionsScreenViewModel by viewModel {
        parametersOf(
            PermissionsScreenViewModel.Parameters(
                isCameraRequiredNow = true,
            )
        )
    }

    private var isSplashScreenVisible by saved { true }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
            .keepWhile(::isSplashScreenVisible)
            .fadeOutExit()

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(0, 0),
        )

        super.onCreate(savedInstanceState)

        sharedViewModel.initOnce(
            collectionId =
                intent.getStringExtra(COLLECTION_ID_EXTRA),
            showToastOnSave =
                intent.getBooleanExtra(SHOW_TOAST_ON_SAVE_EXTRA, true),
            receivedImageUri =
                if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true)
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                else
                    null,
        )
        isSplashScreenVisible = false

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
                    StampCutNavHost(
                        sharedViewModel = sharedViewModel,
                        startWithPermissions = isPermissionActionRequired,
                        onDone = ::finish,
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
    sharedViewModel: NewStampViewModel,
    startWithPermissions: Boolean,
    onDone: () -> Unit,
) {
    val navController = rememberNavController()
    val confirmDiscardChangesContract = remember(navController) {
        ConfirmDiscardChangesContract(navController)
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
                PermissionsRoute(
                    isCameraRequiredNow = true,
                )
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
            val viewModel: StampCutScreenViewModel = koinViewModel {
                parametersOf(sharedViewModel)
            }
            val cameraImageSource = viewModel.imageSource as StampCutCameraImageSource
            val surfaceRequest by cameraImageSource.surfaceRequest.collectAsState()
            val cutImage by viewModel.cutImage.collectAsState()

            CompositionLocalProvider(
                LocalColors provides LightAppColors
            ) {
                StampCutFromCameraScreen(
                    useCaseGroup = cameraImageSource.useCaseGroup,
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

                        is StampCutScreenViewModel.Event.ProceedToSave -> {
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
        ) { navEntry ->

            val viewModel: StampSaveScreenViewModel = koinViewModel {
                parametersOf(sharedViewModel)
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
                sharedTransitionScope = this@StampCutNavHost,
                animatedVisibilityScope = this@composable,
                modifier = Modifier
                    .paperBackground(
                        drawBackgroundColor = true,
                    )
                    .fillMaxSize(),
            )

            val context = LocalContext.current
            val colors = LocalColors.current

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        StampSaveScreenViewModel.Event.ShowStampSavedMessage -> {
                            showToast(
                                context = context,
                                text = "Stamp saved",
                                durationMs = 1000,
                                colors = colors,
                            )
                        }

                        StampSaveScreenViewModel.Event.Done -> {
                            onDone()
                        }
                    }
                }
            }

            BackHandler(
                enabled = isDiscardConfirmationRequired,
            ) {
                confirmDiscardChangesContract.launch(
                    request = "Discard this stamp?",
                )
            }

            LaunchedEffect(navEntry, navController, confirmDiscardChangesContract) {
                confirmDiscardChangesContract
                    .getResultFlow(navEntry)
                    .collect { toDiscard ->
                        if (toDiscard) {
                            navController.popBackStack(
                                route = CutDestination,
                                inclusive = false,
                            )
                        }
                    }
            }
        }

        confirmDiscardChangesDestination(
            onDecision = confirmDiscardChangesContract::setResultAndNavigateUp,
        )
    }
}

private const val CutDestination = "cut"
private const val SaveDestination = "save"
