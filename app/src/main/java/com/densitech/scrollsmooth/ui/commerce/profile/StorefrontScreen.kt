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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.commerce.data.CommerceCatalog
import com.densitech.scrollsmooth.ui.commerce.model.ProductSelection
import com.densitech.scrollsmooth.ui.commerce.model.formatCompact
import com.densitech.scrollsmooth.ui.commerce.view.BuyButton
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.CommerceTopBar
import com.densitech.scrollsmooth.ui.commerce.view.EmptyState
import com.densitech.scrollsmooth.ui.commerce.view.LiveBadge
import com.densitech.scrollsmooth.ui.commerce.view.ProductCard
import com.densitech.scrollsmooth.ui.commerce.view.ProductDetailSheet
import com.densitech.scrollsmooth.ui.commerce.view.SecondaryButton
import com.densitech.scrollsmooth.ui.commerce.view.placeholderBrush
import com.densitech.scrollsmooth.ui.commerce.viewmodel.CartViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.ShopViewModel
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

/**
 * A creator's shop. On this platform the creator page and the storefront are the same thing, so
 * this doubles as the profile you land on from a video.
 */
@Composable
fun StorefrontScreen(
    sellerId: String,
    shopViewModel: ShopViewModel,
    cartViewModel: CartViewModel,
    onBack: () -> Unit,
    onOpenCheckout: () -> Unit,
    onOpenLive: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val seller = shopViewModel.seller(sellerId)
    val allProducts by shopViewModel.allProducts.collectAsState()
    val following by shopViewModel.following.collectAsState()
    val openProduct by shopViewModel.openProduct.collectAsState()

    val products = allProducts.filter { it.sellerId == sellerId }
    val liveStream = CommerceCatalog.liveStreamForSeller(sellerId)
    val isFollowing = following.contains(sellerId)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background),
    ) {
        CommerceTopBar(
            title = seller?.displayName ?: "Shop",
            subtitle = seller?.atHandle,
            onBack = onBack,
        )

        if (seller == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    emoji = "🕳",
                    title = "Shop not found",
                    subtitle = "This creator is no longer selling on the platform.",
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
                            StorefrontStat(value = products.size.toString(), label = "Listings")
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
                            Icon(
                                painter = painterResource(id = R.drawable.ic_verified_24),
                                contentDescription = "Verified seller",
                                tint = CommerceColors.Accent,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = seller.bio,
                        color = CommerceColors.OnSurfaceMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Ships from ${seller.shipsFrom}",
                        color = CommerceColors.OnSurfaceMuted,
                        fontSize = 11.sp,
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isFollowing) {
                            SecondaryButton(
                                text = "Following",
                                onClick = { shopViewModel.toggleFollow(sellerId) },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            BuyButton(
                                text = "Follow",
                                onClick = { shopViewModel.toggleFollow(sellerId) },
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

                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = if (products.isEmpty()) "No listings yet" else "${products.size} listings",
                        color = CommerceColors.OnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            if (products.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "This creator has not listed anything yet.",
                        color = CommerceColors.OnSurfaceMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    )
                }
            } else {
                items(products, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onClick = { shopViewModel.openProduct(product) },
                        onAddToCart = {
                            cartViewModel.addToCart(
                                ProductSelection(
                                    product = product,
                                    options = product.defaultSelection(),
                                    quantity = 1,
                                    unitPriceCents = product.priceCents,
                                )
                            )
                        },
                    )
                }
            }
        }
    }

    openProduct?.let { product ->
        ProductDetailSheet(
            product = product,
            onDismiss = shopViewModel::closeProduct,
            onAddToCart = { selection ->
                cartViewModel.addToCart(selection)
                shopViewModel.closeProduct()
            },
            onBuyNow = { selection ->
                cartViewModel.addToCart(selection)
                shopViewModel.closeProduct()
                onOpenCheckout()
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
