package com.densitech.scrollsmooth.ui.commerce.cart

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.commerce.model.Order
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.CommerceTopBar
import com.densitech.scrollsmooth.ui.commerce.view.EmptyState
import com.densitech.scrollsmooth.ui.commerce.view.ProductImage
import com.densitech.scrollsmooth.ui.commerce.viewmodel.CartViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrdersScreen(
    cartViewModel: CartViewModel,
    onBack: () -> Unit,
    onKeepShopping: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val orders by cartViewModel.orders.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background),
    ) {
        CommerceTopBar(
            title = "My orders",
            subtitle = if (orders.isEmpty()) null else "${orders.size} orders",
            onBack = onBack,
        )

        if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    emoji = "📦",
                    title = "No orders yet",
                    subtitle = "Anything you buy from a video or a live shows up here.",
                    actionText = "Browse the shop",
                    onActionClick = onKeepShopping,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(orders, key = { it.id }) { order ->
                OrderCard(order = order)
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.Surface)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = order.id,
                    color = CommerceColors.OnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatOrderDate(order.placedAtMillis),
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 11.sp,
                )
            }
            Text(
                text = order.status.label,
                color = CommerceColors.Success,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CommerceColors.Success.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        Spacer(Modifier.height(12.dp))

        order.lines.forEach { line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProductImage(
                    seed = line.productId,
                    emoji = line.emoji,
                    emojiSize = 18,
                    corner = 8.dp,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = line.title,
                        color = CommerceColors.OnSurface,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "x${line.quantity}",
                        color = CommerceColors.OnSurfaceMuted,
                        fontSize = 11.sp,
                    )
                }
                Text(
                    text = line.lineTotalCents.formatMoney(),
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 12.sp,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CommerceColors.Outline),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (order.sourceLiveStreamId != null) "Bought in a live" else "Bought in the feed",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 11.sp,
            )
            Text(
                text = order.totalCents.formatMoney(),
                color = CommerceColors.Accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun formatOrderDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.US).format(Date(millis))
