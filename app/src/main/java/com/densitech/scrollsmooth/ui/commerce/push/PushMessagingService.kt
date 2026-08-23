package com.densitech.scrollsmooth.ui.commerce.push

import com.densitech.scrollsmooth.ui.commerce.data.CommerceSync
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives pushes from FCM.
 *
 * The server sends both halves of the payload: `notification` so Android can
 * render something when the app is dead, and `data` so this class knows which
 * thread the message belongs to. When the app is in the foreground Android hands
 * the whole thing here instead of drawing it, which is why the notification is
 * built by hand below rather than left to the system.
 */
class PushMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Fires on install, on reinstall, and whenever Firebase rotates the
        // token on its own — which it does without asking. Registering here
        // rather than only at sign-in is what stops notifications quietly
        // stopping months later.
        PushTokens.onTokenRefreshed(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val conversationId = message.data["conversationId"]

        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(com.densitech.scrollsmooth.R.string.notification_new_message)
        val body = message.notification?.body ?: message.data["body"] ?: ""

        PushNotifications.show(this, title, body, conversationId)

        // Pull the thread in the background so opening the app from the
        // notification lands on the message rather than on a stale screen.
        if (conversationId != null) {
            CommerceSync.refreshConversation(conversationId)
        }
    }
}
