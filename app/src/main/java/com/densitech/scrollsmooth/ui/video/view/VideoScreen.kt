@file:OptIn(ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)

package com.densitech.scrollsmooth.ui.video.view

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.commerce.data.CommerceCatalog
import com.densitech.scrollsmooth.ui.commerce.model.Listing
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.ContactBanner
import com.densitech.scrollsmooth.ui.commerce.view.ListingDetailSheet
import com.densitech.scrollsmooth.ui.commerce.view.ListingListSheet
import com.densitech.scrollsmooth.ui.commerce.viewmodel.BrowseViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.MessagesViewModel
import com.densitech.scrollsmooth.ui.video.model.ScreenState
import com.densitech.scrollsmooth.ui.video.model.VideoItemParams
import com.densitech.scrollsmooth.ui.video.viewmodel.VideoScreenViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.distinctUntilChanged

@SuppressLint("InlinedApi")
@ExperimentalPermissionsApi
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoScreen(
    pagerState: PagerState,
    videoScreenViewModel: VideoScreenViewModel,
    browseViewModel: BrowseViewModel,
    messagesViewModel: MessagesViewModel,
    onOpenMessages: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenSeller: (String) -> Unit,
    onOpenLive: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifeCycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val mediaItemSource = videoScreenViewModel.mediaItemSource.collectAsState()
    val playerPool = videoScreenViewModel.playerPool.collectAsState()
    val screenState = videoScreenViewModel.screenState.collectAsState()
    val videoDownloadedListState = videoScreenViewModel.videoDownloadedList.collectAsState()

    // Marketplace state layered over the feed.
    val conversations by messagesViewModel.conversations.collectAsState()
    val banner by messagesViewModel.banner.collectAsState()
    val openListing by browseViewModel.openListing.collectAsState()
    var listingListSheet by remember { mutableStateOf<List<Listing>?>(null) }
    val unreadCount = conversations.count { it.hasUnread }
    val nowMillis = remember(openListing, listingListSheet) { System.currentTimeMillis() }

    // State management
    var currentActiveIndex by remember { mutableIntStateOf(videoScreenViewModel.currentPlayingIndex) }
    var isPaused by remember { mutableStateOf(false) }
    val mediaList = remember { mutableStateListOf<MediaItem>() }
    val downloadedVideoList = remember { mutableStateListOf<String>() }

    val fling = PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(1),
        snapPositionalThreshold = 0.3f,
        snapAnimationSpec = spring(stiffness = Spring.StiffnessMedium)
    )

    val notificationPermission = rememberPermissionState(
        permission = android.Manifest.permission.POST_NOTIFICATIONS,
        onPermissionResult = { _ ->
            // Handle permission result
        }
    )

    DisposableEffect(lifeCycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // Register download listener
                    videoScreenViewModel.registerDownloadState()
                    // Play latest video if needed
                    val currentPlayingIndex = videoScreenViewModel.currentPlayingIndex
                    if (currentPlayingIndex == -1) {
                        return@LifecycleEventObserver
                    }
                    videoScreenViewModel.play(currentPlayingIndex)
                }

                Lifecycle.Event.ON_STOP -> {
                    videoScreenViewModel.pauseAllPlayer()
                    // Unregister download state
                    videoScreenViewModel.unRegisterDownloadState()
                }

                Lifecycle.Event.ON_CREATE -> {
                    videoScreenViewModel.pauseAllPlayer()
                    if (mediaItemSource.value?.mediaItems.isNullOrEmpty()) {
                        videoScreenViewModel.initData(context)
                    }
                }

                else -> {

                }
            }
        }

        lifeCycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(videoDownloadedListState.value) {
        downloadedVideoList.clear()
        downloadedVideoList.addAll(videoDownloadedListState.value)
    }

    LaunchedEffect(mediaItemSource.value?.mediaItems) {
        val mediaItems = mediaItemSource.value?.mediaItems
        if (mediaItems != null) {
            mediaList.clear()
            mediaList.addAll(mediaItems)
        }
    }

    LaunchedEffect(screenState.value) {
        if (screenState.value == ScreenState.PLAY_STATE) {
            val playIndex = if (videoScreenViewModel.currentPlayingIndex == -1) {
                0
            } else {
                videoScreenViewModel.currentPlayingIndex
            }
            videoScreenViewModel.play(playIndex)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow {
            pagerState.currentPage
        }.distinctUntilChanged().collect { page ->
            if (mediaList.isNotEmpty()) {
                val realPage = page % mediaList.count()
                currentActiveIndex = realPage
                videoScreenViewModel.play(realPage)

                isPaused = false
            }
        }
    }

    when (screenState.value) {
        ScreenState.LOADING_STATE -> {
            LoadingScreen()
        }

        ScreenState.BUFFER_STATE, ScreenState.PLAY_STATE -> {
            if (mediaList.size > 0) {
                val totalPageCount = remember(mediaList) { mediaList.size }

                Box {
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        beyondViewportPageCount = 1,
                        flingBehavior = fling
                    ) { page ->
                        val realPage = page % totalPageCount
                        val mediaItem = mediaList.getOrNull(realPage) ?: return@VerticalPager
                        val mediaSource =
                            videoScreenViewModel.getMediaSourceByMediaItem(
                                context,
                                mediaItem,
                                realPage
                            )
                                ?: return@VerticalPager

                        // Ensure playerPool.value is not null
                        val currentPlayerPool = playerPool.value ?: return@VerticalPager
                        val mediaInfo =
                            videoScreenViewModel.getCurrentMediaInfo(mediaItem.mediaMetadata)

                        // Tagging is derived from the video id, so it only needs recomputing
                        // when the page actually shows a different video.
                        val taggedListings = remember(mediaInfo.videoId) {
                            browseViewModel.listingsForVideo(mediaInfo.videoId)
                        }
                        val tagSeller = remember(taggedListings) {
                            browseViewModel.seller(taggedListings.firstOrNull()?.sellerId)
                        }
                        val sellerLiveStream = remember(tagSeller) {
                            tagSeller?.let { CommerceCatalog.liveStreamForSeller(it.id) }
                        }

                        VideoItemView(
                            params = VideoItemParams(
                                playerPool = currentPlayerPool,
                                isActive = currentActiveIndex == realPage,
                                currentToken = realPage,
                                currentMediaSource = mediaSource,
                                mediaInfo = mediaInfo,
                                isDownloaded = downloadedVideoList.contains(mediaItem.localConfiguration?.uri.toString()),
                                taggedListings = taggedListings,
                                seller = tagSeller,
                                isSellerLiveNow = sellerLiveStream != null,
                                unreadMessageCount = unreadCount,
                            ),
                            onListingClick = { listing ->
                                videoScreenViewModel.pauseAllPlayer()
                                browseViewModel.openListing(listing)
                            },
                            onSeeAllListingsClick = { listings ->
                                videoScreenViewModel.pauseAllPlayer()
                                listingListSheet = listings
                            },
                            onMessagesClick = onOpenMessages,
                            onSellerClick = onOpenSeller,
                            onWatchLiveClick = {
                                sellerLiveStream?.let { stream -> onOpenLive(stream.id) }
                            },
                            onPlayerReady = { token, exoPlayer ->
                                videoScreenViewModel.onPlayerReady(token, exoPlayer)
                            },
                            onPlayerDestroy = { token ->
                                videoScreenViewModel.onPlayerDestroy(token)
                            },
                            onReceiveRatio = { token, width, height ->
                                videoScreenViewModel.onReceiveRatio(token, width, height)
                            },
                            onPauseClick = {
                                isPaused = it
                            },
                            onDownloadVideoClick = { token ->
                                handleDownloadVideoClick(notificationPermission) {
                                    videoScreenViewModel.downloadVideo(token)
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                        )
                    }

                    AnimatedVisibility(
                        visible = isPaused,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play_arrow_24),
                            contentDescription = null,
                            modifier = Modifier
                                .size(96.dp)
                                .align(Alignment.Center)
                                .alpha(0.2f),
                        )
                    }

                    AnimatedVisibility(
                        visible = banner != null,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(CommerceDimens.ScreenPadding)
                    ) {
                        ContactBanner(
                            text = banner.orEmpty(),
                            onDismiss = messagesViewModel::dismissBanner,
                            onOpenMessages = {
                                messagesViewModel.dismissBanner()
                                onOpenMessages()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        ScreenState.OFFLINE_REQUEST_STATE -> {
            DialogConfirmSwitchOffline(
                dialogTitle = "Can't fetch video from network",
                dialogText = "Click 'Retry' to retry a network call, click 'Offline' to switch to Offline Mode",
                dialogConfirmText = "Offline",
                dialogDismissText = "Retry",
                onRetryClick = {
                    videoScreenViewModel.loadRemoteVideoList()
                },
                onSwitchOfflineClick = {
                    videoScreenViewModel.loadDownloadedVideoList()
                })
        }
    }

    // Detail sheet for a single tagged ad.
    openListing?.let { listing ->
        ListingDetailSheet(
            listing = listing,
            nowMillis = nowMillis,
            onDismiss = browseViewModel::closeListing,
            onMessageSeller = { target ->
                browseViewModel.closeListing()
                onOpenConversation(messagesViewModel.startConversation(target))
            },
            onOpenSeller = { sellerId ->
                browseViewModel.closeListing()
                onOpenSeller(sellerId)
            },
        )
    }

    // Everything this video is advertising, when it carries more than one ad.
    listingListSheet?.let { listings ->
        ListingListSheet(
            title = "In this video",
            subtitle = "${listings.size} ads from this seller",
            listings = listings,
            nowMillis = nowMillis,
            onDismiss = { listingListSheet = null },
            onListingClick = { listing ->
                listingListSheet = null
                browseViewModel.openListing(listing)
            },
        )
    }
}

private fun handleDownloadVideoClick(
    notificationPermission: PermissionState,
    onDownload: () -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Handle download video here, but need to check permission first
        if (!notificationPermission.status.isGranted) {
            notificationPermission.launchPermissionRequest()
        } else {
            // Start to download
            onDownload.invoke()
        }
    } else {
        // Start to download
        onDownload.invoke()
    }
}