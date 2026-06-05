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
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject
import ua.com.radiokot.camerapp.about.ui.AboutRoute
import ua.com.radiokot.camerapp.about.ui.aboutDestination
import ua.com.radiokot.camerapp.collectionselection.ui.SelectDestinationCollectionContract
import ua.com.radiokot.camerapp.collectionselection.ui.selectDestinationCollectionDestination
import ua.com.radiokot.camerapp.cut.ui.NewStampActivity
import ua.com.radiokot.camerapp.envelopes.ui.SendEnvelopeRoute
import ua.com.radiokot.camerapp.envelopes.ui.sendEnvelopeDestination
import ua.com.radiokot.camerapp.intro.domain.OnboardingPreferences
import ua.com.radiokot.camerapp.intro.ui.IntroRoute
import ua.com.radiokot.camerapp.intro.ui.PermissionsRoute
import ua.com.radiokot.camerapp.intro.ui.PermissionsScreenViewModel
import ua.com.radiokot.camerapp.intro.ui.introDestination
import ua.com.radiokot.camerapp.intro.ui.permissionsDestination
import ua.com.radiokot.camerapp.ui.AppTheme
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.util.lazyLogger

class StampsActivity : ComponentActivity() {

    private val log by lazyLogger("StampsActivity")

    private val permissionsScreenViewModel: PermissionsScreenViewModel by viewModel()
    private val onboardingPreferences: OnboardingPreferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(0, 0),
        )
        super.onCreate(savedInstanceState)

        val isPermissionActionRequired = permissionsScreenViewModel.isActionRequired
        val isIntroSeen = onboardingPreferences.isIntroSeen

        log.debug {
            "onCreate(): preconditions checked:" +
                    "\nisPermissionActionRequired=$isPermissionActionRequired" +
                    "\nisIntroSeen=$isIntroSeen"
        }

        if (isPermissionActionRequired) {
            log.info {
                "Permission action is required"
            }
        }

        if (!isIntroSeen) {
            log.info {
                "Intro must be seen"
            }
        }

        setContent {
            AppTheme(
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
                    StampsScreenDummy(
                        modifier = Modifier
                            .alpha(0.01f)
                    )

                    LaunchedEffect(Unit) {
                        isStampsScreenWarmupShown = false
                    }
                }

                SharedTransitionLayout {
                    StampsNavHost(
                        arePermissionsNeeded = isPermissionActionRequired,
                        isIntroNeeded = !isIntroSeen,
                        onIntroSeen = remember { onboardingPreferences::introSeen },
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
    arePermissionsNeeded: Boolean,
    isIntroNeeded: Boolean,
    onIntroSeen: () -> Unit,
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
    val selectDestinationCollectionContract = SelectDestinationCollectionContract(
        navController = navController,
    )
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
            if (isIntroNeeded)
                IntroRoute
            else if (arePermissionsNeeded)
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
        introDestination(
            onDone = {
                onIntroSeen()

                navController.navigate(
                    route =
                        if (arePermissionsNeeded)
                            PermissionsRoute
                        else
                            CollectionsRoute,
                ) {
                    popUpTo(IntroRoute) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        )

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
            },
            onProceedToAbout = {
                navController.navigate(
                    route = AboutRoute,
                ) {
                    launchSingleTop = true
                }
            }
        )

        collectionActionsDestination(
            sharedTransitionScope = this@StampsNavHost,
            selectDestinationCollectionContract = selectDestinationCollectionContract,
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

        selectDestinationCollectionDestination(
            contract = selectDestinationCollectionContract,
        )

        moveStampsDestination(
            onDone = navController::navigateUp,
        )

        stampsDestination(
            sharedTransitionScope = this@StampsNavHost,
            selectDestinationCollectionContract = selectDestinationCollectionContract,
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
            onProceedToSendEnvelope = {
                navController.navigate(
                    route = SendEnvelopeRoute(
                        stampSelectionIndex = it,
                    )
                ) {
                    launchSingleTop = true
                }
            },
            onDone = navController::navigateUp,
        )

        sendEnvelopeDestination(
            onDone = navController::navigateUp,
        )

        stampDestination(
            sharedTransitionScope = this@StampsNavHost,
            selectDestinationCollectionContract = selectDestinationCollectionContract,
            onDone = navController::navigateUp,
        )

        aboutDestination()
    }
}
