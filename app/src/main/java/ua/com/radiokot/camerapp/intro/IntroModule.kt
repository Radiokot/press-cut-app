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

package ua.com.radiokot.camerapp.intro

import android.content.Context
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.camerapp.intro.data.OnboardingPreferencesOnPrefs
import ua.com.radiokot.camerapp.intro.domain.OnboardingPreferences
import ua.com.radiokot.camerapp.intro.ui.PermissionsScreenViewModel
import ua.com.radiokot.camerapp.stamps.DIRECTORY_STAMPS

val introModule = module {

    single {
        OnboardingPreferencesOnPrefs(
            sharedPreferences = androidApplication().getSharedPreferences(
                "onboarding",
                Context.MODE_PRIVATE,
            ),
        )
    } bind OnboardingPreferences::class

    viewModel {
        PermissionsScreenViewModel(
            stampDirectory = get(named(DIRECTORY_STAMPS)),
            stampDirectoryDocumentUri = get(named(DIRECTORY_STAMPS)),
            application = androidApplication(),
        )
    }
}
