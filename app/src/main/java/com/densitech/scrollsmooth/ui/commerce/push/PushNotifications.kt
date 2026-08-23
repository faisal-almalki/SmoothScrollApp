package com.densitech.scrollsmooth.ui.commerce.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.densitech.scrollsmooth.MainActivity
import com.densitech.scrollsmooth.R

/**
 * The one notification this app posts: someone replied about an ad.
 *
 * The channel id has to match what the server sets in the FCM payload
 * (`MESSAGES_CHANNEL_ID` in server/src/lib/push.ts). If the two ever drift,
 * Android drops the notification into a default channel with no sound rather
 * than failing loudly, which is a very quiet kind of bug.
 */
object PushNotifications {

    const val MESSAGES_CHANNEL_ID = "messages"

    /** Extras MainActivity reads to open the right thread from a tap. */
    const val EXTRA_CONVERSATION_ID = "conversationId"

    /**
     * Channels are created once and then owned by the user — importance and
     * sound cannot be changed from code afterwards, only in system settings.
     * Creating one that already exists is a no-op, so this is safe on every
     * launch.
     */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            MESSAGES_CHANNEL_ID,
            context.getString(R.string.notification_channel_messages),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_messages_description)
            enableVibration(true)
        }

        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    fun show(context: Context, title: String, body: String, conversationId: String?) {
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (conversationId != null) putExtra(EXTRA_CONVERSATION_ID, conversationId)
        }
        val pending = PendingIntent.getActivity(
            context,
            conversationId?.hashCode() ?: 0,
            intent,
            // IMMUTABLE is required from Android 12 and is the right default
            // regardless: nothing outside this app should be able to rewrite it.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, MESSAGES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_message)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        // One notification per conversation, so ten replies in one thread
        // replace each other instead of stacking into ten rows.
        val id = conversationId?.hashCode() ?: System.currentTimeMillis().toInt()

        // From Android 13 the user can refuse notifications outright. areNotificationsEnabled()
        // is checked rather than the permission, because a channel the user
        // turned off individually also has to be respected.
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            try {
                NotificationManagerCompat.from(context).notify(id, notification)
            } catch (security: SecurityException) {
                // POST_NOTIFICATIONS revoked between the check and the call.
                android.util.Log.w("PushNotifications", "not allowed to post", security)
            }
        }
    }
}
