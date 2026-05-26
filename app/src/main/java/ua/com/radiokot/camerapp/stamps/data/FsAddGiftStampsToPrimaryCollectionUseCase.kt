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

package ua.com.radiokot.camerapp.stamps.data

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.com.radiokot.camerapp.intro.domain.OnboardingPreferences
import ua.com.radiokot.camerapp.stamps.domain.AddGiftStampsToPrimaryCollectionUseCase
import ua.com.radiokot.camerapp.stamps.domain.StampCollection
import ua.com.radiokot.camerapp.util.lazyLogger

class FsAddGiftStampsToPrimaryCollectionUseCase(
    private val stampRepository: FsStampRepository,
    private val assetManager: AssetManager,
    private val giftStampsAssetsDirectoryName: String,
    private val onboardingPreferences: OnboardingPreferences,
) : AddGiftStampsToPrimaryCollectionUseCase {

    private val log by lazyLogger("AddGiftStampsToPrimaryCollectionUC")

    override suspend operator fun invoke(): Unit = withContext(Dispatchers.IO) {

        log.debug {
            "invoke(): adding gift stamps:" +
                    "\ngiftStampsAssetsDirectoryName=$giftStampsAssetsDirectoryName"
        }

        assetManager
            .list(giftStampsAssetsDirectoryName)
            ?.forEach { giftStampFileName ->
                assetManager
                    .open("$giftStampsAssetsDirectoryName/$giftStampFileName")
                    .use { giftStampFileInputStream ->
                        stampRepository.addStamp(
                            collectionId = StampCollection.PRIMARY_ID,
                            stampWebpName = giftStampFileName,
                            stampWebpContent = giftStampFileInputStream,
                        )
                    }
            }

        onboardingPreferences.primaryCollectionGiftStampsMessageRequired()

        log.info {
            "Gift stamps were added to the primary collection"
        }
    }
}
