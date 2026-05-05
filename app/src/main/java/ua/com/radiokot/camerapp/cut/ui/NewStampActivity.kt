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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.asIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.util.lazyLogger

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

        val areAllPermissionsGranted =
            permissionsScreenViewModel.areAllPermissionsGranted(
                context = this,
            )

        log.debug {
            "onCreate(): permissions checked:" +
                    "\nareAllPermissionsGranted=$areAllPermissionsGranted"
        }

        setContent {
            SharedTransitionLayout(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val context = LocalContext.current

                StampCutNavHost(
                    startWithPermissions = !areAllPermissionsGranted,
                    collectionId = collectionId,
                    onDidSave = {
                        if (showToastOnSave) {
                            Toast.makeText(
                                context,
                                "Stamp saved",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        finish()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                )
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
            modifier = Modifier
                .paperBackground(
                    drawBackgroundColor = true,
                )
        )

        composable(
            route = CutDestination,
        ) {
            val viewModel: StampCutScreenViewModel = koinViewModel()
            val surfaceRequest by viewModel.surfaceRequest.collectAsState()
            val cutImage by viewModel.cutImage.collectAsState()

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
                imageState = viewModel.adjustedStampImage.collectAsState(),
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
