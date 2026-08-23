package com.densitech.scrollsmooth.ui.commerce.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.commerce.model.ChatMessage
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.CommerceTopBar
import com.densitech.scrollsmooth.ui.commerce.view.EmptyState
import com.densitech.scrollsmooth.ui.commerce.view.ListingImage
import com.densitech.scrollsmooth.ui.commerce.view.dialSeller
import com.densitech.scrollsmooth.ui.commerce.viewmodel.MessagesViewModel
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One thread with a seller. The ad that started the conversation stays pinned at the top so both
 * sides know what is being discussed, and the seller's number sits one tap away if they publish it.
 */
@Composable
fun ChatScreen(
    conversationId: String,
    messagesViewModel: MessagesViewModel,
    onBack: () -> Unit,
    onOpenSeller: (String) -> Unit,
    onOpenListing: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val conversations by messagesViewModel.conversations.collectAsState()
    val conversation = remember(conversations, conversationId) {
        conversations.firstOrNull { it.id == conversationId }
    }
    val seller = messagesViewModel.seller(conversation?.sellerId)
    val listState = rememberLazyListState()

    LaunchedEffect(conversationId) {
        messagesViewModel.markRead(conversationId)
    }

    LaunchedEffect(conversation?.messages?.size) {
        val count = conversation?.messages?.size ?: 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background),
    ) {
        CommerceTopBar(
            title = seller?.displayName ?: "Conversation",
            subtitle = seller?.atHandle,
            onBack = onBack,
            trailing = {
                // Bound to a non-null local so the number is only reachable where it exists.
                val callable = seller?.takeIf { it.hasPublicPhone }
                if (callable != null) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_phone_24),
                        contentDescription = "Call seller",
                        tint = CommerceColors.Call,
                        modifier = Modifier
                            .size(22.dp)
                            .clickableNoRipple { dialSeller(context, callable.phoneNumber) },
                    )
                }
            },
        )

        if (conversation == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    emoji = "🕳",
                    title = "Conversation not found",
                    subtitle = "It may have been deleted from this device.",
                )
            }
            return@Column
        }

        // The ad this thread is about.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CommerceDimens.ScreenPadding)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(CommerceDimens.CardCorner))
                .background(CommerceColors.Surface)
                .clickableNoRipple { onOpenListing(conversation.listingId) }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ListingImage(
                seed = conversation.listingId,
                emoji = conversation.listingEmoji,
                emojiSize = 18,
                corner = 8.dp,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.listingTitle,
                    color = CommerceColors.OnSurface,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = conversation.listingPriceCents.formatMoney(),
                    color = CommerceColors.Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            seller?.let {
                Text(
                    text = "Seller",
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.clickableNoRipple { onOpenSeller(it.id) },
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = CommerceDimens.ScreenPadding,
                end = CommerceDimens.ScreenPadding,
                bottom = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(conversation.messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
        }

        MessageComposer(
            onSend = { text -> messagesViewModel.send(conversationId, text) },
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding(),
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val alignment = if (message.fromMe) Alignment.CenterEnd else Alignment.CenterStart
    val background = if (message.fromMe) CommerceColors.Accent else CommerceColors.Surface
    val textColor = if (message.fromMe) Color.White else CommerceColors.OnSurface

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(background)
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(
                text = message.text,
                color = textColor,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = formatClock(message.sentAtMillis),
                color = textColor.copy(alpha = 0.65f),
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun MessageComposer(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CommerceColors.Surface)
            .padding(CommerceDimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(CommerceDimens.PillCorner))
                .background(CommerceColors.SurfaceElevated)
                .border(
                    1.dp,
                    CommerceColors.Outline,
                    RoundedCornerShape(CommerceDimens.PillCorner),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (draft.isEmpty()) {
                Text(
                    text = "Message the seller…",
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 13.sp,
                )
            }
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                textStyle = TextStyle(color = CommerceColors.OnSurface, fontSize = 13.sp),
                cursorBrush = SolidColor(CommerceColors.Accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(CommerceDimens.PillCorner))
                .background(
                    if (draft.isNotBlank()) CommerceColors.Accent else CommerceColors.Outline
                )
                .clickableNoRipple(enabled = draft.isNotBlank()) {
                    onSend(draft)
                    draft = ""
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun formatClock(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.US).format(Date(millis))
