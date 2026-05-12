package ua.com.radiokot.camerapp.intro.data

import android.content.SharedPreferences
import androidx.core.content.edit
import ua.com.radiokot.camerapp.intro.domain.OnboardingPreferences
import ua.com.radiokot.camerapp.util.lazyLogger

class OnboardingPreferencesOnPrefs(
    private val sharedPreferences: SharedPreferences,
) : OnboardingPreferences {

    private val log by lazyLogger("OnboardingPreferencesOnPrefs")

    override val isIntroSeen: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_INTRO_SEEN, false)

    override fun introSeen() {
        log.debug {
            "introSeen(): saving"
        }

        sharedPreferences.edit {
            putBoolean(KEY_IS_INTRO_SEEN, true)
        }
    }

    private companion object {
        private const val KEY_IS_INTRO_SEEN = "is_intro_seen"
    }
}
