package com.densitech.scrollsmooth.ui.commerce.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.densitech.scrollsmooth.ui.commerce.data.ListingRepository
import com.densitech.scrollsmooth.ui.commerce.data.MessageRepository
import com.densitech.scrollsmooth.ui.commerce.model.Conversation
import com.densitech.scrollsmooth.ui.commerce.model.Listing
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The inbox and the individual threads. Contacting a seller is the only "conversion" in this app,
 * so this sits at activity scope alongside browsing.
 */
@HiltViewModel
class MessagesViewModel @Inject constructor() : ViewModel() {

    val conversations = MessageRepository.conversations

    /** Transient confirmation shown over whichever surface started the contact. */
    private val _banner = MutableStateFlow<String?>(null)
    val banner = _banner.asStateFlow()

    fun seller(id: String?): Seller? = ListingRepository.seller(id)

    fun conversation(id: String?): Conversation? = MessageRepository.conversation(id)

    fun listing(id: String?): Listing? = ListingRepository.listing(id)

    fun unreadCount(): Int = MessageRepository.unreadCount

    /** Opens the thread for a listing and returns its id, creating it the first time. */
    fun startConversation(listing: Listing, nowMillis: Long = System.currentTimeMillis()): String =
        MessageRepository.openConversation(listing, nowMillis)

    fun send(conversationId: String, text: String) {
        val now = System.currentTimeMillis()
        MessageRepository.send(conversationId, text, now)

        // Stand-in for a seller replying, so a fresh thread does not look dead.
        viewModelScope.launch {
            delay(AUTO_REPLY_DELAY_MS)
            MessageRepository.autoReply(conversationId, System.currentTimeMillis())
        }
    }

    fun markRead(conversationId: String) = MessageRepository.markRead(conversationId)

    fun delete(conversationId: String) = MessageRepository.delete(conversationId)

    fun showBanner(text: String) {
        _banner.value = text
    }

    fun dismissBanner() {
        _banner.value = null
    }

    private companion object {
        const val AUTO_REPLY_DELAY_MS = 2_200L
    }
}
