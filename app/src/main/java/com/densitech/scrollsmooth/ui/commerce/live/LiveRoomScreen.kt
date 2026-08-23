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
import com.densitech.scrollsmooth.ui.commerce.model.Listing
import com.densitech.scrollsmooth.ui.commerce.model.LiveChatKind
import com.densitech.scrollsmooth.ui.commerce.model.LiveChatMessage
import com.densitech.scrollsmooth.ui.commerce.model.formatCompact
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.ListingDetailSheet
import com.densitech.scrollsmooth.ui.commerce.view.ListingImage
import com.densitech.scrollsmooth.ui.commerce.view.ListingListSheet
import com.densitech.scrollsmooth.ui.commerce.view.LiveBadge
import com.densitech.scrollsmooth.ui.commerce.view.dialSeller
import com.densitech.scrollsmooth.ui.commerce.viewmodel.LiveViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.MessagesViewModel
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple
import com.densitech.scrollsmooth.ui.video.PlayerSurface
import com.densitech.scrollsmooth.ui.video.SURFACE_TYPE_SURFACE_VIEW

/**
 * A live room: the stream fills the screen, chat runs up the left, and the ad the seller is
 * currently showing sits pinned above the composer. Contacting the seller never leaves the room.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun LiveRoomScreen(
    streamId: String,
    liveViewModel: LiveViewModel,
    messagesViewModel: MessagesViewModel,
    onClose: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenSeller: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val stream by liveViewModel.activeStream.collectAsState()
    val viewerCount by liveViewModel.viewerCount.collectAsState()
    val chat by liveViewModel.chat.collectAsState()
    val pinnedListing by liveViewModel.pinnedListing.collectAsState()
    val openListing by liveViewModel.openListing.collectAsState()
    val showListingList by liveViewModel.showListingList.collectAsState()
    val conversations by messagesViewModel.conversations.collectAsState()

    val unread = conversations.count { it.hasUnread }
    val nowMillis = remember(stream?.id) { System.currentTimeMillis() }

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
    val roomListings = liveViewModel.listingsInRoom()
    val callableSeller = seller?.takeIf { it.hasPublicPhone }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        PlayerSurface(
            player = exoPlayer,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = Modifier.fillMaxSize(),
        )

        // Scrims so the overlays stay legible over any frame.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.28f to Color.Transparent,
                        0.60f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.78f),
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
                sellerEmoji = seller?.emoji ?: "📦",
                sellerHandle = seller?.handle.orEmpty(),
                viewerCount = viewerCount,
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
                    .height(200.dp)
                    .padding(start = 12.dp),
            )

            pinnedListing?.let { listing ->
                PinnedListingCard(
                    listing = listing,
                    onViewClick = { liveViewModel.openListing(listing) },
                    onCallClick = callableSeller?.let { callable ->
                        { dialSeller(context, callable.phoneNumber) }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }

            LiveRoomBottomBar(
                listingCount = roomListings.size,
                unreadCount = unread,
                onListingsClick = liveViewModel::showListingList,
                onMessagesClick = {
                    val listing = pinnedListing
                    if (listing != null) {
                        liveViewModel.recordContact()
                        onOpenConversation(messagesViewModel.startConversation(listing))
                    }
                },
                onSendChat = liveViewModel::sendChat,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 10.dp)
                    .imePadding(),
            )
        }
    }

    openListing?.let { listing ->
        ListingDetailSheet(
            listing = listing,
            nowMillis = nowMillis,
            onDismiss = liveViewModel::closeListing,
            onMessageSeller = { target ->
                liveViewModel.recordContact()
                liveViewModel.closeListing()
                onOpenConversation(messagesViewModel.startConversation(target))
            },
            onCalledSeller = { liveViewModel.recordContact() },
            onOpenSeller = { sellerId ->
                liveViewModel.closeListing()
                onOpenSeller(sellerId)
            },
        )
    }

    if (showListingList) {
        ListingListSheet(
            title = "In this live",
            subtitle = "Ads this seller is showing right now",
            listings = roomListings,
            nowMillis = nowMillis,
            onDismiss = liveViewModel::hideListingList,
            onListingClick = { listing ->
                liveViewModel.hideListingList()
                liveViewModel.openListing(listing)
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

/** The ad the seller is talking about right now. */
@Composable
private fun PinnedListingCard(
    listing: Listing,
    onViewClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCallClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(Color.White.copy(alpha = 0.94f))
            .clickableNoRipple { onViewClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ListingImage(
            seed = listing.id,
            emoji = listing.emoji,
            imageUrl = listing.imageUrl,
            emojiSize = 26,
            corner = 10.dp,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = listing.title,
                color = Color(0xFF111114),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (listing.isFree) "Free" else listing.priceCents.formatMoney(),
                    color = CommerceColors.AccentPressed,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (listing.isNegotiable && !listing.isFree) {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "negotiable",
                        color = Color(0xFF7B7B85),
                        fontSize = 10.sp,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${listing.city} · ${listing.condition.label}",
                color = Color(0xFF7B7B85),
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (onCallClick != null) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(CommerceColors.Call)
                    .clickableNoRipple { onCallClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_phone_24),
                    contentDescription = "Call seller",
                    tint = Color.White,
                    modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = "View",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(CommerceDimens.PillCorner))
                .background(CommerceColors.Accent)
                .clickableNoRipple { onViewClick() }
                .padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun LiveRoomBottomBar(
    listingCount: Int,
    unreadCount: Int,
    onListingsClick: () -> Unit,
    onMessagesClick: () -> Unit,
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
                        text = "Ask in the live…",
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

        LiveRoomActionButton(
            iconRes = R.drawable.ic_shop_bag_24,
            description = "Ads in this live",
            badge = if (listingCount > 0) listingCount.toString() else null,
            badgeColor = Color.White,
            badgeTextColor = CommerceColors.Accent,
            container = CommerceColors.Accent,
            onClick = onListingsClick,
        )

        LiveRoomActionButton(
            iconRes = R.drawable.ic_chat_24,
            description = "Message the seller",
            badge = if (unreadCount > 0) unreadCount.coerceAtMost(99).toString() else null,
            badgeColor = CommerceColors.Alert,
            badgeTextColor = Color.White,
            container = Color.White.copy(alpha = 0.14f),
            onClick = onMessagesClick,
        )
    }
}

@Composable
private fun LiveRoomActionButton(
    iconRes: Int,
    description: String,
    badge: String?,
    badgeColor: Color,
    badgeTextColor: Color,
    container: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(container)
            .clickableNoRipple { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
        if (badge != null) {
            Text(
                text = badge,
                color = badgeTextColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .padding(horizontal = 4.dp),
            )
        }
    }
}

/**
 * Chat runs bottom aligned and auto scrolls. Joins and "messaged the seller" lines are styled
 * apart from ordinary messages so the activity in the room reads at a glance.
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
        LiveChatKind.CONTACT -> CommerceColors.Accent.copy(alpha = 0.85f)
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
                LiveChatKind.CONTACT -> "💬 ${message.text}"
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
