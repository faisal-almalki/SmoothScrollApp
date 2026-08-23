package com.densitech.scrollsmooth.ui.commerce.creator

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.commerce.model.Listing
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.CommerceTopBar
import com.densitech.scrollsmooth.ui.commerce.view.ListingImage
import com.densitech.scrollsmooth.ui.commerce.view.PrimaryButton
import com.densitech.scrollsmooth.ui.commerce.view.SecondaryButton
import com.densitech.scrollsmooth.ui.commerce.viewmodel.MyListingsViewModel
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

/**
 * The publishing step that makes a video shoppable: choose which of your ads appear as the tag
 * while it plays.
 */
@Composable
fun TagListingsScreen(
    videoId: String,
    myListingsViewModel: MyListingsViewModel,
    onBack: () -> Unit,
    onPublished: () -> Unit,
    onPostListing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val me by myListingsViewModel.me.collectAsState()
    val allListings by myListingsViewModel.allListings.collectAsState()
    val selectedIds by myListingsViewModel.pendingVideoTags.collectAsState()

    val myListings = allListings.filter { it.sellerId == me.id && !it.isSold }
    val nowMillis = remember(myListings) { System.currentTimeMillis() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background),
    ) {
        CommerceTopBar(
            title = "Tag your ads",
            subtitle = "Buyers tap these while your video plays",
            onBack = onBack,
        )

        if (myListings.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(CommerceDimens.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "🏷", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "You have no ads to tag",
                    color = CommerceColors.OnSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Post an ad first, then come back and attach it to this video.",
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(20.dp))
                PrimaryButton(
                    text = "Post an ad",
                    onClick = onPostListing,
                    modifier = Modifier.fillMaxWidth(),
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
                    TaggableListingRow(
                        listing = listing,
                        selected = selectedIds.contains(listing.id),
                        onToggle = { myListingsViewModel.togglePendingTag(listing.id) },
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CommerceColors.Surface)
                .navigationBarsPadding()
                .padding(CommerceDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (selectedIds.isEmpty()) {
                    "No ads tagged. The video posts without a tag."
                } else {
                    "${selectedIds.size} tagged. The first one shows as the pill on the video."
                },
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 12.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    text = "Skip",
                    onClick = {
                        myListingsViewModel.clearPendingTags()
                        onPublished()
                    },
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "Post video",
                    onClick = {
                        myListingsViewModel.publishTags(videoId)
                        onPublished()
                    },
                    modifier = Modifier.weight(1.4f),
                )
            }
        }
    }
}

@Composable
private fun TaggableListingRow(
    listing: Listing,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.Surface)
            .border(
                width = 1.dp,
                color = if (selected) CommerceColors.Accent else CommerceColors.Outline,
                shape = RoundedCornerShape(CommerceDimens.CardCorner),
            )
            .clickableNoRipple { onToggle() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${listing.priceCents.formatMoney()} · ${listing.city}",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selected) CommerceColors.Accent else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (selected) CommerceColors.Accent else CommerceColors.Outline,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
