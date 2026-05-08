@file:Suppress("FunctionName")

package ua.com.radiokot.camerapp.stamps.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.skydoves.landscapist.image.LocalLandscapist
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject
import ua.com.radiokot.camerapp.cut.ui.NewStampActivity
import ua.com.radiokot.camerapp.intro.ui.PermissionsRoute
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
                    StampsScreenPreview(
                        modifier = Modifier
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
                PermissionsRoute
            else
                CollectionsRoute,
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
                    route = CollectionsRoute,
                ) {
                    popUpTo(PermissionsRoute) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        )

        collectionsDestination(
            sharedTransitionScope = this@StampsNavHost,
            onProceedToCollection = { collectionId: String, focusNameInput: Boolean ->
                navController.navigate(
                    route = StampsRoute(
                        collectionId = collectionId,
                        focusNameInput = focusNameInput,
                    )
                ) {
                    launchSingleTop = true
                }
            },
            onProceedToCollectionActions = { collectionId: String ->
                navController.navigate(
                    route = CollectionActionsRoute(
                        collectionId = collectionId,
                    )
                ) {
                    launchSingleTop = true
                }
            },
            onProceedToNewStamp = {
                proceedToNewStamp(
                    collectionId = null,
                )
            }
        )

        collectionActionsDestination(
            sharedTransitionScope = this@StampsNavHost,
            onProceedToMoveDestinationCollectionSelection = { currentCollectionId ->
                navController.navigate(
                    route = SelectMoveDestinationCollectionDestinationRoute(
                        sourceCollectionId = currentCollectionId,
                        isSingleStamp = false,
                    )
                ) {
                    launchSingleTop = true
                }
            },
            onProceedToMoveStamps = { sourceCollectionId, destinationCollectionId ->
                navController.navigate(
                    route = MoveStampsRoute(
                        sourceCollectionId = sourceCollectionId,
                        destinationCollectionId = destinationCollectionId,
                    )
                ) {
                    popUpTo(CollectionActionsRoute) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
            onDone = navController::navigateUp,
        )

        selectMoveDestinationCollectionDestination(
            onSelected = { collectionId ->
                navController
                    .previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(
                        key = SelectedMoveDestinationCollectionId,
                        value = collectionId,
                    )
                navController.navigateUp()
            },
            onCancel = navController::navigateUp,
        )

        moveStampsDestination(
            onDone = navController::navigateUp,
        )

        stampsDestination(
            sharedTransitionScope = this@StampsNavHost,
            onProceedToStamp = { stampId ->
                navController.navigate(
                    route = StampRoute(
                        stampId = stampId,
                    )
                ) {
                    launchSingleTop = true
                }
            },
            onProceedToNewStamp = { collectionId ->
                proceedToNewStamp(
                    collectionId = collectionId,
                )
            },
            onProceedToMoveDestinationCollectionSelection = { currentCollectionId ->
                navController.navigate(
                    route = SelectMoveDestinationCollectionDestinationRoute(
                        sourceCollectionId = currentCollectionId,
                        isSingleStamp = false,
                    )
                ) {
                    launchSingleTop = true
                }
            },
            onProceedToMoveStamps = { sourceCollectionId, destinationCollectionId, stampSelectionIndex ->
                navController.navigate(
                    route = MoveStampsRoute(
                        sourceCollectionId = sourceCollectionId,
                        destinationCollectionId = destinationCollectionId,
                        stampSelectionIndex = stampSelectionIndex,
                    )
                ) {
                    launchSingleTop = true
                }
            },
            onDone = navController::navigateUp,
        )

        stampDestination(
            sharedTransitionScope = this@StampsNavHost,
            onProceedToMoveDestinationCollectionSelection = { currentCollectionId ->
                navController.navigate(
                    route = SelectMoveDestinationCollectionDestinationRoute(
                        sourceCollectionId = currentCollectionId,
                        isSingleStamp = true,
                    )
                ) {
                    launchSingleTop = true
                }
            },
            onDone = navController::navigateUp,
        )
    }
}
