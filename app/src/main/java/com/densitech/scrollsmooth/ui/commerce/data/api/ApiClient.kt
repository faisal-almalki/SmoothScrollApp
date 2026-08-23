package com.densitech.scrollsmooth.ui.commerce.data.api

import com.densitech.scrollsmooth.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * The one place that talks to the API.
 *
 * OkHttp rather than Retrofit: the app already pulls OkHttp in through Media3's
 * datasource, the surface needed here is small, and kotlinx.serialization is
 * already a dependency — Retrofit would add a converter and a code generator to
 * do what forty lines do.
 */
object ApiClient {

    private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /** Serialises refreshes so a burst of 401s produces one refresh, not five. */
    private val refreshMutex = Mutex()

    var baseUrl: String = BuildConfig.API_BASE_URL
        private set

    fun overrideBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
    }

    /* ============================================================
       Auth
       ============================================================ */

    suspend fun requestOtp(phone: String): OtpRequestDto =
        post("/auth/otp/request", PhoneBody(phone), authenticated = false)

    suspend fun verifyOtp(phone: String, code: String, city: String? = null): AuthTokensDto {
        val tokens: AuthTokensDto =
            post("/auth/otp/verify", VerifyBody(phone, code, city), authenticated = false)
        TokenStore.save(tokens.accountId, tokens.accessToken, tokens.refreshToken)
        return tokens
    }

    suspend fun signOut() {
        val refresh = TokenStore.refreshToken
        if (refresh != null) {
            // Best effort: the local session is cleared either way, so a failure
            // here must not leave the person stuck on a screen they cannot pass.
            runCatching { postUnit("/auth/logout", RefreshBody(refresh), authenticated = false) }
        }
        TokenStore.clear()
    }

    suspend fun me(): AccountDto = get("/account")

    suspend fun updateProfile(body: ProfileBody): AccountDto = patch("/account", body)

    suspend fun updateContactPreferences(body: ContactPreferencesBody): AccountDto =
        patch("/account/contact", body)

    suspend fun deleteAccount() {
        deleteUnit("/account?confirm=true")
    }

    /* ============================================================
       Listings
       ============================================================ */

    suspend fun browse(
        query: String? = null,
        city: String? = null,
        category: String? = null,
        limit: Int = 30,
        cursor: String? = null,
    ): ListingPageDto {
        val url = baseUrl.toHttpUrl().newBuilder().addPathSegment("listings").apply {
            if (!query.isNullOrBlank()) addQueryParameter("q", query)
            if (!city.isNullOrBlank()) addQueryParameter("city", city)
            if (!category.isNullOrBlank()) addQueryParameter("category", category)
            addQueryParameter("limit", limit.toString())
            if (cursor != null) addQueryParameter("cursor", cursor)
        }.build()
        return request(Request.Builder().url(url).get())
    }

    suspend fun listing(id: String): ListingDto = get("/listings/$id")

    suspend fun createListing(body: CreateListingBody): ListingDto = post("/listings", body)

    suspend fun markSold(id: String, isSold: Boolean): ListingDto =
        post("/listings/$id/sold", SoldBody(isSold))

    suspend fun removeListing(id: String) {
        deleteUnit("/listings/$id")
    }

    suspend fun follow(sellerId: String) {
        postEmpty("/sellers/$sellerId/follow")
    }

    suspend fun unfollow(sellerId: String) {
        deleteUnit("/sellers/$sellerId/follow")
    }

    /* ============================================================
       Messaging
       ============================================================ */

    suspend fun conversations(limit: Int = 30, cursor: String? = null): ConversationPageDto {
        val url = baseUrl.toHttpUrl().newBuilder().addPathSegment("conversations").apply {
            addQueryParameter("limit", limit.toString())
            if (cursor != null) addQueryParameter("cursor", cursor)
        }.build()
        return request(Request.Builder().url(url).get())
    }

    suspend fun openConversation(listingId: String): OpenConversationDto =
        post("/conversations", OpenConversationBody(listingId))

    suspend fun messages(conversationId: String, limit: Int = 50, cursor: String? = null): MessagePageDto {
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegment("conversations").addPathSegment(conversationId).addPathSegment("messages")
            .addQueryParameter("limit", limit.toString())
            .apply { if (cursor != null) addQueryParameter("cursor", cursor) }
            .build()
        return request(Request.Builder().url(url).get())
    }

    suspend fun sendMessage(conversationId: String, body: String): MessageDto =
        post("/conversations/$conversationId/messages", SendMessageBody(body))

    suspend fun markConversationRead(conversationId: String) {
        postEmpty("/conversations/$conversationId/read")
    }

    suspend fun unreadCount(): UnreadCountDto = get("/conversations/unread-count")

    /* ============================================================
       Media and feed
       ============================================================ */

    suspend fun photoUploadUrl(listingId: String, body: UploadUrlBody): UploadUrlDto =
        post("/listings/$listingId/photos/upload-url", body)

    suspend fun attachPhoto(listingId: String, body: AttachPhotoBody): PhotoDto =
        post("/listings/$listingId/photos", body)

    /** Uploads the bytes straight to storage; this never goes through the API. */
    suspend fun uploadBytes(uploadUrl: String, contentType: String, bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(uploadUrl)
                .put(bytes.toRequestBody(contentType.toMediaType()))
                .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw ApiException(response.code, "upload_failed", "The photo could not be uploaded.")
                }
            }
        }
    }

    suspend fun feed(limit: Int = 10, cursor: String? = null): FeedPageDto {
        val url = baseUrl.toHttpUrl().newBuilder().addPathSegment("feed")
            .addQueryParameter("limit", limit.toString())
            .apply { if (cursor != null) addQueryParameter("cursor", cursor) }
            .build()
        return request(Request.Builder().url(url).get())
    }

    /* ============================================================
       Push notifications
       ============================================================ */

    suspend fun registerPushToken(token: String, platform: String = "android"): PushRegistrationDto =
        post("/account/push-tokens", PushTokenBody(token, platform))

    /**
     * The token travels in the body rather than the path: FCM tokens contain
     * characters that need escaping in a URL and are long enough that some
     * proxies truncate the path.
     */
    suspend fun unregisterPushToken(token: String) {
        deleteWithBody("/account/push-tokens", PushTokenBody(token))
    }

    /* ============================================================
       Plumbing
       ============================================================ */

    private suspend inline fun <reified T> get(path: String): T =
        request(Request.Builder().url(baseUrl + path).get())

    private suspend inline fun <reified B, reified T> post(
        path: String,
        body: B,
        authenticated: Boolean = true,
    ): T = request(
        Request.Builder().url(baseUrl + path).post(jsonBody(body)),
        authenticated,
    )

    private suspend inline fun <reified B, reified T> patch(path: String, body: B): T =
        request(Request.Builder().url(baseUrl + path).patch(jsonBody(body)))

    private suspend inline fun <reified B> postUnit(
        path: String,
        body: B,
        authenticated: Boolean = true,
    ) {
        requestRaw(Request.Builder().url(baseUrl + path).post(jsonBody(body)), authenticated)
    }

    /** For endpoints that take no body — an empty object keeps the parser happy. */
    private suspend fun postEmpty(path: String) {
        requestRaw(Request.Builder().url(baseUrl + path).post(emptyJsonBody()), true)
    }

    private suspend fun deleteUnit(path: String) {
        requestRaw(Request.Builder().url(baseUrl + path).delete(), true)
    }

    private suspend inline fun <reified B> deleteWithBody(path: String, body: B) {
        requestRaw(Request.Builder().url(baseUrl + path).delete(jsonBody(body)), true)
    }

    // Private so the public-inline restriction does not bite: these touch `json`,
    // which is private to this object.
    private inline fun <reified B> jsonBody(body: B) =
        json.encodeToString(body).toRequestBody(JSON_MEDIA_TYPE.toMediaType())

    private fun emptyJsonBody() = "{}".toRequestBody(JSON_MEDIA_TYPE.toMediaType())

    private suspend inline fun <reified T> request(
        builder: Request.Builder,
        authenticated: Boolean = true,
    ): T {
        val text = requestRaw(builder, authenticated)
        return runCatching { json.decodeFromString<T>(text) }
            .getOrElse { throw ApiException.unreadable(200) }
    }

    /**
     * Sends the request, and on a 401 refreshes once and retries. A second
     * failure means the refresh token is dead too, so the session is cleared
     * and the app falls back to the login screen.
     */
    suspend fun requestRaw(builder: Request.Builder, authenticated: Boolean): String =
        withContext(Dispatchers.IO) {
            var response = execute(builder, authenticated)

            if (response.code == 401 && authenticated) {
                response.close()
                if (refreshSession()) {
                    response = execute(builder, true)
                } else {
                    TokenStore.clear()
                    throw ApiException(401, "unauthorized", "Please sign in again.")
                }
            }

            response.use {
                val text = it.body?.string().orEmpty()
                if (it.isSuccessful) return@withContext text
                throw parseError(it.code, text)
            }
        }

    private fun execute(builder: Request.Builder, authenticated: Boolean): Response {
        val request = builder
            .apply {
                if (authenticated) {
                    TokenStore.accessToken?.let { header("Authorization", "Bearer $it") }
                }
            }
            .build()
        return try {
            http.newCall(request).execute()
        } catch (io: IOException) {
            throw ApiException.offline(io)
        }
    }

    private suspend fun refreshSession(): Boolean = refreshMutex.withLock {
        val refresh = TokenStore.refreshToken ?: return@withLock false
        try {
            val request = Request.Builder()
                .url("$baseUrl/auth/refresh")
                .post(jsonBody(RefreshBody(refresh)))
                .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withLock false
                val tokens = json.decodeFromString<AuthTokensDto>(response.body?.string().orEmpty())
                TokenStore.updateTokens(tokens.accessToken, tokens.refreshToken)
                true
            }
        } catch (error: Exception) {
            false
        }
    }

    private fun parseError(status: Int, text: String): ApiException {
        val parsed = runCatching { json.decodeFromString<ApiErrorBody>(text) }.getOrNull()
        return if (parsed != null) {
            ApiException(status, parsed.error.code, parsed.error.message)
        } else {
            ApiException.unreadable(status)
        }
    }
}
