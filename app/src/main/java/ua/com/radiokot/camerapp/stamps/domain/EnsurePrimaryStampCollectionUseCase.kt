package ua.com.radiokot.camerapp.stamps.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ua.com.radiokot.camerapp.intro.domain.OnboardingPreferences
import ua.com.radiokot.camerapp.util.lazyLogger
import kotlin.coroutines.cancellation.CancellationException

class EnsurePrimaryStampCollectionUseCase(
    private val collectionRepository: StampCollectionRepository,
    private val stampRepository: StampRepository,
    private val onboardingPreferences: OnboardingPreferences,
) {
    private val log by lazyLogger("EnsurePrimaryStampCollectionUC")
    private val leMutex = Mutex()

    suspend operator fun invoke() = leMutex.withLock {
        val primaryCollectionExists =
            collectionRepository
                .getStampCollectionsFlow()
                .first()
                .any(StampCollection::isPrimary)

        if (!primaryCollectionExists) {
            log.debug {
                "invoke(): creating the primary collection"
            }

            collectionRepository.addStampCollection(
                id = StampCollection.PRIMARY_ID,
                name = "My stamps",
            )

            try {
                log.debug {
                    "invoke(): putting gift stamps into it"
                }

                stampRepository.addGiftStamps(
                    collectionId = StampCollection.PRIMARY_ID,
                )
                onboardingPreferences.primaryCollectionGiftStampsMessageRequired()
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                }

                log.error(e) {
                    "invoke(): failed to put gift stamps into the primary collection"
                }
            }

            log.info {
                "Primary collection created, gift stamps given"
            }
        } else {
            log.debug {
                "invoke(): primary collection exists"
            }
        }
    }
}
