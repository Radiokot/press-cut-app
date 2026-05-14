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

@file:OptIn(ExperimentalCoroutinesApi::class)

package ua.com.radiokot.camerapp.stamps.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class GetStampCollectionsWithSamplesUseCase(
    private val stampRepository: StampRepository,
    private val getSortedStampCollectionsUseCase: GetSortedStampCollectionsUseCase,
) {
    operator fun invoke(): Flow<List<StampCollectionWithSamples>> =
        getSortedStampCollectionsUseCase()
            .flatMapLatest { collections ->
                stampRepository
                    .getStampsFlow()
                    .map { allStamps ->
                        allStamps
                            .groupBy(Stamp::collectionId)
                            .mapValues { (_, collectionStamps) ->
                                collectionStamps
                                    .sortedByDescending(Stamp::takenAtLocal)
                                    .take(3)
                            }
                    }
                    .distinctUntilChanged()
                    .map { samplesByCollectionId ->
                        collections.map { collection ->
                            StampCollectionWithSamples(
                                collection = collection,
                                samples = samplesByCollectionId[collection.id] ?: emptyList(),
                            )
                        }
                    }
            }

    suspend operator fun invoke(
        singleCollectionId: String,
    ): StampCollectionWithSamples? =
        invoke()
            .first()
            .find { it.collection.id == singleCollectionId }
}
