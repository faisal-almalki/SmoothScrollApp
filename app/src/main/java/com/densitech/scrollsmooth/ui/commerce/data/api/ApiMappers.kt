package com.densitech.scrollsmooth.ui.commerce.data.api

import com.densitech.scrollsmooth.ui.commerce.model.ChatMessage
import com.densitech.scrollsmooth.ui.commerce.model.Conversation
import com.densitech.scrollsmooth.ui.commerce.model.Listing
import com.densitech.scrollsmooth.ui.commerce.model.ListingCondition
import com.densitech.scrollsmooth.ui.commerce.model.Seller

/**
 * Wire shapes to domain models.
 *
 * The single place the two vocabularies meet, so the server can rename a field
 * without any Compose screen noticing.
 */

/** ISO-8601 to epoch millis, falling back to zero rather than throwing on junk. */
private fun parseInstant(value: String?): Long {
    if (value.isNullOrBlank()) return 0L
    return runCatching { java.time.Instant.parse(value).toEpochMilli() }.getOrDefault(0L)
}

fun ListingDto.toListing(): Listing = Listing(
    id = id,
    sellerId = sellerId.orEmpty(),
    title = title,
    description = description,
    priceCents = priceHalalas,
    isNegotiable = isNegotiable,
    category = category,
    emoji = emoji,
    city = city,
    condition = runCatching { ListingCondition.valueOf(condition) }.getOrDefault(ListingCondition.USED),
    postedAtMillis = parseInstant(postedAt),
    viewCount = viewCount,
    isSold = status == "SOLD",
    imageUrl = photos.minByOrNull { it.position }?.url,
)

/** The seller summary that rides along with a browse result. */
fun ListingDto.toSeller(): Seller? {
    val id = sellerId ?: return null
    return Seller(
        id = id,
        handle = sellerHandle.orEmpty(),
        displayName = sellerName.orEmpty(),
        bio = "",
        emoji = sellerEmoji ?: "🙂",
        rating = sellerRating,
        ratingCount = 0,
        followers = 0,
        isVerified = sellerIsVerified,
        city = sellerCity.orEmpty(),
        phoneNumber = sellerPhone.orEmpty(),
        // The server only sends a number when the seller published it, so its
        // presence is the permission — the client never decides this itself.
        allowCalls = sellerPhone != null,
        allowMessages = sellerAllowMessages,
    )
}

fun AccountDto.toSeller(): Seller = Seller(
    id = id,
    handle = handle,
    displayName = displayName,
    bio = bio,
    emoji = emoji,
    rating = rating,
    ratingCount = ratingCount,
    followers = followerCount,
    isVerified = isVerified,
    city = city,
    phoneNumber = publicPhone.orEmpty(),
    allowCalls = allowCalls,
    allowMessages = allowMessages,
)

fun ConversationDto.toConversation(messages: List<ChatMessage> = emptyList()): Conversation =
    Conversation(
        id = id,
        sellerId = if (iAmSeller) otherId else otherId,
        listingId = listingId,
        listingTitle = listingTitle,
        listingEmoji = listingEmoji,
        listingPriceCents = listingPriceHalalas,
        messages = messages,
        hasUnread = unread > 0,
        updatedAtMillis = parseInstant(lastMessageAt),
    )

fun MessageDto.toChatMessage(myAccountId: String?): ChatMessage = ChatMessage(
    // The domain model keys messages by Long; the server uses uuids, so the
    // hash is used for identity within a thread. Collisions only affect list
    // animation, never delivery.
    id = id.hashCode().toLong(),
    text = body,
    fromMe = senderId == myAccountId,
    sentAtMillis = parseInstant(sentAt),
)

fun FeedVideoDto.toListings(): List<Listing> = listings.map { it.toListing() }
