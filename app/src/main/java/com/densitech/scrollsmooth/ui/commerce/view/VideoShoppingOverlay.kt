package com.densitech.scrollsmooth.ui.commerce.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.commerce.model.Product
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple
import kotlinx.coroutines.delay

/**
 * The shopping affordance that sits over a feed video.
 *
 * It starts as a small yellow bag and expands into a product pill a beat after the video starts
 * playing, which is the pattern shoppers already know from short video commerce apps: the video
 * gets the first moment, the product gets the second.
 */
@Composable
fun VideoProductPill(
    product: Product,
    totalTaggedCount: Int,
    onProductClick: () -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    expandDelayMillis: Long = 1_400,
) {
    var expanded by remember(product.id) { mutableStateOf(false) }

    LaunchedEffect(product.id) {
        delay(expandDelayMillis)
        expanded = true
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(CommerceDimens.PillCorner))
                .background(Color.White.copy(alpha = 0.95f))
                .clickableNoRipple { if (expanded) onProductClick() else expanded = true }
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CommerceColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_shop_bag_24),
                    contentDescription = "Shop this video",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.widthIn(max = 150.dp)) {
                        Text(
                            text = product.title,
                            color = Color(0xFF111114),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = product.priceCents.formatMoney(),
                                color = CommerceColors.Accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            product.discountPercent?.let {
                                Spacer(Modifier.width(4.dp))
                                DiscountBadge(percentOff = it)
                            }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Shop",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(CommerceDimens.PillCorner))
                            .background(CommerceColors.Accent)
                            .clickableNoRipple { onProductClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                }
            }
        }

        if (totalTaggedCount > 1) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = "+${totalTaggedCount - 1}",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(CommerceDimens.PillCorner))
                    .background(CommerceColors.Scrim)
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(CommerceDimens.PillCorner))
                    .clickableNoRipple { onSeeAllClick() }
                    .padding(horizontal = 9.dp, vertical = 6.dp),
            )
        }
    }
}

/**
 * "LIVE" chip with a breathing dot, used both on the feed and on the live grid.
 */
@Composable
fun LiveBadge(
    modifier: Modifier = Modifier,
    label: String = "LIVE",
) {
    val transition = rememberInfiniteTransition(label = "live-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "live-pulse-alpha",
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CommerceColors.Live)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(pulse)
                .clip(CircleShape)
                .background(Color.White),
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * The creator strip under a feed video: avatar, handle, follow, and a "live now" entry point when
 * that creator happens to be streaming.
 */
@Composable
fun VideoSellerStrip(
    displayName: String,
    handle: String,
    emoji: String,
    isVerified: Boolean,
    isLiveNow: Boolean,
    onSellerClick: () -> Unit,
    onLiveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(placeholderBrush(handle))
                .clickableNoRipple { onSellerClick() },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.clickableNoRipple { onSellerClick() }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isVerified) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_verified_24),
                        contentDescription = "Verified seller",
                        tint = CommerceColors.Accent,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            Text(
                text = "@$handle",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isLiveNow) {
            Spacer(Modifier.width(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(CommerceDimens.PillCorner))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(
                        1.dp,
                        CommerceColors.Live,
                        RoundedCornerShape(CommerceDimens.PillCorner),
                    )
                    .clickableNoRipple { onLiveClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveBadge()
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Watch",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Cart button with an item-count badge, used in the feed rail and on commerce top bars. */
@Composable
fun CartIconWithBadge(
    itemCount: Int,
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
                painter = painterResource(id = R.drawable.ic_cart_24),
                contentDescription = "Cart",
                tint = tint,
                modifier = Modifier.size(iconSize.dp),
            )
            if (itemCount > 0) {
                Text(
                    text = if (itemCount > 99) "99+" else itemCount.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(CommerceColors.Accent)
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
