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

package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel

fun NavGraphBuilder.collectionsDestination(
    sharedTransitionScope: SharedTransitionScope?,
    onProceedToCollection: (
        collectionId: String,
        focusNameInput: Boolean,
    ) -> Unit,
    onProceedToCollectionActions: (collectionId: String) -> Unit,
    onProceedToNewStamp: () -> Unit,
    onProceedToAbout: () -> Unit,
) = composable(
    route = CollectionsRoute,
) {
    val viewModel: CollectionsScreenViewModel = koinViewModel()
    val items = viewModel.items.collectAsState()

    CollectionsScreen(
        itemsState = items,
        onItemClicked = viewModel::onItemClicked,
        onItemLongClicked = viewModel::onItemLongClicked,
        onNewStampAction = viewModel::onNewStampAction,
        onNewCollectionAction = viewModel::onNewCollectionAction,
        onMoreClicked = viewModel::onMoreClicked,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = this@composable,
        modifier = Modifier
            .fillMaxSize()
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CollectionsScreenViewModel.Event.ProceedToCollection -> {
                    onProceedToCollection(
                        event.collectionId,
                        event.focusNameInput,
                    )
                }

                is CollectionsScreenViewModel.Event.ProceedToCollectionActions -> {
                    onProceedToCollectionActions(
                        event.collectionId,
                    )
                }

                is CollectionsScreenViewModel.Event.ProceedToNewStamp -> {
                    onProceedToNewStamp()
                }

                is CollectionsScreenViewModel.Event.ProceedToAbout -> {
                    onProceedToAbout()
                }
            }
        }
    }
}

const val CollectionsRoute = "collections"
