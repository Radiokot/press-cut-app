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

    override val isPrimaryCollectionGiftStampsMessageRequired: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_GIFT_STAMPS_MESSAGE_REQUIRED, false)

    override fun primaryCollectionGiftStampsMessageRequired() {
        log.debug {
            "giftStampsMessageRequired(): saving"
        }

        sharedPreferences.edit {
            putBoolean(KEY_IS_GIFT_STAMPS_MESSAGE_REQUIRED, true)
        }
    }

    override fun primaryCollectionGiftStampsMessageSeen() {
        log.debug {
            "giftStampsMessageSeen(): saving"
        }

        sharedPreferences.edit {
            putBoolean(KEY_IS_GIFT_STAMPS_MESSAGE_REQUIRED, false)
        }
    }

    private companion object {
        private const val KEY_IS_INTRO_SEEN = "is_intro_seen"
        private const val KEY_IS_GIFT_STAMPS_MESSAGE_REQUIRED = "is_gift_stamps_message_required"
    }
}
