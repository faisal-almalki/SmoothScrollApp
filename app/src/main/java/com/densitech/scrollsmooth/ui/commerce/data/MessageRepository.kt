package com.densitech.scrollsmooth.ui.commerce.data

import com.densitech.scrollsmooth.ui.commerce.model.ChatMessage
import com.densitech.scrollsmooth.ui.commerce.model.Conversation
import com.densitech.scrollsmooth.ui.commerce.data.api.ApiClient
import com.densitech.scrollsmooth.ui.commerce.model.Listing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Direct messages between the signed in user and sellers.
 *
 * Nothing is delivered anywhere: there is no messaging backend in this build, so threads are held
 * here and mirrored to disk. [autoReply] fakes the other side answering once, which is what keeps
 * the inbox from looking broken while you try it out.
 */
object MessageRepository {

    private const val KEY_CONVERSATIONS = "conversations"

    private val _conversations = MutableStateFlow(
        CommerceStore.readOrNull<List<Conversation>>(KEY_CONVERSATIONS) ?: emptyList()
    )
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private var nextMessageId: Long =
        (_conversations.value.flatMap { it.messages }.maxOfOrNull { it.id } ?: 0L) + 1L

    val unreadCount: Int get() = _conversations.value.count { it.hasUnread }

    /**
     * Adopts the server's inbox. Local drafts are not a concern here — messages
     * are sent immediately — so the server's list simply wins, except that any
     * thread only known locally is kept so a just-opened conversation does not
     * vanish before the next refresh.
     */
    fun applyRemoteConversations(remote: List<Conversation>) {
        if (remote.isEmpty()) return
        val remoteIds = remote.map { it.id }.toSet()
        val localOnly = _conversations.value.filterNot { it.id in remoteIds }
        persist((remote + localOnly).sortedByDescending { it.updatedAtMillis })
    }

    /** Replaces one thread's messages with what the server has. */
    fun applyRemoteMessages(conversationId: String, messages: List<ChatMessage>) {
        val existing = conversation(conversationId) ?: return
        replace(existing.copy(messages = messages))
        nextMessageId = (messages.maxOfOrNull { it.id } ?: nextMessageId) + 1L
    }

    fun conversation(id: String?): Conversation? =
        id?.let { wanted -> _conversations.value.firstOrNull { it.id == wanted } }

    fun conversationFor(listing: Listing): Conversation? =
        conversation(Conversation.idFor(listing.sellerId, listing.id))

    /**
     * Opens (or reuses) the thread for a listing. Returns its id so the caller can navigate
     * straight into it.
     */
    fun openConversation(listing: Listing, nowMillis: Long): String {
        val id = Conversation.idFor(listing.sellerId, listing.id)
        if (conversation(id) == null) {
            val created = Conversation(
                id = id,
                sellerId = listing.sellerId,
                listingId = listing.id,
                listingTitle = listing.title,
                listingEmoji = listing.emoji,
                listingPriceCents = listing.priceCents,
                messages = emptyList(),
                hasUnread = false,
                updatedAtMillis = nowMillis,
            )
            persist(listOf(created) + _conversations.value)
        }
        CommerceSync.push {
            val opened = ApiClient.openConversation(listing.id)
            // The server owns thread identity; adopt its id so later sends and
            // reads address the same row.
            adoptServerId(localId = id, serverId = opened.id)
        }
        return id
    }

    /** Re-keys a locally created thread once the server tells us its real id. */
    private fun adoptServerId(localId: String, serverId: String) {
        if (localId == serverId) return
        val existing = conversation(localId) ?: return
        val others = _conversations.value.filterNot { it.id == localId || it.id == serverId }
        persist((listOf(existing.copy(id = serverId)) + others).sortedByDescending { it.updatedAtMillis })
    }

    fun send(conversationId: String, text: String, nowMillis: Long) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val existing = conversation(conversationId) ?: return

        val message = ChatMessage(
            id = nextMessageId++,
            text = trimmed,
            fromMe = true,
            sentAtMillis = nowMillis,
        )
        replace(existing.copy(
            messages = existing.messages + message,
            updatedAtMillis = nowMillis,
        ))
        CommerceSync.push { ApiClient.sendMessage(conversationId, trimmed) }
    }

    /**
     * A single canned acknowledgement from the seller, sent the first time you message a thread.
     * It is clearly a stand-in for a real backend, not a bot pretending to be the seller.
     */
    fun autoReply(conversationId: String, nowMillis: Long) {
        val existing = conversation(conversationId) ?: return
        if (existing.messages.any { !it.fromMe }) return

        val reply = ChatMessage(
            id = nextMessageId++,
            text = "Hi, thanks for your interest. The ad is still available. " +
                "(Demo build: there is no messaging server, so this reply is automatic.)",
            fromMe = false,
            sentAtMillis = nowMillis,
        )
        replace(existing.copy(
            messages = existing.messages + reply,
            hasUnread = true,
            updatedAtMillis = nowMillis,
        ))
    }

    fun markRead(conversationId: String) {
        val existing = conversation(conversationId) ?: return
        if (!existing.hasUnread) return
        replace(existing.copy(hasUnread = false))
        CommerceSync.push { ApiClient.markConversationRead(conversationId) }
    }

    fun delete(conversationId: String) {
        persist(_conversations.value.filterNot { it.id == conversationId })
    }

    private fun replace(updated: Conversation) {
        val others = _conversations.value.filterNot { it.id == updated.id }
        persist((listOf(updated) + others).sortedByDescending { it.updatedAtMillis })
    }

    private fun persist(updated: List<Conversation>) {
        _conversations.value = updated
        CommerceStore.writeValue(KEY_CONVERSATIONS, updated)
    }
}
