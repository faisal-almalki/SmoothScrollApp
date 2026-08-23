package com.densitech.scrollsmooth.ui.commerce.push

import android.content.Context
import android.util.Log
import com.densitech.scrollsmooth.ui.commerce.data.api.ApiClient
import com.densitech.scrollsmooth.ui.commerce.data.api.TokenStore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Keeps the server's idea of this device's FCM token in step with Firebase's.
 *
 * Every path here degrades to doing nothing:
 *
 *  - no google-services.json, so no Firebase project → asking for a token
 *    throws, it is caught, and the app carries on with no notifications;
 *  - not signed in → there is no account to attach a token to, so nothing is
 *    sent until sign-in calls this again;
 *  - the server has no FCM credentials → it accepts the registration and
 *    reports deliveryEnabled=false.
 *
 * None of the three is an error worth showing anyone. A person who never sees a
 * notification has a slightly worse app, not a broken one.
 */
object PushTokens {

    private const val TAG = "PushTokens"

    private val errors = CoroutineExceptionHandler { _, error ->
        Log.w(TAG, "push registration failed: ${error.message}")
    }
    private val scope = CoroutineScope(SupervisorJob() + errors)

    /** The last token handed to us, so signing out knows what to unregister. */
    @Volatile
    private var currentToken: String? = null

    /**
     * Creates the notification channel and, if someone is signed in, registers
     * this device. Safe to call on every launch.
     */
    fun start(context: Context) {
        PushNotifications.ensureChannel(context)
        if (TokenStore.isSignedIn.value) register()
    }

    /** Call after a successful sign-in, once there is an account to attach to. */
    fun register() {
        scope.launch {
            val token = fetchToken() ?: return@launch
            currentToken = token
            sendToServer(token)
        }
    }

    fun onTokenRefreshed(token: String) {
        currentToken = token
        if (!TokenStore.isSignedIn.value) return
        scope.launch { sendToServer(token) }
    }

    /**
     * Called before the session is cleared, while the access token still works
     * — afterwards the request would be rejected and the device would keep
     * receiving the previous user's messages.
     */
    fun unregister() {
        val token = currentToken ?: return
        scope.launch {
            runCatching { ApiClient.unregisterPushToken(token) }
                .onFailure { Log.w(TAG, "unregister failed: ${it.message}") }
            currentToken = null
        }
    }

    /**
     * Bridges Firebase's Task to a coroutine by hand rather than pulling in
     * kotlinx-coroutines-play-services for one call. Tasks.await() would have
     * been shorter and blocks a thread, which is the wrong trade for something
     * that runs on every launch.
     *
     * getInstance() itself throws when Firebase has no configuration — the
     * normal state of a build with no google-services.json — so the try has to
     * wrap both halves.
     */
    private suspend fun fetchToken(): String? = try {
        suspendCancellableCoroutine<String?> { continuation ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    Log.i(TAG, "no FCM token: ${task.exception?.message}")
                    continuation.resume(null)
                }
            }
        }
    } catch (error: Exception) {
        Log.i(TAG, "notifications are off (${error.message})")
        null
    }

    private suspend fun sendToServer(token: String) {
        try {
            val result = ApiClient.registerPushToken(token)
            if (!result.deliveryEnabled) {
                Log.i(TAG, "registered, but the server has no FCM credentials yet")
            }
        } catch (error: Exception) {
            Log.w(TAG, "could not register the device: ${error.message}")
        }
    }
}
