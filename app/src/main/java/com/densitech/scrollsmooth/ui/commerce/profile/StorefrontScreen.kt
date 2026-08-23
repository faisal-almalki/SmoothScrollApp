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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.commerce.data.CommerceCatalog
import com.densitech.scrollsmooth.ui.commerce.model.formatCompact
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.CommerceTopBar
import com.densitech.scrollsmooth.ui.commerce.view.ContactActions
import com.densitech.scrollsmooth.ui.commerce.view.EmptyState
import com.densitech.scrollsmooth.ui.commerce.view.ListingCard
import com.densitech.scrollsmooth.ui.commerce.view.ListingDetailSheet
import com.densitech.scrollsmooth.ui.commerce.view.LiveBadge
import com.densitech.scrollsmooth.ui.commerce.view.PrimaryButton
import com.densitech.scrollsmooth.ui.commerce.view.SecondaryButton
import com.densitech.scrollsmooth.ui.commerce.view.SellerRatingRow
import com.densitech.scrollsmooth.ui.commerce.view.VerifiedTick
import com.densitech.scrollsmooth.ui.commerce.view.placeholderBrush
import com.densitech.scrollsmooth.ui.commerce.viewmodel.BrowseViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.MessagesViewModel
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

/**
 * A seller's page. On this platform the profile and the shop are the same thing, so this is where
 * a video's creator link lands: who they are, what they have for sale, and how to reach them.
 */
@Composable
fun StorefrontScreen(
    sellerId: String,
    browseViewModel: BrowseViewModel,
    messagesViewModel: MessagesViewModel,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenLive: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allListings by browseViewModel.allListings.collectAsState()
    val following by browseViewModel.following.collectAsState()
    val openListing by browseViewModel.openListing.collectAsState()

    val seller = browseViewModel.seller(sellerId)
    val listings = allListings.filter { it.sellerId == sellerId && !it.isSold }
    val liveStream = CommerceCatalog.liveStreamForSeller(sellerId)
    val isFollowing = following.contains(sellerId)
    val nowMillis = remember(listings) { System.currentTimeMillis() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background),
    ) {
        CommerceTopBar(
            title = seller?.displayName ?: "Seller",
            subtitle = seller?.atHandle,
            onBack = onBack,
        )

        if (seller == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    emoji = "🕳",
                    title = "Seller not found",
                    subtitle = "This account is no longer on the platform.",
                )
            }
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = CommerceDimens.ScreenPadding,
                end = CommerceDimens.ScreenPadding,
                bottom = 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(CommerceDimens.GridGutter),
            verticalArrangement = Arrangement.spacedBy(CommerceDimens.GridGutter),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(placeholderBrush(seller.id)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = seller.emoji, fontSize = 30.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            StorefrontStat(
                                value = seller.followers.formatCompact(),
                                label = "Followers",
                            )
                            StorefrontStat(value = listings.size.toString(), label = "Ads")
                            StorefrontStat(
                                value = String.format(java.util.Locale.US, "%.1f", seller.rating),
                                label = "Rating",
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = seller.displayName,
                            color = CommerceColors.OnSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (seller.isVerified) {
                            Spacer(Modifier.width(5.dp))
                            VerifiedTick(size = 15.dp)
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    SellerRatingRow(
                        rating = seller.rating,
                        ratingCount = seller.ratingCount,
                        textSize = 12,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = seller.bio,
                        color = CommerceColors.OnSurfaceMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Based in ${seller.city}",
                        color = CommerceColors.OnSurfaceMuted,
                        fontSize = 11.sp,
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isFollowing) {
                            SecondaryButton(
                                text = "Following",
                                onClick = { browseViewModel.toggleFollow(sellerId) },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            PrimaryButton(
                                text = "Follow",
                                onClick = { browseViewModel.toggleFollow(sellerId) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (liveStream != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(CommerceDimens.PillCorner))
                                    .background(CommerceColors.Live.copy(alpha = 0.16f))
                                    .clickableNoRipple { onOpenLive(liveStream.id) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LiveBadge()
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Watch live",
                                        color = CommerceColors.OnSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    // Contact from the profile uses whatever ad is newest, so there is always a
                    // thread to anchor the conversation to.
                    val newestListing = listings.maxByOrNull { it.postedAtMillis }
                    if (newestListing != null) {
                        Spacer(Modifier.height(10.dp))
                        ContactActions(
                            seller = seller,
                            compact = true,
                            onMessage = {
                                onOpenConversation(
                                    messagesViewModel.startConversation(newestListing)
                                )
                            },
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = if (listings.isEmpty()) "No ads right now" else "${listings.size} ads",
                        color = CommerceColors.OnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            if (listings.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "This seller has nothing listed at the moment.",
                        color = CommerceColors.OnSurfaceMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    )
                }
            } else {
                items(listings, key = { it.id }) { listing ->
                    ListingCard(
                        listing = listing,
                        nowMillis = nowMillis,
                        onClick = { browseViewModel.openListing(listing) },
                    )
                }
            }
        }
    }

    openListing?.let { listing ->
        ListingDetailSheet(
            listing = listing,
            nowMillis = nowMillis,
            onDismiss = browseViewModel::closeListing,
            onMessageSeller = { target ->
                browseViewModel.closeListing()
                onOpenConversation(messagesViewModel.startConversation(target))
            },
        )
    }
}

@Composable
private fun StorefrontStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = CommerceColors.OnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(text = label, color = CommerceColors.OnSurfaceMuted, fontSize = 11.sp)
    }
}
