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

package ua.com.radiokot.camerapp.envelopes.data

import android.content.Intent
import ua.com.radiokot.camerapp.envelopes.domain.CreateSendEnvelopeIntentUseCase

class CpCreateSendEnvelopeIntentUseCase : CreateSendEnvelopeIntentUseCase {

    override operator fun invoke(
        message: String?,
        stampIds: Set<String>,
    ): Intent {
        val uri =
            EnvelopeContentProvider
                .provideNewEnvelope(
                    message = message,
                    stampIds = stampIds,
                )

        var intentText = "Open this with PressCut on Android or OneStamp on iOS"
        if (message != null) {
            intentText = message + "\n\n" + intentText
        }

        return Intent(Intent.ACTION_SEND)
            .setDataAndType(uri, EnvelopeContentProvider.ENVELOPE_CONTENT_TYPE)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, intentText)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
