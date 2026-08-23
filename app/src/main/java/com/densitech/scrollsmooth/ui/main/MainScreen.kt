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
import androidx.compose.runtime.collectAsState
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
import com.densitech.scrollsmooth.ui.commerce.auth.AuthViewModel
import com.densitech.scrollsmooth.ui.commerce.auth.LoginScreen
import com.densitech.scrollsmooth.ui.commerce.browse.BrowseScreen
import com.densitech.scrollsmooth.ui.commerce.creator.TagListingsScreen
import com.densitech.scrollsmooth.ui.commerce.live.LiveRoomScreen
import com.densitech.scrollsmooth.ui.commerce.live.LiveScreen
import com.densitech.scrollsmooth.ui.commerce.messages.ChatScreen
import com.densitech.scrollsmooth.ui.commerce.messages.InboxScreen
import com.densitech.scrollsmooth.ui.commerce.profile.AccountSettingsScreen
import com.densitech.scrollsmooth.ui.commerce.profile.MyListingsScreen
import com.densitech.scrollsmooth.ui.commerce.profile.PostListingScreen
import com.densitech.scrollsmooth.ui.commerce.profile.ProfileScreen
import com.densitech.scrollsmooth.ui.commerce.profile.StorefrontScreen
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.viewmodel.BrowseViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.LiveViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.MessagesViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.MyListingsViewModel
import com.densitech.scrollsmooth.ui.video.view.VideoScreen
import com.densitech.scrollsmooth.ui.video.viewmodel.VideoScreenViewModel
import com.densitech.scrollsmooth.ui.video_creation.view.VideoCreationScreen
import com.densitech.scrollsmooth.ui.video_creation.viewmodel.VideoCreationViewModel
import com.densitech.scrollsmooth.ui.video_transformation.view.VideoTransformationScreen
import com.densitech.scrollsmooth.ui.video_transformation.viewmodel.VideoTransformationViewModel
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Routes that take over the whole screen. The bottom bar is hidden on these so the video, the
 * live room or a conversation gets the full height.
 */
private val FULL_SCREEN_ROUTES = setOf(
    Screen.Add.route,
    Screen.VideoTransformation.route,
    Screen.LiveRoom.route,
    Screen.Inbox.route,
    Screen.Chat.route,
    Screen.MyListings.route,
    Screen.PostListing.route,
    Screen.AccountSettings.route,
    Screen.Storefront.route,
    Screen.TagListings.route,
    Screen.Login.route,
)

@OptIn(UnstableApi::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    videoScreenViewModel: VideoScreenViewModel = hiltViewModel(),
    videoCreationViewModel: VideoCreationViewModel = hiltViewModel(),
    videoTransformationViewModel: VideoTransformationViewModel = hiltViewModel(),
    audioSelectionViewModel: AudioSelectionViewModel = hiltViewModel(),
    // Marketplace view models are resolved here, at activity scope, so the feed, the live rooms
    // and the browse tab all share one set of listings and one inbox.
    browseViewModel: BrowseViewModel = hiltViewModel(),
    messagesViewModel: MessagesViewModel = hiltViewModel(),
    liveViewModel: LiveViewModel = hiltViewModel(),
    myListingsViewModel: MyListingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val tabTitles = listOf(Screen.Home, Screen.Browse, Screen.Add, Screen.Live, Screen.Profile)

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

    val isSignedIn by authViewModel.isSignedIn.collectAsState()

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
    val openInbox: () -> Unit = { navController.navigate(Screen.Inbox.route) }
    val openConversation: (String) -> Unit = { navController.navigate(Screen.Chat.create(it)) }
    val openSeller: (String) -> Unit = { navController.navigate(Screen.Storefront.create(it)) }
    val openLiveRoom: (String) -> Unit = { navController.navigate(Screen.LiveRoom.create(it)) }
    val openPostListing: () -> Unit = { navController.navigate(Screen.PostListing.route) }
    val openLogin: () -> Unit = { navController.navigate(Screen.Login.route) }

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
                    browseViewModel = browseViewModel,
                    messagesViewModel = messagesViewModel,
                    onOpenMessages = openInbox,
                    onOpenConversation = openConversation,
                    onOpenSeller = openSeller,
                    onOpenLive = openLiveRoom,
                )
            }

            composable(Screen.Browse.route) {
                BrowseScreen(
                    browseViewModel = browseViewModel,
                    messagesViewModel = messagesViewModel,
                    liveViewModel = liveViewModel,
                    onOpenMessages = openInbox,
                    onOpenConversation = openConversation,
                    onOpenSeller = openSeller,
                    onOpenLive = openLiveRoom,
                )
            }

            composable(Screen.Live.route) {
                LiveScreen(
                    liveViewModel = liveViewModel,
                    messagesViewModel = messagesViewModel,
                    onOpenRoom = openLiveRoom,
                    onOpenMessages = openInbox,
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
                    myListingsViewModel = myListingsViewModel,
                    messagesViewModel = messagesViewModel,
                    browseViewModel = browseViewModel,
                    onPostListing = openPostListing,
                    onOpenMyListings = { navController.navigate(Screen.MyListings.route) },
                    onOpenMessages = openInbox,
                    onOpenSettings = { navController.navigate(Screen.AccountSettings.route) },
                    onOpenSeller = openSeller,
                    isSignedIn = isSignedIn,
                    onOpenLogin = openLogin,
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

            composable(Screen.Inbox.route) {
                InboxScreen(
                    messagesViewModel = messagesViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenConversation = openConversation,
                    onBrowse = { openTab(Screen.Browse) },
                )
            }

            composable(Screen.Chat.route) { entry ->
                val raw = entry.arguments?.getString(Screen.Chat.ARG_CONVERSATION_ID).orEmpty()
                ChatScreen(
                    conversationId = Screen.Chat.decode(raw),
                    messagesViewModel = messagesViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenSeller = openSeller,
                    onOpenListing = { listingId ->
                        messagesViewModel.listing(listingId)?.let { listing ->
                            openSeller(listing.sellerId)
                        }
                    },
                )
            }

            composable(Screen.LiveRoom.route) { entry ->
                val streamId = entry.arguments?.getString(Screen.LiveRoom.ARG_STREAM_ID).orEmpty()
                LiveRoomScreen(
                    streamId = streamId,
                    liveViewModel = liveViewModel,
                    messagesViewModel = messagesViewModel,
                    onClose = { navController.popBackStack() },
                    onOpenConversation = openConversation,
                    onOpenSeller = openSeller,
                )
            }

            composable(Screen.Storefront.route) { entry ->
                val sellerId = entry.arguments?.getString(Screen.Storefront.ARG_SELLER_ID).orEmpty()
                StorefrontScreen(
                    sellerId = sellerId,
                    browseViewModel = browseViewModel,
                    messagesViewModel = messagesViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenConversation = openConversation,
                    onOpenLive = openLiveRoom,
                )
            }

            composable(Screen.MyListings.route) {
                MyListingsScreen(
                    myListingsViewModel = myListingsViewModel,
                    messagesViewModel = messagesViewModel,
                    onBack = { navController.popBackStack() },
                    onPostListing = openPostListing,
                )
            }

            composable(Screen.PostListing.route) {
                PostListingScreen(
                    myListingsViewModel = myListingsViewModel,
                    onBack = { navController.popBackStack() },
                    onPosted = { navController.popBackStack() },
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onDismiss = { navController.popBackStack() },
                )
            }

            composable(Screen.AccountSettings.route) {
                AccountSettingsScreen(
                    myListingsViewModel = myListingsViewModel,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.TagListings.route) { entry ->
                val videoId = entry.arguments?.getString(Screen.TagListings.ARG_VIDEO_ID).orEmpty()
                TagListingsScreen(
                    videoId = videoId,
                    myListingsViewModel = myListingsViewModel,
                    onBack = { navController.popBackStack() },
                    onPostListing = openPostListing,
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
