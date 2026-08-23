package com.densitech.scrollsmooth.ui.commerce.shop

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.commerce.data.CommerceCatalog
import com.densitech.scrollsmooth.ui.commerce.model.LiveStream
import com.densitech.scrollsmooth.ui.commerce.model.ProductCategory
import com.densitech.scrollsmooth.ui.commerce.model.ProductSelection
import com.densitech.scrollsmooth.ui.commerce.model.formatCompact
import com.densitech.scrollsmooth.ui.commerce.view.CartIconWithBadge
import com.densitech.scrollsmooth.ui.commerce.view.CommerceChip
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.EmptyState
import com.densitech.scrollsmooth.ui.commerce.view.LiveBadge
import com.densitech.scrollsmooth.ui.commerce.view.ProductCard
import com.densitech.scrollsmooth.ui.commerce.view.ProductDetailSheet
import com.densitech.scrollsmooth.ui.commerce.view.SectionHeader
import com.densitech.scrollsmooth.ui.commerce.view.placeholderBrush
import com.densitech.scrollsmooth.ui.commerce.viewmodel.CartViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.LiveViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.ShopViewModel
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

/**
 * The shop tab. Search, categories and a product grid, with the live rooms surfaced at the top
 * because a running live is the highest converting entry point in the app.
 */
@Composable
fun ShopScreen(
    shopViewModel: ShopViewModel,
    cartViewModel: CartViewModel,
    liveViewModel: LiveViewModel,
    onOpenCart: () -> Unit,
    onOpenCheckout: () -> Unit,
    onOpenSeller: (String) -> Unit,
    onOpenLive: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val query by shopViewModel.query.collectAsState()
    val category by shopViewModel.category.collectAsState()
    val results by shopViewModel.results.collectAsState()
    val openProduct by shopViewModel.openProduct.collectAsState()
    val cart by cartViewModel.cart.collectAsState()
    val streams by liveViewModel.streams.collectAsState()
    val allProducts by shopViewModel.allProducts.collectAsState()

    // A product listed in Creator Studio should show up here without a restart.
    LaunchedEffect(allProducts) { shopViewModel.refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CommerceDimens.ScreenPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchField(
                query = query,
                onQueryChange = shopViewModel::onQueryChange,
                onClear = shopViewModel::clearQuery,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
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
            item(span = { GridItemSpan(maxLineSpan) }) {
                CategoryRow(
                    selected = category,
                    onSelect = shopViewModel::onCategoryChange,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            if (query.isBlank() && streams.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        SectionHeader(title = "Live right now")
                        Spacer(Modifier.height(10.dp))
                        LiveRail(
                            streams = streams,
                            sellerNameOf = { liveViewModel.sellerOf(it)?.displayName.orEmpty() },
                            sellerEmojiOf = { liveViewModel.sellerOf(it)?.emoji ?: "🛍" },
                            onOpenLive = onOpenLive,
                        )
                        Spacer(Modifier.height(18.dp))
                        SectionHeader(
                            title = if (category == ProductCategory.ALL) {
                                "Trending"
                            } else {
                                category.label
                            }
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            if (results.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        emoji = "🔍",
                        title = "Nothing matches that",
                        subtitle = "Try a different search or clear the category filter.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                items(results, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        sellerHandle = shopViewModel.seller(product.sellerId)?.atHandle,
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
            onOpenSeller = { sellerId ->
                shopViewModel.closeProduct()
                onOpenSeller(sellerId)
            },
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(CommerceDimens.PillCorner))
            .background(CommerceColors.Surface)
            .border(1.dp, CommerceColors.Outline, RoundedCornerShape(CommerceDimens.PillCorner))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = CommerceColors.OnSurfaceMuted,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search products and creators",
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 13.sp,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = CommerceColors.OnSurface, fontSize = 13.sp),
                cursorBrush = SolidColor(CommerceColors.Accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear search",
                tint = CommerceColors.OnSurfaceMuted,
                modifier = Modifier
                    .size(18.dp)
                    .clickableNoRipple { onClear() },
            )
        }
    }
}

@Composable
private fun CategoryRow(
    selected: ProductCategory,
    onSelect: (ProductCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(CommerceCatalog.categories, key = { it.name }) { item ->
            CommerceChip(
                text = "${item.emoji}  ${item.label}",
                selected = item == selected,
                onClick = { onSelect(item) },
            )
        }
    }
}

@Composable
private fun LiveRail(
    streams: List<LiveStream>,
    sellerNameOf: (LiveStream) -> String,
    sellerEmojiOf: (LiveStream) -> String,
    onOpenLive: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(streams, key = { it.id }) { stream ->
            Column(
                modifier = Modifier
                    .width(120.dp)
                    .clickableNoRipple { onOpenLive(stream.id) },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(CommerceDimens.CardCorner))
                        .background(placeholderBrush(stream.id)),
                ) {
                    Text(
                        text = sellerEmojiOf(stream),
                        fontSize = 36.sp,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    LiveBadge(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                    )
                    Text(
                        text = "👁 ${stream.startingViewerCount.formatCompact()}",
                        color = CommerceColors.OnSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CommerceColors.Scrim)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = sellerNameOf(stream),
                    color = CommerceColors.OnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${stream.flashSaleDiscountPercent}% off live",
                    color = CommerceColors.Accent,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
