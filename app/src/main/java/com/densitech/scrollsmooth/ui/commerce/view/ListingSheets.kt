@file:OptIn(ExperimentalMaterial3Api::class)

package com.densitech.scrollsmooth.ui.commerce.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.commerce.data.ListingRepository
import com.densitech.scrollsmooth.ui.commerce.model.Listing
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import com.densitech.scrollsmooth.ui.commerce.model.formatCompact
import com.densitech.scrollsmooth.ui.commerce.model.formatPostedAge
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

/**
 * The ad detail sheet. Reached from a feed video, a live room, a profile or the browse grid, so
 * it never assumes a navigation host: callers say what "message the seller" should do.
 */
@Composable
fun ListingDetailSheet(
    listing: Listing,
    nowMillis: Long,
    onDismiss: () -> Unit,
    onMessageSeller: (Listing) -> Unit,
    modifier: Modifier = Modifier,
    onCalledSeller: ((Listing) -> Unit)? = null,
    onOpenSeller: ((String) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CommerceColors.Surface,
        contentColor = CommerceColors.OnSurface,
        dragHandle = { SheetGrabber() },
        modifier = modifier,
    ) {
        ListingDetailContent(
            listing = listing,
            nowMillis = nowMillis,
            onMessageSeller = onMessageSeller,
            onCalledSeller = onCalledSeller,
            onOpenSeller = onOpenSeller,
        )
    }
}

@Composable
fun SheetGrabber(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(CommerceColors.Outline),
        )
    }
}

@Composable
fun ListingDetailContent(
    listing: Listing,
    nowMillis: Long,
    onMessageSeller: (Listing) -> Unit,
    modifier: Modifier = Modifier,
    onCalledSeller: ((Listing) -> Unit)? = null,
    onOpenSeller: ((String) -> Unit)? = null,
) {
    val seller = ListingRepository.seller(listing.sellerId)

    Column(modifier = modifier.navigationBarsPadding()) {
        Column(
            modifier = Modifier
                .heightIn(max = 440.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CommerceDimens.ScreenPadding),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                ListingImage(
                    seed = listing.id,
                    emoji = listing.emoji,
                    imageUrl = listing.imageUrl,
                    modifier = Modifier.size(112.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    PriceTag(
                        priceCents = listing.priceCents,
                        isNegotiable = listing.isNegotiable,
                        priceSize = 24,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = listing.title,
                        color = CommerceColors.OnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    ListingMetaRow(listing = listing, nowMillis = nowMillis, textSize = 12)
                    if (listing.viewCount > 0) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "👁 ${listing.viewCount.formatCompact()} views",
                            color = CommerceColors.OnSurfaceMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            if (seller != null) {
                SellerRow(
                    seller = seller,
                    listingCount = ListingRepository.listingsOf(seller.id).size,
                    onClick = onOpenSeller?.let { open -> { open(seller.id) } },
                )
                Spacer(Modifier.height(16.dp))
            }

            Text(
                text = "Description",
                color = CommerceColors.OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = listing.description,
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )

            Spacer(Modifier.height(16.dp))
            MeetSafelyNote()
            Spacer(Modifier.height(20.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CommerceColors.Surface)
                .padding(CommerceDimens.ScreenPadding),
        ) {
            if (listing.isSold) {
                Text(
                    text = "This ad is marked sold.",
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            ContactActions(
                seller = seller,
                onMessage = { onMessageSeller(listing) },
                onCalled = { onCalledSeller?.invoke(listing) },
            )
        }
    }
}

@Composable
fun SellerRow(
    seller: Seller,
    listingCount: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.SurfaceElevated)
            .let { if (onClick != null) it.clickableNoRipple { onClick() } else it }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SellerAvatar(seller = seller, size = 40.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = seller.displayName,
                    color = CommerceColors.OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (seller.isVerified) {
                    Spacer(Modifier.width(4.dp))
                    VerifiedTick()
                }
            }
            Spacer(Modifier.height(2.dp))
            SellerRatingRow(rating = seller.rating, ratingCount = seller.ratingCount)
            Spacer(Modifier.height(2.dp))
            FollowerLine(seller = seller, listingCount = listingCount)
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = CommerceColors.OnSurfaceMuted,
            )
        }
    }
}

/** Everything one video or live room is advertising. */
@Composable
fun ListingListSheet(
    title: String,
    listings: List<Listing>,
    nowMillis: Long,
    onDismiss: () -> Unit,
    onListingClick: (Listing) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CommerceColors.Surface,
        contentColor = CommerceColors.OnSurface,
        dragHandle = { SheetGrabber() },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = CommerceDimens.ScreenPadding)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = title,
                color = CommerceColors.OnSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(text = subtitle, color = CommerceColors.OnSurfaceMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                listings.forEachIndexed { index, listing ->
                    ListingRow(
                        listing = listing,
                        nowMillis = nowMillis,
                        index = index + 1,
                        onClick = { onListingClick(listing) },
                        trailing = {
                            Text(
                                text = "View",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(CommerceDimens.PillCorner))
                                    .background(CommerceColors.Accent)
                                    .clickableNoRipple { onListingClick(listing) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

/** Brief confirmation that a thread was opened with a seller. */
@Composable
fun ContactBanner(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenMessages: (() -> Unit)? = null,
) {
    LaunchedEffect(text) {
        kotlinx.coroutines.delay(2_600)
        onDismiss()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.SurfaceElevated)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "💬", fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = CommerceColors.OnSurface,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (onOpenMessages != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Open",
                color = CommerceColors.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickableNoRipple { onOpenMessages() },
            )
        }
    }
}

/** Kept for callers that only need the age string without the rest of the meta row. */
@Composable
fun PostedAgeText(postedAtMillis: Long, nowMillis: Long, modifier: Modifier = Modifier) {
    Text(
        text = formatPostedAge(postedAtMillis, nowMillis),
        color = CommerceColors.OnSurfaceMuted,
        fontSize = 11.sp,
        modifier = modifier,
    )
}
