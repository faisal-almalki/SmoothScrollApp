package com.densitech.scrollsmooth.ui.commerce.live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.commerce.model.LiveChatKind
import com.densitech.scrollsmooth.ui.commerce.model.LiveChatMessage
import com.densitech.scrollsmooth.ui.commerce.model.Product
import com.densitech.scrollsmooth.ui.commerce.model.formatCompact
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.LiveBadge
import com.densitech.scrollsmooth.ui.commerce.view.ProductDetailSheet
import com.densitech.scrollsmooth.ui.commerce.view.ProductImage
import com.densitech.scrollsmooth.ui.commerce.view.ProductListSheet
import com.densitech.scrollsmooth.ui.commerce.viewmodel.CartViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.LiveViewModel
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple
import com.densitech.scrollsmooth.ui.video.PlayerSurface
import com.densitech.scrollsmooth.ui.video.SURFACE_TYPE_SURFACE_VIEW

/**
 * A live selling room: the stream fills the screen, the chat runs up the left, and the products
 * sit in a pinned card the seller can push. Buying never leaves the room.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun LiveRoomScreen(
    streamId: String,
    liveViewModel: LiveViewModel,
    cartViewModel: CartViewModel,
    onClose: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenCheckout: () -> Unit,
    onOpenSeller: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val stream by liveViewModel.activeStream.collectAsState()
    val viewerCount by liveViewModel.viewerCount.collectAsState()
    val chat by liveViewModel.chat.collectAsState()
    val secondsLeft by liveViewModel.secondsLeft.collectAsState()
    val pinnedProduct by liveViewModel.pinnedProduct.collectAsState()
    val openProduct by liveViewModel.openProduct.collectAsState()
    val showProductList by liveViewModel.showProductList.collectAsState()
    val soldThisSession by liveViewModel.soldThisSession.collectAsState()
    val cart by cartViewModel.cart.collectAsState()

    // The room owns its player: entering starts it, leaving releases it.
    val exoPlayer = remember(streamId) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            playWhenReady = true
        }
    }

    DisposableEffect(streamId) {
        liveViewModel.enterRoom(streamId)
        onDispose {
            liveViewModel.leaveRoom()
            exoPlayer.release()
        }
    }

    LaunchedEffect(stream?.videoUrl) {
        val url = stream?.videoUrl ?: return@LaunchedEffect
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    val seller = liveViewModel.seller()
    val roomProducts = liveViewModel.productsInRoom()

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        PlayerSurface(
            player = exoPlayer,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = Modifier.fillMaxSize(),
        )

        // Top and bottom scrims so the overlays stay legible over any frame.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.28f to Color.Transparent,
                        0.62f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.75f),
                    )
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            LiveRoomHeader(
                sellerName = seller?.displayName.orEmpty(),
                sellerEmoji = seller?.emoji ?: "🛍",
                sellerHandle = seller?.handle.orEmpty(),
                viewerCount = viewerCount,
                soldThisSession = soldThisSession,
                onSellerClick = { seller?.id?.let(onOpenSeller) },
                onClose = onClose,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )

            stream?.let {
                Text(
                    text = it.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            LiveChatOverlay(
                messages = chat,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(210.dp)
                    .padding(start = 12.dp),
            )

            pinnedProduct?.let { product ->
                PinnedProductCard(
                    product = product,
                    livePriceCents = liveViewModel.livePriceCents(product),
                    secondsLeft = secondsLeft,
                    isSaleRunning = liveViewModel.isFlashSaleRunning(),
                    onBuyClick = { liveViewModel.openProduct(product) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }

            LiveRoomBottomBar(
                productCount = roomProducts.size,
                cartItemCount = cart.itemCount,
                onProductsClick = liveViewModel::showProductList,
                onCartClick = onOpenCart,
                onSendChat = liveViewModel::sendChat,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 10.dp)
                    .imePadding(),
            )
        }
    }

    // Buying inside the room: the live price applies while the sale clock is running.
    openProduct?.let { product ->
        val livePrice = liveViewModel.livePriceCents(product)
        val saleRunning = liveViewModel.isFlashSaleRunning()
        ProductDetailSheet(
            product = product,
            livePriceCents = if (saleRunning) livePrice else null,
            liveBadge = if (saleRunning) "LIVE PRICE" else null,
            onDismiss = liveViewModel::closeProduct,
            onAddToCart = { selection ->
                cartViewModel.addToCart(selection, liveStreamId = streamId)
                liveViewModel.recordPurchase(selection.quantity)
                liveViewModel.closeProduct()
            },
            onBuyNow = { selection ->
                cartViewModel.addToCart(selection, liveStreamId = streamId)
                liveViewModel.recordPurchase(selection.quantity)
                liveViewModel.closeProduct()
                onOpenCheckout()
            },
            onOpenSeller = { sellerId ->
                liveViewModel.closeProduct()
                onOpenSeller(sellerId)
            },
        )
    }

    if (showProductList) {
        ProductListSheet(
            title = "In this live",
            subtitle = if (liveViewModel.isFlashSaleRunning()) {
                "Live prices apply while the sale clock is running"
            } else {
                null
            },
            products = roomProducts,
            onDismiss = liveViewModel::hideProductList,
            onProductClick = { product ->
                liveViewModel.hideProductList()
                liveViewModel.openProduct(product)
            },
        )
    }
}

@Composable
private fun LiveRoomHeader(
    sellerName: String,
    sellerEmoji: String,
    sellerHandle: String,
    viewerCount: Int,
    soldThisSession: Int,
    onSellerClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(CommerceDimens.PillCorner))
                .background(CommerceColors.Scrim)
                .clickableNoRipple { onSellerClick() }
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, CommerceColors.Live, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = sellerEmoji, fontSize = 15.sp)
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sellerName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "@$sellerHandle",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            LiveBadge()
            Spacer(Modifier.width(6.dp))
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "👁 ${viewerCount.formatCompact()}",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CommerceColors.Scrim)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            )
            if (soldThisSession > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "🔥 $soldThisSession sold",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CommerceColors.Accent)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(CommerceColors.Scrim)
                .clickableNoRipple { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Leave live",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * The pinned product the seller is currently talking about, with the flash sale clock. This is
 * the highest intent surface in the room, so it gets the accent button.
 */
@Composable
private fun PinnedProductCard(
    product: Product,
    livePriceCents: Long,
    secondsLeft: Int,
    isSaleRunning: Boolean,
    onBuyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(Color.White.copy(alpha = 0.94f))
            .clickableNoRipple { onBuyClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductImage(
            seed = product.id,
            emoji = product.emoji,
            imageUrl = product.imageUrl,
            emojiSize = 26,
            corner = 10.dp,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.title,
                color = Color(0xFF111114),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = livePriceCents.formatMoney(),
                    color = CommerceColors.Accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (isSaleRunning && livePriceCents < product.priceCents) {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = product.priceCents.formatMoney(),
                        color = Color(0xFF7B7B85),
                        fontSize = 11.sp,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                    )
                }
            }
            if (isSaleRunning) {
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bolt_24),
                        contentDescription = null,
                        tint = CommerceColors.Discount,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "Live price ends in ${formatCountdown(secondsLeft)}",
                        color = CommerceColors.Discount,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Buy",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(CommerceDimens.PillCorner))
                .background(CommerceColors.Accent)
                .clickableNoRipple { onBuyClick() }
                .padding(horizontal = 18.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun LiveRoomBottomBar(
    productCount: Int,
    cartItemCount: Int,
    onProductsClick: () -> Unit,
    onCartClick: () -> Unit,
    onSendChat: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(CommerceDimens.PillCorner))
                .background(Color.White.copy(alpha = 0.14f))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.25f),
                    RoundedCornerShape(CommerceDimens.PillCorner),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (draft.isEmpty()) {
                    Text(
                        text = "Say something…",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (draft.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier
                        .size(18.dp)
                        .clickableNoRipple {
                            onSendChat(draft)
                            draft = ""
                        },
                )
            }
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(CommerceColors.Accent)
                .clickableNoRipple { onProductsClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_shop_bag_24),
                contentDescription = "Products in this live",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            if (productCount > 0) {
                Text(
                    text = productCount.toString(),
                    color = CommerceColors.Accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(horizontal = 4.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
                .clickableNoRipple { onCartClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_cart_24),
                contentDescription = "Cart",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            if (cartItemCount > 0) {
                Text(
                    text = if (cartItemCount > 99) "99+" else cartItemCount.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(CommerceColors.Accent)
                        .padding(horizontal = 4.dp),
                )
            }
        }
    }
}

/**
 * Chat runs bottom aligned and auto scrolls, the way a live chat does. Purchases and joins are
 * styled differently from ordinary messages so the social proof reads at a glance.
 */
@Composable
fun LiveChatOverlay(
    messages: List<LiveChatMessage>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
    ) {
        items(messages, key = { it.id }) { message ->
            LiveChatRow(message = message)
        }
    }
}

@Composable
private fun LiveChatRow(message: LiveChatMessage, modifier: Modifier = Modifier) {
    val background = when (message.kind) {
        LiveChatKind.PURCHASE -> CommerceColors.Accent.copy(alpha = 0.85f)
        LiveChatKind.JOIN -> Color.White.copy(alpha = 0.10f)
        LiveChatKind.CHAT -> CommerceColors.Scrim
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(CommerceDimens.PillCorner))
            .background(background)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = message.emoji, fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = message.author,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = when (message.kind) {
                LiveChatKind.PURCHASE -> "🛍 ${message.text}"
                LiveChatKind.JOIN -> message.text
                LiveChatKind.CHAT -> message.text
            },
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** m:ss for the flash sale clock. Reaching zero simply ends the live price. */
private fun formatCountdown(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}
