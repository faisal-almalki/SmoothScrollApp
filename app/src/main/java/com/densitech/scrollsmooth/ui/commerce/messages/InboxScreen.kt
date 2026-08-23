package com.densitech.scrollsmooth.ui.commerce.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.commerce.model.Conversation
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.commerce.model.formatPostedAge
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.CommerceTopBar
import com.densitech.scrollsmooth.ui.commerce.view.EmptyState
import com.densitech.scrollsmooth.ui.commerce.view.ListingImage
import com.densitech.scrollsmooth.ui.commerce.viewmodel.MessagesViewModel
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

@Composable
fun InboxScreen(
    messagesViewModel: MessagesViewModel,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onBrowse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val conversations by messagesViewModel.conversations.collectAsState()
    val nowMillis = remember(conversations) { System.currentTimeMillis() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background),
    ) {
        CommerceTopBar(
            title = "Messages",
            subtitle = if (conversations.isEmpty()) null else "${conversations.size} conversations",
            onBack = onBack,
        )

        if (conversations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    emoji = "💬",
                    title = "No messages yet",
                    subtitle = "Open any ad and message the seller. Your conversations show up here.",
                    actionText = "Browse ads",
                    onActionClick = onBrowse,
                )
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = CommerceDimens.ScreenPadding,
                end = CommerceDimens.ScreenPadding,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(conversations, key = { it.id }) { conversation ->
                ConversationRow(
                    conversation = conversation,
                    sellerName = messagesViewModel.seller(conversation.sellerId)?.displayName
                        ?: "Seller",
                    nowMillis = nowMillis,
                    onClick = { onOpenConversation(conversation.id) },
                )
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    sellerName: String,
    nowMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.Surface)
            .clickableNoRipple { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ListingImage(
            seed = conversation.listingId,
            emoji = conversation.listingEmoji,
            emojiSize = 22,
            corner = 10.dp,
            modifier = Modifier.size(52.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sellerName,
                    color = CommerceColors.OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (conversation.updatedAtMillis > 0L) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatPostedAge(conversation.updatedAtMillis, nowMillis),
                        color = CommerceColors.OnSurfaceMuted,
                        fontSize = 10.sp,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${conversation.listingTitle} · ${conversation.listingPriceCents.formatMoney()}",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = conversation.lastMessage?.text ?: "No messages yet",
                color = if (conversation.hasUnread) {
                    CommerceColors.OnSurface
                } else {
                    CommerceColors.OnSurfaceMuted
                },
                fontSize = 12.sp,
                fontWeight = if (conversation.hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (conversation.hasUnread) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(CommerceColors.Alert),
            )
        }
    }
}
