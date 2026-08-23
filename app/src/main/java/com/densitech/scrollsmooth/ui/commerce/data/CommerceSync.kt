package com.densitech.scrollsmooth.ui.commerce.data

import com.densitech.scrollsmooth.ui.commerce.data.api.ApiClient
import com.densitech.scrollsmooth.ui.commerce.data.api.ApiException
import com.densitech.scrollsmooth.ui.commerce.data.api.TokenStore
import com.densitech.scrollsmooth.ui.commerce.data.api.toChatMessage
import com.densitech.scrollsmooth.ui.commerce.data.api.toConversation
import com.densitech.scrollsmooth.ui.commerce.data.api.toListing
import com.densitech.scrollsmooth.ui.commerce.data.api.toSeller
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Keeps the repositories fed from the API.
 *
 * The repositories stay synchronous and in-memory because every Compose screen
 * and view model already reads them that way. Rather than turning a hundred call
 * sites into suspend functions, this object pulls fresh data in the background
 * and pushes it into the same StateFlows the screens are already collecting.
 *
 * That also gives the app its offline behaviour for free: with no server the
 * seeded catalogue is what stays on screen, and everything still renders.
 */
object CommerceSync {

    private val errors = CoroutineExceptionHandler { _, error ->
        // A failed sync must never take the app down; the cached data is still
        // on screen and the next refresh will try again.
        android.util.Log.w("CommerceSync", "sync failed: ${error.message}")
    }

    private val scope = CoroutineScope(SupervisorJob() + errors)

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    /** True once a successful call has replaced the seeded data. */
    private val _hasRemoteData = MutableStateFlow(false)
    val hasRemoteData: StateFlow<Boolean> = _hasRemoteData.asStateFlow()

    fun refreshAll() {
        scope.launch {
            refreshListings()
            if (TokenStore.isSignedIn.value) {
                refreshProfile()
                refreshConversations()
            }
        }
    }

    suspend fun refreshListings() {
        runRemote {
            val page = ApiClient.browse(limit = 50)
            val listings = page.items.map { it.toListing() }
            val sellers = page.items.mapNotNull { it.toSeller() }
            if (listings.isNotEmpty()) {
                ListingRepository.applyRemoteListings(listings, sellers)
                _hasRemoteData.value = true
            }
        }
    }

    suspend fun refreshProfile() {
        runRemote { SellerRepository.applyRemoteProfile(ApiClient.me().toSeller()) }
    }

    suspend fun refreshConversations() {
        runRemote {
            val page = ApiClient.conversations(limit = 50)
            MessageRepository.applyRemoteConversations(page.items.map { it.toConversation() })
        }
    }

    fun refreshConversation(conversationId: String) {
        scope.launch {
            runRemote {
                val myId = TokenStore.accountId.value
                val page = ApiClient.messages(conversationId)
                MessageRepository.applyRemoteMessages(
                    conversationId,
                    page.items
                        .map { it.toChatMessage(myId) }
                        .sortedBy { it.sentAtMillis },
                )
            }
        }
    }

    /** Fire-and-forget writes: the local state already changed optimistically. */
    fun push(block: suspend () -> Unit) {
        scope.launch { runRemote { block() } }
    }

    private suspend fun runRemote(block: suspend () -> Unit) {
        try {
            block()
            _isOnline.value = true
            _lastError.value = null
        } catch (api: ApiException) {
            _isOnline.value = api.status != 0
            _lastError.value = api.message
        } catch (error: Exception) {
            _isOnline.value = false
            _lastError.value = "Could not reach the server."
        }
    }

    fun clearError() {
        _lastError.value = null
    }
}
