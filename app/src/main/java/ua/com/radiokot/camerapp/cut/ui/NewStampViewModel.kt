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

package ua.com.radiokot.camerapp.cut.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ua.com.radiokot.camerapp.cut.domain.StampCutCameraImageSource
import ua.com.radiokot.camerapp.cut.domain.StampCutImageSource
import ua.com.radiokot.camerapp.stamps.domain.StampCollection
import ua.com.radiokot.camerapp.util.lazyLogger

class NewStampViewModel : ViewModel() {

    private val log by lazyLogger("NewStampVM")

    private var isInitialized = false

    lateinit var collectionId: String
        private set
    var showToastOnSave: Boolean = true
        private set
    lateinit var imageSource: StampCutImageSource
        private set
    var stampImageBitmap: Bitmap? = null

    fun initOnce(
        collectionId: String?,
        showToastOnSave: Boolean,
        imageUri: Uri?,
    ) {
        if (isInitialized) {
            return
        }

        this.collectionId = collectionId ?: StampCollection.PRIMARY_ID
        this.showToastOnSave = showToastOnSave
        // TODO Create a bitmap source when image URI is present
        this.imageSource = StampCutCameraImageSource(
            coroutineScope = viewModelScope,
        )

        log.debug {
            "initOnce(): initialized:" +
                    "\ncollectionId=$collectionId" +
                    "\nshowToastOnSave=$showToastOnSave" +
                    "\nimageUri=$imageUri"
        }

        isInitialized = true
    }
}
