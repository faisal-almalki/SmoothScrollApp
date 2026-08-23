package com.densitech.scrollsmooth.ui.commerce.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shapes, matching the server's JSON exactly.
 *
 * These are kept separate from the domain models in `commerce/model` on purpose:
 * the API can add a field or rename one without the Compose screens noticing,
 * and the mapping in `ApiMappers.kt` is the single place that has to change.
 */

@Serializable
data class AuthTokensDto(
    val accountId: String,
    val accessToken: String,
    val refreshToken: String,
    val isNewAccount: Boolean = false,
)

@Serializable
data class OtpRequestDto(
    val phone: String,
    val expiresAt: String,
    /** Present outside production only, so the flow is testable without SMS. */
    val devCode: String? = null,
)

@Serializable
data class AccountDto(
    val id: String,
    val handle: String,
    val displayName: String,
    val bio: String = "",
    val emoji: String = "🙂",
    val city: String = "Riyadh",
    val publicPhone: String? = null,
    val allowCalls: Boolean = false,
    val allowMessages: Boolean = true,
    val isVerified: Boolean = false,
    val followerCount: Int = 0,
    val rating: Float = 5f,
    val ratingCount: Int = 0,
)

@Serializable
data class ListingDto(
    val id: String,
    val title: String,
    val description: String = "",
    val priceHalalas: Long,
    val isNegotiable: Boolean = true,
    val category: String,
    val condition: String = "USED",
    val emoji: String = "📦",
    val city: String,
    val status: String = "ACTIVE",
    val viewCount: Int = 0,
    val postedAt: String? = null,
    val sellerId: String? = null,
    val sellerHandle: String? = null,
    val sellerName: String? = null,
    val sellerEmoji: String? = null,
    val sellerCity: String? = null,
    val sellerIsVerified: Boolean = false,
    val sellerRating: Float = 5f,
    val sellerAllowCalls: Boolean = false,
    val sellerAllowMessages: Boolean = true,
    val sellerPhone: String? = null,
    val photos: List<PhotoDto> = emptyList(),
)

@Serializable
data class PhotoDto(
    val id: String,
    val url: String,
    val position: Int = 0,
)

@Serializable
data class ListingPageDto(
    val items: List<ListingDto> = emptyList(),
    val nextCursor: String? = null,
)

@Serializable
data class ConversationDto(
    val id: String,
    val listingId: String,
    val listingTitle: String = "",
    val listingEmoji: String = "📦",
    val listingPriceHalalas: Long = 0,
    val lastMessageAt: String? = null,
    val lastMessagePreview: String = "",
    val unread: Int = 0,
    val iAmSeller: Boolean = false,
    val otherId: String = "",
    val otherHandle: String = "",
    val otherName: String = "",
    val otherEmoji: String = "🙂",
)

@Serializable
data class ConversationPageDto(
    val items: List<ConversationDto> = emptyList(),
    val nextCursor: String? = null,
)

@Serializable
data class MessageDto(
    val id: String,
    val senderId: String,
    val body: String,
    val sentAt: String,
    val readAt: String? = null,
)

@Serializable
data class MessagePageDto(
    val items: List<MessageDto> = emptyList(),
    val nextCursor: String? = null,
)

@Serializable
data class OpenConversationDto(val id: String, val created: Boolean = false)

@Serializable
data class UnreadCountDto(val unread: Int = 0)

@Serializable
data class UploadUrlDto(
    val uploadUrl: String,
    val key: String,
    val expiresAt: String,
    val isPlaceholder: Boolean = false,
)

@Serializable
data class FeedVideoDto(
    val id: String,
    val caption: String = "",
    val playbackUrl: String? = null,
    val thumbnailUrl: String? = null,
    val durationMs: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val sellerId: String = "",
    val sellerHandle: String = "",
    val sellerName: String = "",
    val sellerEmoji: String = "🙂",
    val sellerCity: String = "",
    val sellerIsVerified: Boolean = false,
    @SerialName("listings") val listings: List<ListingDto> = emptyList(),
)

@Serializable
data class FeedPageDto(
    val items: List<FeedVideoDto> = emptyList(),
    val nextCursor: String? = null,
)

/* ---- request bodies ---- */

@Serializable
data class PhoneBody(val phone: String)

@Serializable
data class VerifyBody(val phone: String, val code: String, val city: String? = null)

@Serializable
data class RefreshBody(val refreshToken: String)

@Serializable
data class CreateListingBody(
    val title: String,
    val description: String? = null,
    val priceHalalas: Long,
    val isNegotiable: Boolean = true,
    val category: String,
    val condition: String = "USED",
    val emoji: String = "📦",
    val city: String,
)

@Serializable
data class SoldBody(val isSold: Boolean)

@Serializable
data class OpenConversationBody(val listingId: String)

@Serializable
data class SendMessageBody(val body: String)

@Serializable
data class UploadUrlBody(
    val contentType: String,
    val sizeBytes: Long,
    val filename: String? = null,
)

@Serializable
data class AttachPhotoBody(
    val key: String,
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
data class ProfileBody(
    val displayName: String? = null,
    val handle: String? = null,
    val bio: String? = null,
    val emoji: String? = null,
    val city: String? = null,
)

@Serializable
data class ContactPreferencesBody(
    val publicPhone: String? = null,
    val allowCalls: Boolean,
    val allowMessages: Boolean,
)

@Serializable
data class PushTokenBody(
    val token: String,
    val platform: String = "android",
)

/**
 * `deliveryEnabled` is false when the server itself has no FCM credentials, so
 * the app can say notifications are off rather than waiting for something that
 * is never coming.
 */
@Serializable
data class PushRegistrationDto(
    val registered: Boolean = true,
    val deliveryEnabled: Boolean = false,
)
