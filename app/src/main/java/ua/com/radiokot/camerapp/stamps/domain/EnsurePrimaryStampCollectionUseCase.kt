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

package ua.com.radiokot.camerapp.stamps.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ua.com.radiokot.camerapp.util.lazyLogger
import kotlin.coroutines.cancellation.CancellationException

class EnsurePrimaryStampCollectionUseCase(
    private val collectionRepository: StampCollectionRepository,
    private val addGiftStampsToPrimaryCollectionUseCase: AddGiftStampsToPrimaryCollectionUseCase,
) {
    private val log by lazyLogger("EnsurePrimaryStampCollectionUC")
    private val leMutex = Mutex()

    suspend operator fun invoke() = leMutex.withLock {
        val primaryCollectionExists =
            collectionRepository
                .getStampCollectionsFlow()
                .first()
                .any(StampCollection::isPrimary)

        if (!primaryCollectionExists) {
            log.debug {
                "invoke(): creating the primary collection"
            }

            collectionRepository.addStampCollection(
                id = StampCollection.PRIMARY_ID,
                name = "My stamps",
            )

            try {
                addGiftStampsToPrimaryCollectionUseCase()
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                }

                log.error(e) {
                    "invoke(): failed to put gift stamps into the primary collection"
                }
            }

            log.info {
                "Primary collection created, gift stamps given"
            }
        } else {
            log.debug {
                "invoke(): primary collection exists"
            }
        }
    }
}
