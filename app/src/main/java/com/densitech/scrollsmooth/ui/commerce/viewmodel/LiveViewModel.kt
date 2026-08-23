package com.densitech.scrollsmooth.ui.commerce.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.densitech.scrollsmooth.ui.commerce.data.LiveRepository
import com.densitech.scrollsmooth.ui.commerce.data.ProductRepository
import com.densitech.scrollsmooth.ui.commerce.model.LiveChatKind
import com.densitech.scrollsmooth.ui.commerce.model.LiveChatMessage
import com.densitech.scrollsmooth.ui.commerce.model.LiveStream
import com.densitech.scrollsmooth.ui.commerce.model.Product
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/**
 * Drives one live room at a time: the chat ticker, the viewer count and the flash sale clock.
 * The simulation only runs while a room is on screen, so leaving the room stops the work.
 */
@HiltViewModel
class LiveViewModel @Inject constructor() : ViewModel() {

    private val _streams = MutableStateFlow(LiveRepository.liveStreams())
    val streams = _streams.asStateFlow()

    private val _activeStream = MutableStateFlow<LiveStream?>(null)
    val activeStream = _activeStream.asStateFlow()

    private val _viewerCount = MutableStateFlow(0)
    val viewerCount = _viewerCount.asStateFlow()

    private val _chat = MutableStateFlow<List<LiveChatMessage>>(emptyList())
    val chat = _chat.asStateFlow()

    private val _secondsLeft = MutableStateFlow(0)
    val secondsLeft = _secondsLeft.asStateFlow()

    private val _pinnedProduct = MutableStateFlow<Product?>(null)
    val pinnedProduct = _pinnedProduct.asStateFlow()

    private val _openProduct = MutableStateFlow<Product?>(null)
    val openProduct = _openProduct.asStateFlow()

    private val _showProductList = MutableStateFlow(false)
    val showProductList = _showProductList.asStateFlow()

    /** How many units the room has "sold" this session; purely for the social proof counter. */
    private val _soldThisSession = MutableStateFlow(0)
    val soldThisSession = _soldThisSession.asStateFlow()

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
        _secondsLeft.value = stream.flashSaleSeconds
        _pinnedProduct.value = ProductRepository.product(stream.pinnedProductId)
        _soldThisSession.value = 0
        _chat.value = listOf(
            LiveChatMessage(
                id = chatSequence++,
                author = "SmoothScroll",
                emoji = "📣",
                text = "Welcome in. Purchases are protected by buyer protection.",
            )
        )
        startSimulation()
    }

    fun leaveRoom() {
        chatJob?.cancel()
        tickJob?.cancel()
        chatJob = null
        tickJob = null
        _openProduct.value = null
        _showProductList.value = false
    }

    fun productsInRoom(): List<Product> {
        val stream = _activeStream.value ?: return emptyList()
        return ProductRepository.productsByIds(stream.productIds)
    }

    fun seller(): Seller? = ProductRepository.seller(_activeStream.value?.sellerId)

    fun sellerOf(stream: LiveStream): Seller? = ProductRepository.seller(stream.sellerId)

    fun pinnedProductOf(stream: LiveStream): Product? = ProductRepository.product(stream.pinnedProductId)

    /** Price after the room's flash sale discount, while the clock is still running. */
    fun livePriceCents(product: Product): Long {
        val stream = _activeStream.value ?: return product.priceCents
        if (_secondsLeft.value <= 0) return product.priceCents
        return LiveRepository.livePriceCents(product.priceCents, stream.flashSaleDiscountPercent)
    }

    fun isFlashSaleRunning(): Boolean = _secondsLeft.value > 0

    fun openProduct(product: Product) {
        _openProduct.value = product
    }

    fun closeProduct() {
        _openProduct.value = null
    }

    fun showProductList() {
        _showProductList.value = true
    }

    fun hideProductList() {
        _showProductList.value = false
    }

    /** Called when the viewer actually buys, so the room reacts to them too. */
    fun recordPurchase(quantity: Int) {
        _soldThisSession.value += quantity
        pushChat(
            LiveChatMessage(
                id = chatSequence++,
                author = "you",
                emoji = "🛍",
                text = "bought $quantity",
                kind = LiveChatKind.PURCHASE,
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
                val message = LiveRepository.chatMessage(chatSequence++, random)
                pushChat(message)
                if (message.kind == LiveChatKind.PURCHASE) {
                    _soldThisSession.value += 1
                }
            }
        }

        tickJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                if (_secondsLeft.value > 0) _secondsLeft.value -= 1
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
