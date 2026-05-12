package ua.com.radiokot.camerapp.intro

import android.content.Context
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.camerapp.intro.data.OnboardingPreferencesOnPrefs
import ua.com.radiokot.camerapp.intro.domain.OnboardingPreferences
import ua.com.radiokot.camerapp.intro.ui.PermissionsScreenViewModel

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

        )
    }
}
