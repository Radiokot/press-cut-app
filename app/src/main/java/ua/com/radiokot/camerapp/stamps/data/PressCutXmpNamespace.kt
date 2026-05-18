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

import com.ashampoo.xmp.XMPMeta
import com.ashampoo.xmp.XMPMetaFactory

object PressCutXmpNamespace {

    init {
        XMPMetaFactory.schemaRegistry.registerNamespace(
            namespaceURI = "https://github.com/Radiokot/press-cut-app",
            suggestedPrefix = PREFIX,
        )
    }

    fun XMPMeta.getStampShape(): String? =
        getPropertyString(
            schemaNS = NAMESPACE_URI,
            propName = SHAPE_PROPERTY,
        )

    fun XMPMeta.setStampShape(shapeName: String) =
        setProperty(
            schemaNS = NAMESPACE_URI,
            propName = SHAPE_PROPERTY,
            propValue = shapeName,
        )

    const val NAMESPACE_URI = "https://github.com/Radiokot/press-cut-app"
    const val PREFIX = "presscut"

    const val SHAPE_PROPERTY = "shape"
}
