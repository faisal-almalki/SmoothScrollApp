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
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.SectionHeader
import com.densitech.scrollsmooth.ui.commerce.view.placeholderBrush
import com.densitech.scrollsmooth.ui.commerce.viewmodel.CartViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.CreatorViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.ShopViewModel
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

/**
 * The signed in person's own page. Because every account on this platform can sell, the shopper
 * side and the creator side both hang off here.
 */
@Composable
fun ProfileScreen(
    creatorViewModel: CreatorViewModel,
    cartViewModel: CartViewModel,
    shopViewModel: ShopViewModel,
    onOpenOrders: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenStudio: () -> Unit,
    onOpenMyShop: () -> Unit,
    onOpenSeller: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val me by creatorViewModel.me.collectAsState()
    val orders by cartViewModel.orders.collectAsState()
    val cart by cartViewModel.cart.collectAsState()
    val following by shopViewModel.following.collectAsState()
    val allProducts by creatorViewModel.allProducts.collectAsState()

    val myProducts = allProducts.filter { it.sellerId == me.id }
    val revenueCents = creatorViewModel.myGrossRevenueCents()

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
                text = me.atHandle,
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatColumn(value = me.followers.formatCompact(), label = "Followers")
                StatColumn(value = following.size.toString(), label = "Following")
                StatColumn(value = myProducts.size.toString(), label = "Listings")
                StatColumn(value = orders.size.toString(), label = "Orders")
            }
        }

        Spacer(Modifier.height(8.dp))

        Column(modifier = Modifier.padding(horizontal = CommerceDimens.ScreenPadding)) {
            SectionHeader(title = "Selling")
            Spacer(Modifier.height(10.dp))

            ProfileRow(
                icon = painterResource(id = R.drawable.ic_storefront_24),
                title = "Creator Studio",
                subtitle = if (myProducts.isEmpty()) {
                    "List your first product and start selling"
                } else {
                    "${myProducts.size} listings · ${revenueCents.formatMoney()} gross"
                },
                onClick = onOpenStudio,
            )
            Spacer(Modifier.height(8.dp))
            ProfileRow(
                icon = painterResource(id = R.drawable.ic_shop_bag_24),
                title = "My storefront",
                subtitle = "See your shop the way buyers see it",
                onClick = onOpenMyShop,
            )

            Spacer(Modifier.height(22.dp))
            SectionHeader(title = "Shopping")
            Spacer(Modifier.height(10.dp))

            ProfileRow(
                icon = painterResource(id = R.drawable.ic_receipt_24),
                title = "My orders",
                subtitle = if (orders.isEmpty()) "Nothing ordered yet" else "${orders.size} orders",
                onClick = onOpenOrders,
            )
            Spacer(Modifier.height(8.dp))
            ProfileRow(
                icon = painterResource(id = R.drawable.ic_cart_24),
                title = "Cart",
                subtitle = if (cart.isEmpty) {
                    "Empty"
                } else {
                    "${cart.itemCount} items · ${cart.totalCents.formatMoney()}"
                },
                onClick = onOpenCart,
            )

            Spacer(Modifier.height(22.dp))
            SectionHeader(title = "Creators you follow")
            Spacer(Modifier.height(10.dp))

            if (following.isEmpty()) {
                Text(
                    text = "Follow a creator from any video to see their drops here first.",
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            } else {
                following.forEach { sellerId ->
                    shopViewModel.seller(sellerId)?.let { seller ->
                        ProfileRow(
                            emoji = seller.emoji,
                            title = seller.displayName,
                            subtitle = "${seller.followers.formatCompact()} followers · ${
                                shopViewModel.productsOf(seller.id).size
                            } listings",
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
