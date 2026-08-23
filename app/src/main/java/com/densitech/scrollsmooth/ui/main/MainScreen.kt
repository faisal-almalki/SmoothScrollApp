@file:kotlin.OptIn(ExperimentalFoundationApi::class)

package com.densitech.scrollsmooth.ui.main

import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.densitech.scrollsmooth.ui.audio.AudioSelectionViewModel
import com.densitech.scrollsmooth.ui.commerce.cart.CartScreen
import com.densitech.scrollsmooth.ui.commerce.cart.CheckoutScreen
import com.densitech.scrollsmooth.ui.commerce.cart.OrderPlacedScreen
import com.densitech.scrollsmooth.ui.commerce.cart.OrdersScreen
import com.densitech.scrollsmooth.ui.commerce.creator.TagProductsScreen
import com.densitech.scrollsmooth.ui.commerce.live.LiveRoomScreen
import com.densitech.scrollsmooth.ui.commerce.live.LiveScreen
import com.densitech.scrollsmooth.ui.commerce.profile.AddProductScreen
import com.densitech.scrollsmooth.ui.commerce.profile.ProfileScreen
import com.densitech.scrollsmooth.ui.commerce.profile.SellerDashboardScreen
import com.densitech.scrollsmooth.ui.commerce.profile.StorefrontScreen
import com.densitech.scrollsmooth.ui.commerce.shop.ShopScreen
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.viewmodel.CartViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.CreatorViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.LiveViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.ShopViewModel
import com.densitech.scrollsmooth.ui.video.view.VideoScreen
import com.densitech.scrollsmooth.ui.video.viewmodel.VideoScreenViewModel
import com.densitech.scrollsmooth.ui.video_creation.view.VideoCreationScreen
import com.densitech.scrollsmooth.ui.video_creation.viewmodel.VideoCreationViewModel
import com.densitech.scrollsmooth.ui.video_transformation.view.VideoTransformationScreen
import com.densitech.scrollsmooth.ui.video_transformation.viewmodel.VideoTransformationViewModel
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Routes that take over the whole screen. The bottom bar is hidden on these so the video, the
 * live room or the checkout gets the full height.
 */
private val FULL_SCREEN_ROUTES = setOf(
    Screen.Add.route,
    Screen.VideoTransformation.route,
    Screen.LiveRoom.route,
    Screen.Cart.route,
    Screen.Checkout.route,
    Screen.OrderPlaced.route,
    Screen.Orders.route,
    Screen.CreatorStudio.route,
    Screen.AddProduct.route,
    Screen.Storefront.route,
    Screen.TagProducts.route,
)

@OptIn(UnstableApi::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    videoScreenViewModel: VideoScreenViewModel = hiltViewModel(),
    videoCreationViewModel: VideoCreationViewModel = hiltViewModel(),
    videoTransformationViewModel: VideoTransformationViewModel = hiltViewModel(),
    audioSelectionViewModel: AudioSelectionViewModel = hiltViewModel(),
    // Commerce view models are resolved here, at activity scope, so the cart the feed adds to is
    // the same cart the shop tab and checkout read.
    cartViewModel: CartViewModel = hiltViewModel(),
    shopViewModel: ShopViewModel = hiltViewModel(),
    liveViewModel: LiveViewModel = hiltViewModel(),
    creatorViewModel: CreatorViewModel = hiltViewModel(),
) {
    val tabTitles = listOf(Screen.Home, Screen.Shop, Screen.Add, Screen.Live, Screen.Profile)

    val homeVideoPagerState = rememberPagerState(
        pageCount = {
            1000
        },
        initialPage = 0
    )

    val permissionArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        listOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val mediaPermissions = rememberMultiplePermissionsState(
        permissions = permissionArray,
        onPermissionsResult = {

        })

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Shared navigation helpers so every surface opens the same destinations the same way.
    val openTab: (Screen) -> Unit = { screen ->
        navController.navigate(screen.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val openCart: () -> Unit = { navController.navigate(Screen.Cart.route) }
    val openCheckout: () -> Unit = { navController.navigate(Screen.Checkout.route) }
    val openOrders: () -> Unit = { navController.navigate(Screen.Orders.route) }
    val openSeller: (String) -> Unit = { navController.navigate(Screen.Storefront.create(it)) }
    val openLiveRoom: (String) -> Unit = { navController.navigate(Screen.LiveRoom.create(it)) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentDestination?.route.orEmpty() in FULL_SCREEN_ROUTES) {
                return@Scaffold
            }

            NavigationBar(
                containerColor = Color.Black.copy(alpha = 0.1f),
                modifier = Modifier.height(80.dp)
            ) {
                tabTitles.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == screen.route
                    } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = { openTab(screen) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CommerceColors.Accent,
                            unselectedIconColor = Color.White,
                            indicatorColor = Color.Transparent,
                        ),
                        icon = {
                            Icon(
                                painter = painterResource(id = screen.resourceId),
                                contentDescription = screen.title,
                                modifier = Modifier.size(28.dp),
                            )
                        })
                }
            }
        }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(Screen.Home.route) {
                VideoScreen(
                    pagerState = homeVideoPagerState,
                    videoScreenViewModel = videoScreenViewModel,
                    shopViewModel = shopViewModel,
                    cartViewModel = cartViewModel,
                    onOpenCart = openCart,
                    onOpenCheckout = openCheckout,
                    onOpenSeller = openSeller,
                    onOpenLive = openLiveRoom,
                )
            }

            composable(Screen.Shop.route) {
                ShopScreen(
                    shopViewModel = shopViewModel,
                    cartViewModel = cartViewModel,
                    liveViewModel = liveViewModel,
                    onOpenCart = openCart,
                    onOpenCheckout = openCheckout,
                    onOpenSeller = openSeller,
                    onOpenLive = openLiveRoom,
                )
            }

            composable(Screen.Live.route) {
                LiveScreen(
                    liveViewModel = liveViewModel,
                    cartViewModel = cartViewModel,
                    onOpenRoom = openLiveRoom,
                    onOpenCart = openCart,
                )
            }

            composable(Screen.Add.route) {
                if (mediaPermissions.allPermissionsGranted) {
                    VideoCreationScreen(
                        navController = navController,
                        viewModel = videoCreationViewModel
                    )
                } else {
                    mediaPermissions.launchMultiplePermissionRequest()
                }
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    creatorViewModel = creatorViewModel,
                    cartViewModel = cartViewModel,
                    shopViewModel = shopViewModel,
                    onOpenOrders = openOrders,
                    onOpenCart = openCart,
                    onOpenStudio = { navController.navigate(Screen.CreatorStudio.route) },
                    onOpenMyShop = {
                        navController.navigate(
                            Screen.Storefront.create(creatorViewModel.me.value.id)
                        )
                    },
                    onOpenSeller = openSeller,
                )
            }

            composable(Screen.VideoTransformation.route) {
                val selectedVideo = videoCreationViewModel.selectedVideo.value
                    ?: throw Exception("You haven't selected any video")
                VideoTransformationScreen(
                    navController = navController,
                    selectedVideo = selectedVideo,
                    videoTransformationViewModel = videoTransformationViewModel,
                    audioSelectionViewModel = audioSelectionViewModel
                )
            }

            composable(Screen.Cart.route) {
                CartScreen(
                    cartViewModel = cartViewModel,
                    shopViewModel = shopViewModel,
                    onBack = { navController.popBackStack() },
                    onCheckout = openCheckout,
                    onKeepShopping = { openTab(Screen.Shop) },
                    onOpenSeller = openSeller,
                )
            }

            composable(Screen.Checkout.route) {
                CheckoutScreen(
                    cartViewModel = cartViewModel,
                    onBack = { navController.popBackStack() },
                    onOrderPlaced = { orderId ->
                        navController.navigate(Screen.OrderPlaced.create(orderId)) {
                            // The cart is gone once the order exists, so drop checkout and cart
                            // off the back stack rather than letting the shopper walk back into them.
                            popUpTo(Screen.Home.route)
                        }
                    },
                )
            }

            composable(Screen.OrderPlaced.route) { entry ->
                val orderId = entry.arguments?.getString(Screen.OrderPlaced.ARG_ORDER_ID).orEmpty()
                OrderPlacedScreen(
                    orderId = orderId,
                    cartViewModel = cartViewModel,
                    onKeepShopping = { openTab(Screen.Shop) },
                    onViewOrders = openOrders,
                )
            }

            composable(Screen.Orders.route) {
                OrdersScreen(
                    cartViewModel = cartViewModel,
                    onBack = { navController.popBackStack() },
                    onKeepShopping = { openTab(Screen.Shop) },
                )
            }

            composable(Screen.LiveRoom.route) { entry ->
                val streamId = entry.arguments?.getString(Screen.LiveRoom.ARG_STREAM_ID).orEmpty()
                LiveRoomScreen(
                    streamId = streamId,
                    liveViewModel = liveViewModel,
                    cartViewModel = cartViewModel,
                    onClose = { navController.popBackStack() },
                    onOpenCart = openCart,
                    onOpenCheckout = openCheckout,
                    onOpenSeller = openSeller,
                )
            }

            composable(Screen.Storefront.route) { entry ->
                val sellerId = entry.arguments?.getString(Screen.Storefront.ARG_SELLER_ID).orEmpty()
                StorefrontScreen(
                    sellerId = sellerId,
                    shopViewModel = shopViewModel,
                    cartViewModel = cartViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenCheckout = openCheckout,
                    onOpenLive = openLiveRoom,
                )
            }

            composable(Screen.CreatorStudio.route) {
                SellerDashboardScreen(
                    creatorViewModel = creatorViewModel,
                    onBack = { navController.popBackStack() },
                    onListProduct = { navController.navigate(Screen.AddProduct.route) },
                    onOpenMyShop = {
                        navController.navigate(
                            Screen.Storefront.create(creatorViewModel.me.value.id)
                        )
                    },
                )
            }

            composable(Screen.AddProduct.route) {
                AddProductScreen(
                    creatorViewModel = creatorViewModel,
                    onBack = { navController.popBackStack() },
                    onListed = { navController.popBackStack() },
                )
            }

            composable(Screen.TagProducts.route) { entry ->
                val videoId = entry.arguments?.getString(Screen.TagProducts.ARG_VIDEO_ID).orEmpty()
                TagProductsScreen(
                    videoId = videoId,
                    creatorViewModel = creatorViewModel,
                    onBack = { navController.popBackStack() },
                    onListProduct = { navController.navigate(Screen.AddProduct.route) },
                    onPublished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
