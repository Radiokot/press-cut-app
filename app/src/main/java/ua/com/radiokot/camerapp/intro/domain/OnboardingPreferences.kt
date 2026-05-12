package ua.com.radiokot.camerapp.intro.domain

interface OnboardingPreferences {

    val isIntroSeen: Boolean
    fun introSeen()

    val isPrimaryCollectionGiftStampsMessageRequired: Boolean
    fun primaryCollectionGiftStampsMessageRequired()
    fun primaryCollectionGiftStampsMessageSeen()
}
