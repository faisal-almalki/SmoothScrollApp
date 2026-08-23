package com.densitech.scrollsmooth.ui.commerce.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.densitech.scrollsmooth.ui.commerce.data.ListingRepository
import com.densitech.scrollsmooth.ui.commerce.data.LiveRepository
import com.densitech.scrollsmooth.ui.commerce.model.Listing
import com.densitech.scrollsmooth.ui.commerce.model.LiveChatKind
import com.densitech.scrollsmooth.ui.commerce.model.LiveChatMessage
import com.densitech.scrollsmooth.ui.commerce.model.LiveStream
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Drives one live room at a time: the chat ticker and the viewer count. The simulation only runs
 * while a room is on screen, so leaving the room stops the work.
 */
class LiveViewModel() : ViewModel() {

    private val _streams = MutableStateFlow(LiveRepository.liveStreams())
    val streams = _streams.asStateFlow()

    private val _activeStream = MutableStateFlow<LiveStream?>(null)
    val activeStream = _activeStream.asStateFlow()

    private val _viewerCount = MutableStateFlow(0)
    val viewerCount = _viewerCount.asStateFlow()

    private val _chat = MutableStateFlow<List<LiveChatMessage>>(emptyList())
    val chat = _chat.asStateFlow()

    private val _pinnedListing = MutableStateFlow<Listing?>(null)
    val pinnedListing = _pinnedListing.asStateFlow()

    private val _openListing = MutableStateFlow<Listing?>(null)
    val openListing = _openListing.asStateFlow()

    private val _showListingList = MutableStateFlow(false)
    val showListingList = _showListingList.asStateFlow()

    private var chatJob: Job? = null
    private var tickJob: Job? = null
    private var chatSequence = 0L
    private val random = Random(SIMULATION_SEED)

    fun enterRoom(streamId: String) {
        val stream = LiveRepository.liveStream(streamId) ?: return
        if (_activeStream.value?.id == stream.id && chatJob?.isActive == true) return

        leaveRoom()
        _activeStream.value = stream
        _viewerCount.value = stream.startingViewerCount
        _pinnedListing.value = ListingRepository.listing(stream.pinnedListingId)
        _chat.value = listOf(
            LiveChatMessage(
                id = chatSequence++,
                author = "SmoothScroll",
                emoji = "📣",
                text = "Deals happen off the app. Meet in a public place and check before you pay.",
            )
        )
        startSimulation()
    }

    fun leaveRoom() {
        chatJob?.cancel()
        tickJob?.cancel()
        chatJob = null
        tickJob = null
        _openListing.value = null
        _showListingList.value = false
    }

    fun listingsInRoom(): List<Listing> {
        val stream = _activeStream.value ?: return emptyList()
        return ListingRepository.listingsByIds(stream.listingIds)
    }

    fun seller(): Seller? = ListingRepository.seller(_activeStream.value?.sellerId)

    fun sellerOf(stream: LiveStream): Seller? = ListingRepository.seller(stream.sellerId)

    fun pinnedListingOf(stream: LiveStream): Listing? = ListingRepository.listing(stream.pinnedListingId)

    fun openListing(listing: Listing) {
        _openListing.value = listing
    }

    fun closeListing() {
        _openListing.value = null
    }

    fun showListingList() {
        _showListingList.value = true
    }

    fun hideListingList() {
        _showListingList.value = false
    }

    /** Called when the viewer contacts the seller, so the room reacts to them too. */
    fun recordContact() {
        pushChat(
            LiveChatMessage(
                id = chatSequence++,
                author = "you",
                emoji = "💬",
                text = "messaged the seller",
                kind = LiveChatKind.CONTACT,
            )
        )
    }

    fun sendChat(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        pushChat(
            LiveChatMessage(
                id = chatSequence++,
                author = "you",
                emoji = "🙋",
                text = trimmed,
            )
        )
    }

    private fun startSimulation() {
        chatJob = viewModelScope.launch {
            while (isActive) {
                delay(CHAT_INTERVAL_MIN_MS + random.nextLong(CHAT_INTERVAL_JITTER_MS))
                pushChat(LiveRepository.chatMessage(chatSequence++, random))
            }
        }

        tickJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                _viewerCount.value = LiveRepository.nextViewerCount(_viewerCount.value, random)
            }
        }
    }

    private fun pushChat(message: LiveChatMessage) {
        _chat.value = (_chat.value + message).takeLast(MAX_CHAT_LINES)
    }

    override fun onCleared() {
        super.onCleared()
        leaveRoom()
    }

    private companion object {
        const val MAX_CHAT_LINES = 40
        const val CHAT_INTERVAL_MIN_MS = 900L
        const val CHAT_INTERVAL_JITTER_MS = 1_400L
        const val SIMULATION_SEED = 7_412
    }
}
