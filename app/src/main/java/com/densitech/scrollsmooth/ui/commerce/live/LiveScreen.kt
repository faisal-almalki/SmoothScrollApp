package com.densitech.scrollsmooth.ui.commerce.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.commerce.model.LiveStream
import com.densitech.scrollsmooth.ui.commerce.model.formatCompact
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.commerce.view.CartIconWithBadge
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.LiveBadge
import com.densitech.scrollsmooth.ui.commerce.view.placeholderBrush
import com.densitech.scrollsmooth.ui.commerce.viewmodel.CartViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.LiveViewModel
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

/**
 * The live tab: every creator currently selling on camera. Tapping a card drops into that room.
 */
@Composable
fun LiveScreen(
    liveViewModel: LiveViewModel,
    cartViewModel: CartViewModel,
    onOpenRoom: (String) -> Unit,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val streams by liveViewModel.streams.collectAsState()
    val cart by cartViewModel.cart.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CommerceDimens.ScreenPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Live now",
                    color = CommerceColors.OnSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${streams.size} creators selling on camera",
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 13.sp,
                )
            }
            CartIconWithBadge(
                itemCount = cart.itemCount,
                onClick = onOpenCart,
                tint = CommerceColors.OnSurface,
                iconSize = 26,
            )
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
            items(streams, key = { it.id }) { stream ->
                LiveStreamCard(
                    stream = stream,
                    sellerName = liveViewModel.sellerOf(stream)?.displayName.orEmpty(),
                    sellerEmoji = liveViewModel.sellerOf(stream)?.emoji ?: "🛍",
                    pinnedPriceCents = liveViewModel.pinnedProductOf(stream)?.priceCents,
                    onClick = { onOpenRoom(stream.id) },
                )
            }
        }
    }
}

@Composable
private fun LiveStreamCard(
    stream: LiveStream,
    sellerName: String,
    sellerEmoji: String,
    pinnedPriceCents: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.Surface)
            .clickableNoRipple { onClick() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.78f)
                .background(placeholderBrush(stream.id)),
        ) {
            // A soft scrim keeps the white overlay text readable over any gradient.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                        )
                    ),
            )

            Text(
                text = sellerEmoji,
                fontSize = 46.sp,
                modifier = Modifier.align(Alignment.Center),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveBadge()
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "👁 ${stream.startingViewerCount.formatCompact()}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CommerceColors.Scrim)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }

            Text(
                text = "${stream.flashSaleDiscountPercent}% off live",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(CommerceColors.Accent)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }

        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = stream.title,
                color = CommerceColors.OnSurface,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(placeholderBrush(stream.sellerId)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = sellerEmoji, fontSize = 9.sp)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = sellerName,
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (pinnedPriceCents != null) {
                    Text(
                        text = "from ${pinnedPriceCents.formatMoney()}",
                        color = CommerceColors.Accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
