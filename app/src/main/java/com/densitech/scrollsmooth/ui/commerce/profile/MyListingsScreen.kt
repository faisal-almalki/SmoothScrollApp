package com.densitech.scrollsmooth.ui.commerce.profile

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
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
import com.densitech.scrollsmooth.ui.commerce.model.Listing
import com.densitech.scrollsmooth.ui.commerce.model.formatCompact
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.commerce.model.formatPostedAge
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.CommerceTopBar
import com.densitech.scrollsmooth.ui.commerce.view.EmptyState
import com.densitech.scrollsmooth.ui.commerce.view.ListingImage
import com.densitech.scrollsmooth.ui.commerce.view.PrimaryButton
import com.densitech.scrollsmooth.ui.commerce.viewmodel.MessagesViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.MyListingsViewModel
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

/**
 * The ads you have posted, with the two numbers a classifieds seller actually watches: how many
 * people saw it, and how many wrote to you about it.
 */
@Composable
fun MyListingsScreen(
    myListingsViewModel: MyListingsViewModel,
    messagesViewModel: MessagesViewModel,
    onBack: () -> Unit,
    onPostListing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val me by myListingsViewModel.me.collectAsState()
    val allListings by myListingsViewModel.allListings.collectAsState()
    val conversations by messagesViewModel.conversations.collectAsState()

    val myListings = allListings.filter { it.sellerId == me.id }
    val nowMillis = remember(myListings) { System.currentTimeMillis() }
    val enquiriesByListing = remember(conversations) {
        conversations.groupingBy { it.listingId }.eachCount()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background),
    ) {
        CommerceTopBar(
            title = "My ads",
            subtitle = if (myListings.isEmpty()) null else "${myListings.size} posted",
            onBack = onBack,
        )

        if (myListings.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                EmptyState(
                    emoji = "🏷",
                    title = "Nothing posted yet",
                    subtitle = "Post an ad, then attach it to a video so people can find it while " +
                        "they watch.",
                    actionText = "Post an ad",
                    onActionClick = onPostListing,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = CommerceDimens.ScreenPadding,
                    end = CommerceDimens.ScreenPadding,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(myListings, key = { it.id }) { listing ->
                    MyListingRow(
                        listing = listing,
                        nowMillis = nowMillis,
                        enquiryCount = enquiriesByListing[listing.id] ?: 0,
                        onToggleSold = {
                            myListingsViewModel.markSold(listing.id, !listing.isSold)
                        },
                        onDelete = { myListingsViewModel.removeListing(listing.id) },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CommerceColors.Surface)
                .navigationBarsPadding()
                .padding(CommerceDimens.ScreenPadding),
        ) {
            PrimaryButton(
                text = "Post an ad",
                onClick = onPostListing,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MyListingRow(
    listing: Listing,
    nowMillis: Long,
    enquiryCount: Int,
    onToggleSold: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.Surface)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ListingImage(
                seed = listing.id,
                emoji = listing.emoji,
                imageUrl = listing.imageUrl,
                emojiSize = 22,
                corner = 10.dp,
                modifier = Modifier.size(52.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listing.title,
                    color = CommerceColors.OnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(listing.priceCents.formatMoney())
                        append(" · ")
                        append(listing.city)
                        if (listing.postedAtMillis > 0L) {
                            append(" · ")
                            append(formatPostedAge(listing.postedAtMillis, nowMillis))
                        }
                    },
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete ad",
                tint = CommerceColors.OnSurfaceMuted,
                modifier = Modifier
                    .size(18.dp)
                    .clickableNoRipple { onDelete() },
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "👁 ${listing.viewCount.formatCompact()} views",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 11.sp,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "💬 $enquiryCount enquiries",
                color = if (enquiryCount > 0) CommerceColors.Accent else CommerceColors.OnSurfaceMuted,
                fontSize = 11.sp,
                fontWeight = if (enquiryCount > 0) FontWeight.Bold else FontWeight.Normal,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (listing.isSold) "Mark available" else "Mark sold",
                color = if (listing.isSold) CommerceColors.Accent else CommerceColors.OnSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(CommerceDimens.PillCorner))
                    .background(CommerceColors.SurfaceElevated)
                    .clickableNoRipple { onToggleSold() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
