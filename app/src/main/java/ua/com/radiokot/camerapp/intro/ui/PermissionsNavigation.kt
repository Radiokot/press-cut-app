package ua.com.radiokot.camerapp.intro.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel

fun NavGraphBuilder.permissionsDestination(
    modifier: Modifier = Modifier,
    onDone: () -> Unit,
) = composable(
    route = PermissionsRoute,
) {
    val viewModel: PermissionsScreenViewModel = koinViewModel()

    PermissionsScreen(
        permissions = viewModel.requiredPermissions,
        onAllPermissionsGranted = viewModel::onAllPermissionsGranted,
        modifier = modifier
            .fillMaxSize()
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PermissionsScreenViewModel.Event.Done -> {
                    onDone()
                }
            }
        }
    }
}

const val PermissionsRoute = "permissions"
