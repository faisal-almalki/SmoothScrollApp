package com.densitech.scrollsmooth.ui.commerce.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.commerce.model.Listing
import com.densitech.scrollsmooth.ui.commerce.model.ListingCondition
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import com.densitech.scrollsmooth.ui.commerce.model.formatCompact
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.commerce.model.formatPostedAge
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple
import java.util.Locale

/**
 * Square artwork for an ad. Ads in this build carry no photographs, so each one falls back to a
 * gradient derived from its id with its emoji on top: stable per listing, and it works offline.
 */
@Composable
fun ListingImage(
    seed: String,
    emoji: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    corner: Dp = CommerceDimens.CardCorner,
    emojiSize: Int = 44,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(placeholderBrush(seed)),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(text = emoji, fontSize = emojiSize.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SellerAvatar(
    seller: Seller,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(placeholderBrush(seller.id)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = seller.emoji, fontSize = (size.value / 2).sp)
    }
}

/** Price, plus the "negotiable" hint that decides whether a buyer bothers to make contact. */
@Composable
fun PriceTag(
    priceCents: Long,
    modifier: Modifier = Modifier,
    isNegotiable: Boolean = false,
    priceSize: Int = 16,
    accent: Color = CommerceColors.OnSurface,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = if (priceCents <= 0L) "Free" else priceCents.formatMoney(),
            color = accent,
            fontSize = priceSize.sp,
            fontWeight = FontWeight.Bold,
        )
        if (isNegotiable && priceCents > 0L) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = "negotiable",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = (priceSize - 5).coerceAtLeast(9).sp,
            )
        }
    }
}

/** City, age and condition: the three facts a buyer scans before opening an ad. */
@Composable
fun ListingMetaRow(
    listing: Listing,
    nowMillis: Long,
    modifier: Modifier = Modifier,
    textSize: Int = 11,
) {
    Text(
        text = buildString {
            append(listing.city)
            if (listing.postedAtMillis > 0L) {
                append(" · ")
                append(formatPostedAge(listing.postedAtMillis, nowMillis))
            }
            append(" · ")
            append(listing.condition.label)
        },
        color = CommerceColors.OnSurfaceMuted,
        fontSize = textSize.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun SellerRatingRow(
    rating: Float,
    ratingCount: Int,
    modifier: Modifier = Modifier,
    textSize: Int = 11,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = CommerceColors.Warning,
            modifier = Modifier.size((textSize + 3).dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = String.format(Locale.US, "%.1f", rating),
            color = CommerceColors.OnSurfaceMuted,
            fontSize = textSize.sp,
        )
        if (ratingCount > 0) {
            Text(
                text = " ($ratingCount ratings)",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = textSize.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Filled call to action. Reserved for contacting a seller and for posting an ad. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = CommerceColors.Accent,
    contentColor: Color = Color.White,
    leadingEmoji: String? = null,
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(CommerceDimens.PillCorner))
            .background(if (enabled) container else CommerceColors.Outline)
            .clickableNoRipple(enabled = enabled) { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingEmoji != null) {
            Text(text = leadingEmoji, fontSize = 15.sp)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = if (enabled) contentColor else CommerceColors.OnSurfaceMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingEmoji: String? = null,
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(CommerceDimens.PillCorner))
            .border(1.dp, CommerceColors.Outline, RoundedCornerShape(CommerceDimens.PillCorner))
            .clickableNoRipple(enabled = enabled) { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingEmoji != null) {
            Text(text = leadingEmoji, fontSize = 15.sp)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = if (enabled) CommerceColors.OnSurface else CommerceColors.OnSurfaceMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Selectable chip used for categories, cities and condition. */
@Composable
fun CommerceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CommerceDimens.PillCorner))
            .background(if (selected) CommerceColors.Accent else CommerceColors.SurfaceElevated)
            .border(
                width = 1.dp,
                color = if (selected) CommerceColors.Accent else CommerceColors.Outline,
                shape = RoundedCornerShape(CommerceDimens.PillCorner),
            )
            .clickableNoRipple { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else CommerceColors.OnSurface,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            color = CommerceColors.OnSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        if (action != null && onActionClick != null) {
            Text(
                text = action,
                color = CommerceColors.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickableNoRipple { onActionClick() },
            )
        }
    }
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emoji, fontSize = 52.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            color = CommerceColors.OnSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = CommerceColors.OnSurfaceMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        if (actionText != null && onActionClick != null) {
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = actionText,
                onClick = onActionClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Grid tile used on the Browse tab and on a seller's profile. */
@Composable
fun ListingCard(
    listing: Listing,
    nowMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sellerHandle: String? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.Surface)
            .clickableNoRipple { onClick() },
    ) {
        Box {
            ListingImage(
                seed = listing.id,
                emoji = listing.emoji,
                imageUrl = listing.imageUrl,
                corner = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            if (listing.condition == ListingCondition.NEW) {
                Text(
                    text = "New",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CommerceColors.Accent)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            if (listing.isSold) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(CommerceColors.Scrim),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "Sold", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(modifier = Modifier.padding(10.dp)) {
            PriceTag(
                priceCents = listing.priceCents,
                isNegotiable = listing.isNegotiable,
                priceSize = 16,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = listing.title,
                color = CommerceColors.OnSurface,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(5.dp))
            ListingMetaRow(listing = listing, nowMillis = nowMillis)
            if (sellerHandle != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = sellerHandle,
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Horizontal row used in sheets and lists. */
@Composable
fun ListingRow(
    listing: Listing,
    nowMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.SurfaceElevated)
            .clickableNoRipple { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            ListingImage(
                seed = listing.id,
                emoji = listing.emoji,
                imageUrl = listing.imageUrl,
                emojiSize = 28,
                corner = 10.dp,
                modifier = Modifier.size(64.dp),
            )
            if (index != null) {
                Text(
                    text = index.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CommerceColors.Scrim)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            PriceTag(
                priceCents = listing.priceCents,
                isNegotiable = listing.isNegotiable,
                priceSize = 15,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = listing.title,
                color = CommerceColors.OnSurface,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(3.dp))
            ListingMetaRow(listing = listing, nowMillis = nowMillis)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/** Messages entry point with an unread badge. */
@Composable
fun MessagesIconWithBadge(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    iconSize: Int = 30,
    label: String? = null,
) {
    Column(
        modifier = modifier.clickableNoRipple { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Icon(
                painter = painterResource(id = R.drawable.ic_chat_24),
                contentDescription = "Messages",
                tint = tint,
                modifier = Modifier.size(iconSize.dp),
            )
            if (unreadCount > 0) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(CommerceColors.Alert)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        if (label != null) {
            Spacer(Modifier.height(2.dp))
            Text(text = label, color = tint, fontSize = 12.sp)
        }
    }
}

@Composable
fun VerifiedTick(modifier: Modifier = Modifier, size: Dp = 14.dp) {
    Icon(
        painter = painterResource(id = R.drawable.ic_verified_24),
        contentDescription = "Verified seller",
        tint = CommerceColors.Call,
        modifier = modifier.size(size),
    )
}

@Composable
fun FollowerLine(seller: Seller, listingCount: Int, modifier: Modifier = Modifier) {
    Text(
        text = "${seller.followers.formatCompact()} followers · $listingCount ads · ${seller.city}",
        color = CommerceColors.OnSurfaceMuted,
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
