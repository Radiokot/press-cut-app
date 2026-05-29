package ua.com.radiokot.camerapp.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import kotlinx.coroutines.flow.SharedFlow
import ua.com.radiokot.camerapp.util.ShareSheetShareEventReceiver.Companion.newIntentSender
import ua.com.radiokot.camerapp.util.ShareSheetShareEventReceiver.Companion.shareEvents
import kotlin.random.Random

/**
 * A [BroadcastReceiver] that allows subscribing to successful share events
 * of the system share sheet ([Intent.createChooser]).
 * An event is emitted whenever an app is open from the sheet.
 *
 * @see newIntentSender
 * @see shareEvents
 */
class ShareSheetShareEventReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        shareEvents.tryEmit(Unit)
    }

    companion object {
        /**
         * Emits whenever an app is opened from the share sheet.
         */
        val shareEvents: SharedFlow<Unit>
            field = eventSharedFlow()

        /**
         * @return an [IntentSender] to be passed to [Intent.createChooser]
         */
        fun newIntentSender(
            context: Context,
        ): IntentSender =
            PendingIntent
                .getBroadcast(
                    context,
                    Random.nextInt() and 0xffff,
                    Intent(context, ShareSheetShareEventReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                .intentSender
    }
}
