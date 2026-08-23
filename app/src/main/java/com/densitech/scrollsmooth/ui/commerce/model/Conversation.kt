package com.densitech.scrollsmooth.ui.commerce.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: Long,
    val text: String,
    val fromMe: Boolean,
    val sentAtMillis: Long,
)

/**
 * A thread between the signed in user and one seller. A thread is anchored to the listing that
 * started it, so the seller knows which ad the question is about.
 */
@Serializable
data class Conversation(
    val id: String,
    val sellerId: String,
    val listingId: String,
    val listingTitle: String,
    val listingEmoji: String,
    val listingPriceCents: Long,
    val messages: List<ChatMessage> = emptyList(),
    val hasUnread: Boolean = false,
    val updatedAtMillis: Long = 0L,
) {
    val lastMessage: ChatMessage? get() = messages.lastOrNull()

    companion object {
        fun idFor(sellerId: String, listingId: String) = "$sellerId::$listingId"
    }
}
