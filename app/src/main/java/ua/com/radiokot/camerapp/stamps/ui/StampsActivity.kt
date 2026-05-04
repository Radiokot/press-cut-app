@file:Suppress("FunctionName")

package ua.com.radiokot.camerapp.stamps.ui

import android.annotation.SuppressLint
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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastRoundToInt
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.skydoves.landscapist.image.LocalLandscapist
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ua.com.radiokot.camerapp.cut.ui.NewStampActivity
import ua.com.radiokot.camerapp.intro.ui.PermissionsDestination
import ua.com.radiokot.camerapp.intro.ui.PermissionsScreenViewModel
import ua.com.radiokot.camerapp.intro.ui.permissionsDestination
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.util.lazyLogger

class StampsActivity : ComponentActivity() {

    private val log by lazyLogger("StampsActivity")

    private val permissionsScreenViewModel: PermissionsScreenViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(0, 0),
        )
        super.onCreate(savedInstanceState)

        val areAllPermissionsGranted =
            permissionsScreenViewModel.areAllPermissionsGranted(
                context = this,
            )

        log.debug {
            "onCreate(): permissions checked:" +
                    "\nareAllPermissionsGranted=$areAllPermissionsGranted"
        }

        setContent {
            CompositionLocalProvider(
                LocalLandscapist provides koinInject(),
            ) {
                var isStampsScreenWarmupShown by remember {
                    mutableStateOf(true)
                }

                // First appearance of the stamps screen is slow,
                // there's something with the mere existence of a LazyVerticalGrid in it.
                // Until I find the cause, making the screen appear invisible for the first time
                // makes further appearance and animation smooth.
                if (isStampsScreenWarmupShown) {
                    @SuppressLint("UnrememberedMutableState")
                    StampsScreen(
                        collectionId = "",
                        collectionNameInputState = TextFieldState("I ❤️ weird hacks"),
                        focusCollectionNameInput = false,
                        stamps = mutableStateOf(persistentListOf()),
                        onStampClicked = {},
                        onNewStampAction = {},
                        sharedTransitionScope = null,
                        animatedVisibilityScope = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.01f)
                    )

                    LaunchedEffect(Unit) {
                        isStampsScreenWarmupShown = false
                    }
                }

                SharedTransitionLayout {
                    StampsNavHost(
                        startWithPermissions = !areAllPermissionsGranted,
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun SharedTransitionScope.StampsNavHost(
    modifier: Modifier = Modifier,
    startWithPermissions: Boolean,
) {
    val navController = rememberNavController()
    val totalScrollOffsetState = remember {
        mutableIntStateOf(0)
    }
    val totalScrollOffsetCounter = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                totalScrollOffsetState.intValue += consumed.y.fastRoundToInt()
                return Offset.Zero
            }
        }
    }
    val context = LocalContext.current

    fun proceedToNewStamp(
        collectionId: String?,
    ) {
        val newStampIntent =
            Intent(context, NewStampActivity::class.java)
                .putExtras(
                    NewStampActivity.getBundle(
                        collectionId = collectionId,
                        showToastOnSave = false,
                    )
                )
        context.startActivity(newStampIntent)
    }

    NavHost(
        navController = navController,
        startDestination =
            if (startWithPermissions)
                PermissionsDestination
            else
                CollectionsDestination,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        modifier = modifier
            .paperBackground(
                verticalOffset = totalScrollOffsetState::value,
            )
            .nestedScroll(totalScrollOffsetCounter)
    ) {
        permissionsDestination(
            onDone = {
                navController.navigate(
                    route = CollectionsDestination,
                ) {
                    popUpTo(PermissionsDestination) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        )

        composable(
            route = CollectionsDestination,
        ) {
            val viewModel: CollectionsScreenViewModel = koinViewModel()
            val items = viewModel.items.collectAsState()

            CollectionsScreen(
                itemsState = items,
                onItemClicked = viewModel::onItemClicked,
                onItemLongClicked = viewModel::onItemLongClicked,
                onNewStampAction = viewModel::onNewStampAction,
                onNewCollectionAction = viewModel::onNewCollectionAction,
                sharedTransitionScope = this@StampsNavHost,
                animatedVisibilityScope = this@composable,
                modifier = Modifier
                    .fillMaxSize()
            )

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        is CollectionsScreenViewModel.Event.ProceedToCollection -> {
                            navController.navigate(
                                route = CollectionDestination(
                                    collectionId = event.collectionId,
                                    focusNameInput = event.focusNameInput,
                                )
                            ) {
                                launchSingleTop = true
                            }
                        }

                        is CollectionsScreenViewModel.Event.ProceedToCollectionActions -> {
                            navController.navigate(
                                route = CollectionActionsDestination(
                                    collectionId = event.collectionId,
                                )
                            ) {
                                launchSingleTop = true
                            }
                        }

                        is CollectionsScreenViewModel.Event.ProceedToNewStamp -> {
                            proceedToNewStamp(
                                collectionId = null,
                            )
                        }
                    }
                }
            }
        }

        composable(
            route = CollectionActionsDestination,
            arguments = listOf(
                navArgument(CollectionDestinationCollectionId) {
                    type = NavType.StringType
                },
            ),
        ) { navEntry ->
            val viewModel: CollectionActionsScreenViewModel = koinViewModel {
                parametersOf(
                    CollectionActionsScreenViewModel.Parameters(
                        collectionId =
                            navEntry
                                .arguments
                                ?.getString(CollectionDestinationCollectionId)
                                ?: error("No ID argument passed"),
                    )
                )
            }

            CollectionActionsScreen(
                collection = viewModel.collectionItem,
                canDelete = viewModel.canDelete,
                onMoveStampsAction = viewModel::onMoveStampsAction,
                onDeleteAction = viewModel::onDeleteAction,
                sharedTransitionScope = this@StampsNavHost,
                animatedVisibilityScope = this@composable,
                modifier = Modifier
                    .fillMaxSize()
            )

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        is CollectionActionsScreenViewModel.Event.ProceedToMoveDestinationCollectionSelection -> {
                            navController.navigate(
                                route = MoveDestinationCollectionSelectionDestination(
                                    currentCollectionId = event.currentCollectionId,
                                )
                            ) {
                                launchSingleTop = true
                            }
                        }

                        is CollectionActionsScreenViewModel.Event.ProceedToMoveStamps -> {
                            navController.navigate(
                                route = MoveStampsDestination(
                                    sourceCollectionId = event.sourceCollectionId,
                                    destinationCollectionId = event.destinationCollectionId,
                                )
                            ) {
                                popUpTo(CollectionActionsDestination) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }

                        is CollectionActionsScreenViewModel.Event.Done -> {
                            navController.navigateUp()
                        }
                    }
                }
            }

            LaunchedEffect(viewModel) {
                navEntry
                    .savedStateHandle
                    .getStateFlow(
                        key = SelectedCollectionId,
                        initialValue = null
                    )
                    .filterNotNull()
                    .distinctUntilChanged()
                    .collect(viewModel::onMoveDestinationCollectionSelected)
            }
        }

        dialog(
            route = MoveDestinationCollectionSelectionDestination,
            arguments = listOf(
                navArgument(CollectionDestinationCollectionId) {
                    type = NavType.StringType
                },
            ),
        ) { navEntry ->
            val viewModel: MoveToCollectionDialogViewModel = koinViewModel {
                parametersOf(
                    MoveToCollectionDialogViewModel.Parameters(
                        moveFromCollectionId =
                            navEntry
                                .arguments
                                ?.getString(CollectionDestinationCollectionId)
                                ?: error("No ID argument passed"),
                    )
                )
            }

            MoveToCollectionDialog(
                collections = viewModel.collections,
                onCollectionSelected = viewModel::onCollectionSelected,
                onNewCollectionAction = viewModel::onNewCollectionAction,
                onCancel = navController::navigateUp,
            )

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        is MoveToCollectionDialogViewModel.Event.CollectionSelected -> {
                            navController
                                .previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(
                                    key = SelectedCollectionId,
                                    value = event.collectionId,
                                )
                            navController.navigateUp()
                        }
                    }
                }
            }
        }

        composable(
            route = MoveStampsDestination,
            arguments = listOf(
                navArgument(CollectionDestinationCollectionId) {
                    type = NavType.StringType
                },
                navArgument(SelectedCollectionId) {
                    type = NavType.StringType
                },
            ),
        ) { navEntry ->
            val viewModel: MoveStampsScreenViewModel = koinViewModel {
                parametersOf(
                    MoveStampsScreenViewModel.Parameters(
                        sourceCollectionId =
                            navEntry
                                .arguments
                                ?.getString(CollectionDestinationCollectionId)
                                ?: error("No source collection ID argument passed"),
                        destinationCollectionId =
                            navEntry
                                .arguments
                                ?.getString(SelectedCollectionId)
                                ?: error("No destination collection ID argument passed"),
                    )
                )
            }

            MoveStampsScreen(
                progressState = viewModel.progress.collectAsState().asFloatState(),
                modifier = Modifier
                    .fillMaxSize()
            )

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        is MoveStampsScreenViewModel.Event.Done -> {
                            navController.navigateUp()
                        }
                    }
                }
            }
        }

        composable(
            route = CollectionDestination,
            arguments = listOf(
                navArgument(CollectionDestinationCollectionId) {
                    type = NavType.StringType
                },
                navArgument(CollectionDestinationFocusNameInput) {
                    type = NavType.BoolType
                }
            ),
        ) { navEntry ->
            val viewModel: StampsScreenViewModel = koinViewModel {
                parametersOf(
                    StampsScreenViewModel.Parameters(
                        collectionId =
                            navEntry
                                .arguments
                                ?.getString(CollectionDestinationCollectionId)
                                ?: error("No ID argument passed"),
                    )
                )
            }
            val stamps = viewModel.stamps.collectAsState()
            val focusCollectionNameInput =
                navEntry
                    .arguments
                    ?.getBoolean(CollectionDestinationFocusNameInput)
                    ?: false

            StampsScreen(
                collectionId = viewModel.collectionId,
                collectionNameInputState = viewModel.collectionNameInput,
                focusCollectionNameInput = focusCollectionNameInput,
                stamps = stamps,
                onStampClicked = viewModel::onStampClicked,
                onNewStampAction = viewModel::onNewStampAction,
                sharedTransitionScope = this@StampsNavHost,
                animatedVisibilityScope = this@composable,
                modifier = Modifier
                    .fillMaxSize()
            )

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        is StampsScreenViewModel.Event.ProceedToStamp -> {
                            navController.navigate(
                                route = StampDestination(
                                    stampId = event.stampId,
                                ),
                            ) {
                                launchSingleTop = true
                            }
                        }

                        is StampsScreenViewModel.Event.ProceedToNewStamp -> {
                            proceedToNewStamp(
                                collectionId = event.collectionId,
                            )
                        }

                        is StampsScreenViewModel.Event.Done -> {
                            navController.navigateUp()
                        }
                    }
                }
            }

            BackHandler(
                onBack = viewModel::onBackAction,
            )
        }

        composable(
            route = StampDestination,
            arguments = listOf(
                navArgument(StampDestinationStampId) {
                    type = NavType.StringType
                },
            ),
        ) { navEntry ->
            val viewModel: StampScreenViewModel = koinViewModel {
                parametersOf(
                    StampScreenViewModel.Parameters(
                        stampId = navEntry.arguments?.getString(StampDestinationStampId)
                            ?: error("No ID argument passed"),
                    )
                )
            }
            val isCaptionInputEnabled by viewModel.isCaptionInputEnabled.collectAsState()

            StampScreen(
                stampId = viewModel.stampId,
                isEditable = viewModel.isEditable,
                captionState = viewModel.caption,
                isCaptionInputEnabled = isCaptionInputEnabled,
                onAddCaptionAction = viewModel::onAddCaptionAction,
                onDeleteAction = viewModel::onDeleteAction,
                imageUri = viewModel.imageUri,
                takenAt = viewModel.takenAt,
                onSwipedToExit = navController::navigateUp,
                sharedTransitionScope = this@StampsNavHost,
                animatedVisibilityScope = this@composable,
                modifier = Modifier
                    .fillMaxSize()
            )

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        is StampScreenViewModel.Event.Done -> {
                            navController.navigateUp()
                        }
                    }
                }
            }
        }
    }
}

private const val CollectionsDestination = "collections"
private const val CollectionDestinationCollectionId = "collectionId"
private const val CollectionDestinationFocusNameInput = "focusNameInput"
private const val CollectionDestination =
    "$CollectionsDestination/{$CollectionDestinationCollectionId}" +
            "?$CollectionDestinationFocusNameInput={$CollectionDestinationFocusNameInput}"

private fun CollectionDestination(
    collectionId: String,
    focusNameInput: Boolean,
) = "$CollectionsDestination/$collectionId" +
        "?$CollectionDestinationFocusNameInput=$focusNameInput"

private const val CollectionActionsDestination =
    "$CollectionsDestination/{$CollectionDestinationCollectionId}/actions"

private fun CollectionActionsDestination(
    collectionId: String,
) = "$CollectionsDestination/$collectionId/actions"

private const val MoveDestinationCollectionSelectionDestination =
    "$CollectionsDestination/{$CollectionDestinationCollectionId}/move-from"

private fun MoveDestinationCollectionSelectionDestination(
    currentCollectionId: String,
) = "$CollectionsDestination/$currentCollectionId/move-from"

private const val SelectedCollectionId = "selectedCollectionId"

private const val MoveStampsDestination =
    "$CollectionsDestination/{$CollectionDestinationCollectionId}/move?to={$SelectedCollectionId}"

fun MoveStampsDestination(
    sourceCollectionId: String,
    destinationCollectionId: String,
) =
    "$CollectionsDestination/$sourceCollectionId/move?to=$destinationCollectionId"

private const val StampsDestination = "stamps"
private const val StampDestinationStampId = "stampId"
private const val StampDestination = "$StampsDestination/{$StampDestinationStampId}"

private fun StampDestination(
    stampId: String,
) = "$StampsDestination/$stampId"
