package com.densitech.scrollsmooth.ui.commerce.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.commerce.model.formatCompact
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.PrimaryButton
import com.densitech.scrollsmooth.ui.commerce.view.SectionHeader
import com.densitech.scrollsmooth.ui.commerce.view.placeholderBrush
import com.densitech.scrollsmooth.ui.commerce.viewmodel.BrowseViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.MessagesViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.MyListingsViewModel
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

/**
 * My account. Everyone here both buys and sells, so this page carries the ads you have posted and
 * the conversations they started, side by side.
 */
@Composable
fun ProfileScreen(
    myListingsViewModel: MyListingsViewModel,
    messagesViewModel: MessagesViewModel,
    browseViewModel: BrowseViewModel,
    onPostListing: () -> Unit,
    onOpenMyListings: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSeller: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSignedIn: Boolean = false,
    onOpenLogin: () -> Unit = {},
) {
    val me by myListingsViewModel.me.collectAsState()
    val allListings by myListingsViewModel.allListings.collectAsState()
    val conversations by messagesViewModel.conversations.collectAsState()
    val following by browseViewModel.following.collectAsState()

    val myListings = allListings.filter { it.sellerId == me.id }
    val liveListings = myListings.filterNot { it.isSold }
    val totalViews = liveListings.sumOf { it.viewCount }
    val unread = conversations.count { it.hasUnread }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CommerceDimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(placeholderBrush(me.id)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = me.emoji, fontSize = 38.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = me.displayName,
                color = CommerceColors.OnSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${me.atHandle} · ${me.city}",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatColumn(value = liveListings.size.toString(), label = "Live ads")
                StatColumn(value = totalViews.formatCompact(), label = "Views")
                StatColumn(value = conversations.size.toString(), label = "Chats")
                StatColumn(value = following.size.toString(), label = "Following")
            }

            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                text = "Post an ad",
                leadingEmoji = "➕",
                onClick = onPostListing,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(modifier = Modifier.padding(horizontal = CommerceDimens.ScreenPadding)) {
            // Browsing works signed out, so the prompt lives here rather than
            // as a wall in front of the app.
            if (!isSignedIn) {
                ProfileRow(
                    emoji = "🔐",
                    title = "Sign in",
                    subtitle = "Needed to message sellers and post ads",
                    onClick = onOpenLogin,
                    modifier = Modifier.padding(bottom = 18.dp),
                )
            }

            SectionHeader(title = "Selling")
            Spacer(Modifier.height(10.dp))

            ProfileRow(
                icon = painterResource(id = R.drawable.ic_storefront_24),
                title = "My ads",
                subtitle = if (myListings.isEmpty()) {
                    "You have not posted anything yet"
                } else {
                    "${liveListings.size} live · ${myListings.size - liveListings.size} sold"
                },
                onClick = onOpenMyListings,
            )
            Spacer(Modifier.height(8.dp))
            ProfileRow(
                icon = painterResource(id = R.drawable.ic_phone_24),
                title = "Contact settings",
                subtitle = if (me.hasPublicPhone) {
                    "Calls on · ${me.phoneNumber}"
                } else {
                    "Calls off · messages only"
                },
                onClick = onOpenSettings,
            )

            Spacer(Modifier.height(22.dp))
            SectionHeader(title = "Buying")
            Spacer(Modifier.height(10.dp))

            ProfileRow(
                icon = painterResource(id = R.drawable.ic_chat_24),
                title = "Messages",
                subtitle = when {
                    conversations.isEmpty() -> "No conversations yet"
                    unread > 0 -> "$unread unread · ${conversations.size} conversations"
                    else -> "${conversations.size} conversations"
                },
                onClick = onOpenMessages,
            )

            Spacer(Modifier.height(22.dp))
            SectionHeader(title = "Sellers you follow")
            Spacer(Modifier.height(10.dp))

            if (following.isEmpty()) {
                Text(
                    text = "Follow a seller from any video to see their new ads first.",
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            } else {
                following.forEach { sellerId ->
                    browseViewModel.seller(sellerId)?.let { seller ->
                        ProfileRow(
                            emoji = seller.emoji,
                            title = seller.displayName,
                            subtitle = "${seller.followers.formatCompact()} followers · ${
                                browseViewModel.listingsOf(seller.id).size
                            } ads · ${seller.city}",
                            onClick = { onOpenSeller(seller.id) },
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = CommerceColors.OnSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(text = label, color = CommerceColors.OnSurfaceMuted, fontSize = 11.sp)
    }
}

@Composable
private fun ProfileRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    emoji: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.Surface)
            .clickableNoRipple { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CommerceColors.SurfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            when {
                icon != null -> Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = CommerceColors.OnSurface,
                    modifier = Modifier.size(19.dp),
                )

                emoji != null -> Text(text = emoji, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = CommerceColors.OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = subtitle, color = CommerceColors.OnSurfaceMuted, fontSize = 12.sp)
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = CommerceColors.OnSurfaceMuted,
        )
    }
}
