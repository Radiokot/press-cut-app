package ua.com.radiokot.camerapp.intro.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.introDestination(
    onDone: () -> Unit,
) = composable(IntroRoute) {
    IntroScreen(
        onDone = onDone,
    )
}

const val IntroRoute = "intro"
