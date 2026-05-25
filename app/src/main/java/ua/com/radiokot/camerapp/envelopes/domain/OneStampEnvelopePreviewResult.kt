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

package ua.com.radiokot.camerapp.envelopes.domain

import android.net.Uri
import ua.com.radiokot.camerapp.stamps.domain.Stamp

sealed interface OneStampEnvelopePreviewResult {

    class Preview(
        val message: String?,
        /**
         * Some stamps with extracted image URIs, can be shown.
         */
        val previewStamps: List<Stamp>,
        val assetFileNamesById: Map<String, String>,
        /**
         * All the stamps in the envelope,
         * need image extraction before can be shown
         */
        val allStamps: List<Stamp>,
        val envelopeContentUri: Uri,
    ) : OneStampEnvelopePreviewResult

    sealed interface Error : OneStampEnvelopePreviewResult {
        class Malformed(
            val reason: String,
        ) : Error

        object NoSupportedStamps : Error
    }
}
