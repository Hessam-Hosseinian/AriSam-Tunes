package com.arisamtunes.core.navigation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.arisamtunes.R
import com.arisamtunes.feature.home.HomeQuickAction
import com.arisamtunes.feature.downloads.DownloadsRoute
import com.arisamtunes.feature.chat.ChatExperienceRoute
import com.arisamtunes.feature.chat.ChatListRoute
import com.arisamtunes.feature.chat.ChatConnectionViewModel
import com.arisamtunes.feature.chat.ShareSongRoute
import com.arisamtunes.feature.home.HomeRoute
import com.arisamtunes.feature.artist.ArtistRoute
import com.arisamtunes.feature.library.LibraryCollectionRoute
import com.arisamtunes.feature.search.SearchRoute
import com.arisamtunes.feature.playlists.PlaylistDetailRoute
import com.arisamtunes.feature.playlists.PlaylistsRoute
import com.arisamtunes.feature.player.MiniPlayer
import com.arisamtunes.feature.player.ChatMiniPlayer
import com.arisamtunes.feature.player.NowPlayingRoute
import com.arisamtunes.feature.player.PlayerViewModel
import com.arisamtunes.feature.player.RhythmGameRoute
import com.arisamtunes.feature.settings.SettingsRoute
import com.arisamtunes.feature.songdetail.SongDetailRoute
import com.arisamtunes.feature.social.SocialProfileRoute
import com.arisamtunes.feature.social.SocialProfileViewModel
import com.arisamtunes.feature.social.SocialUsersRoute
import coil3.compose.AsyncImage

private const val SettingsRoutePath = "settings"
private const val PlaylistDetailRoutePattern = "playlist/{playlistId}"
private const val SongDetailRoutePattern = "song/{songId}"
private const val NowPlayingRoutePath = "now-playing"
private const val RhythmGameRoutePath = "rhythm-game"
private const val UserProfileRoutePattern = "social/user/{userId}"
private const val UserListRoutePattern = "social/users/{userId}/{kind}"
private const val ChatDetailRoutePattern = "chat/{userId}"
private const val ShareSongRoutePattern = "chat/share/{songId}"
private const val LibraryCollectionRoutePattern = "library/{kind}"
private const val ArtistRoutePattern = "artist/{artistId}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AriSamAppShell(onLoggedOut: () -> Unit) {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()
    val chatConnectionViewModel: ChatConnectionViewModel = hiltViewModel()
    val currentUserProfileViewModel: SocialProfileViewModel = hiltViewModel()
    val currentUserProfileState by currentUserProfileViewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(chatConnectionViewModel, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> chatConnectionViewModel.start()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) chatConnectionViewModel.start()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            chatConnectionViewModel.stop()
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentMainDestination = currentRoute?.mainDestination()
    val showMainChrome = currentRoute in MainDestinations.map(AppDestination::route)
    val showHomeBrand = currentMainDestination == AppDestination.Home
    val isFullscreenDestination = currentRoute == NowPlayingRoutePath ||
        currentRoute == RhythmGameRoutePath ||
        currentRoute == ChatDetailRoutePattern

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showMainChrome) {
                TopAppBar(
                    title = {
                        if (showHomeBrand) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF4FBFF)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.arisam_mark_dark),
                                        contentDescription = stringResource(R.string.app_logo_description),
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        stringResource(R.string.home_welcome),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF8ED8FF),
                                    )
                                }
                            }
                        } else {
                            Column {
                                Text(
                                    currentMainDestination?.let { stringResource(it.labelRes) }
                                        ?: stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (showHomeBrand) Color(0xFF081721) else MaterialTheme.colorScheme.background,
                        titleContentColor = if (showHomeBrand) Color.White else MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = if (showHomeBrand) Color(0xFFB9E8FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    actions = {
                        IconButton(onClick = { navController.navigateSingleTop(SettingsRoutePath) }) {
                            Icon(Icons.Rounded.Settings, stringResource(R.string.settings))
                        }
                        HeaderProfileAction(
                            avatarUrl = currentUserProfileState.user?.avatarUrl,
                            onClick = { navController.navigateTopLevel(AppDestination.Profile.route) },
                        )
                    },
                )
            }
        },
        bottomBar = {
            if (showMainChrome) {
                Column {
                    MiniPlayer(onOpen = navController::openNowPlaying)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                    ) {
                        MainDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentMainDestination == destination,
                                onClick = { navController.navigateTopLevel(destination.route) },
                                icon = { Icon(destination.icon, null) },
                                label = { Text(stringResource(destination.labelRes)) },
                            )
                        }
                    }
                }
            }
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .then(
                    if (isFullscreenDestination) Modifier
                    else Modifier.padding(contentPadding),
                ),
        ) {
            NavHost(
                navController = navController,
                startDestination = AppDestination.Home.route,
            ) {
                composable(AppDestination.Home.route) {
                    HomeRoute(
                        onSongClick = {
                            playerViewModel.play(it)
                            navController.navigateSingleTop(NowPlayingRoutePath)
                        },
                        onPlaylistClick = { navController.navigate(playlistRoute(it.id)) },
                        onQuickAction = { action ->
                            when (action) {
                                HomeQuickAction.Playlists -> navController.navigateTopLevel(AppDestination.Playlists.route)
                                HomeQuickAction.Artists -> navController.navigate(artistRoute(DefaultArtistId))
                                HomeQuickAction.Liked -> navController.navigate(libraryRoute("liked"))
                                HomeQuickAction.Recent -> navController.navigate(libraryRoute("recent"))
                            }
                        },
                    )
                }
                composable(AppDestination.Search.route) {
                    SearchRoute(onSongClick = {
                        playerViewModel.play(it)
                        navController.navigateSingleTop(NowPlayingRoutePath)
                    }, onArtistClick = { navController.navigate(artistRoute(it)) })
                }
                composable(AppDestination.Playlists.route) {
                    PlaylistsRoute(onPlaylistClick = { navController.navigate(playlistRoute(it.id)) })
                }
                composable(AppDestination.Downloads.route) {
                    DownloadsRoute(onSongClick = { playerViewModel.play(it) })
                }
                composable(AppDestination.Chat.route) {
                    ChatListRoute(
                        onConversationClick = { navController.navigate(chatRoute(it)) },
                        onUserProfileClick = { navController.navigate(userProfileRoute(it)) },
                    )
                }
                composable(
                    route = ChatDetailRoutePattern,
                    deepLinks = listOf(navDeepLink { uriPattern = "arisamtunes://chat/{userId}" }),
                ) {
                    val navigateBack = { navController.backFromChatDetail() }
                    BackHandler(onBack = navigateBack)
                    ChatExperienceRoute(
                        onBack = navigateBack,
                        onPeerProfileClick = { navController.navigate(userProfileRoute(it)) },
                        onPlaySong = { song, chatQueue -> playerViewModel.play(song, chatQueue) },
                        currentSong = playerState.currentSong,
                        miniPlayer = { chatSongIds ->
                            if (playerState.currentSong?.id in chatSongIds) {
                                ChatMiniPlayer(onOpen = navController::openNowPlaying)
                            }
                        },
                    )
                }
                composable(ShareSongRoutePattern) {
                    ShareSongRoute(onBack = { navController.backOrNavigateTopLevel(AppDestination.Home.route) })
                }
                composable(PlaylistDetailRoutePattern) {
                    PlaylistDetailRoute(onBack = { navController.backOrNavigateTopLevel(AppDestination.Playlists.route) }, onSongClick = { song, queue ->
                        playerViewModel.play(song, queue)
                        navController.openNowPlaying()
                    })
                }
                composable(LibraryCollectionRoutePattern) {
                    LibraryCollectionRoute(
                        onBack = { navController.backOrNavigateTopLevel(AppDestination.Home.route) },
                        onSongClick = {
                            navController.navigate(songRoute(it))
                        },
                    )
                }
                composable(ArtistRoutePattern) {
                    ArtistRoute(
                        onBack = { navController.backOrNavigateTopLevel(AppDestination.Home.route) },
                        onArtistClick = { navController.navigate(artistRoute(it)) },
                        onSongClick = { navController.navigate(songRoute(it)) },
                    )
                }
                composable(SongDetailRoutePattern) {
                    SongDetailRoute(
                        onBack = { navController.backOrNavigateTopLevel(AppDestination.Home.route) },
                        onPlay = {
                            playerViewModel.play(it)
                            navController.openNowPlaying()
                        },
                        onShare = { navController.navigate(shareSongRoute(it.id)) },
                    )
                }
                composable(NowPlayingRoutePath) {
                    NowPlayingRoute(
                        onBack = { navController.backOrNavigateTopLevel(AppDestination.Home.route) },
                        onShowSongInfo = { navController.navigate(songRoute(it)) },
                        onArtistClick = { navController.navigate(artistRoute(it)) },
                        onShareSong = { navController.navigate(shareSongRoute(it)) },
                        onOpenRhythmGame = { navController.navigateSingleTop(RhythmGameRoutePath) },
                    )
                }
                composable(RhythmGameRoutePath) {
                    RhythmGameRoute(
                        onBack = {
                            if (!navController.popBackStack()) {
                                navController.navigateSingleTop(NowPlayingRoutePath)
                            }
                        },
                    )
                }
                composable(AppDestination.Profile.route) {
                    SocialProfileRoute(
                        onFollowersClick = { navController.navigate(userListRoute(it, "followers")) },
                        onFollowingClick = { navController.navigate(userListRoute(it, "following")) },
                        onPlaylistClick = { navController.navigate(playlistRoute(it.id)) },
                        onMessageClick = { navController.navigate(chatRoute(it)) },
                        viewModel = currentUserProfileViewModel,
                    )
                }
                composable(UserProfileRoutePattern) {
                    SocialProfileRoute(
                        onBack = { navController.backOrNavigateTopLevel(AppDestination.Profile.route) },
                        onFollowersClick = { navController.navigate(userListRoute(it, "followers")) },
                        onFollowingClick = { navController.navigate(userListRoute(it, "following")) },
                        onPlaylistClick = { navController.navigate(playlistRoute(it.id)) },
                        onMessageClick = { navController.navigate(chatRoute(it)) },
                    )
                }
                composable(UserListRoutePattern) {
                    SocialUsersRoute(
                        onBack = { navController.backOrNavigateTopLevel(AppDestination.Profile.route) },
                        onUserClick = { navController.navigate(userProfileRoute(it.id)) },
                    )
                }
                composable(SettingsRoutePath) {
                    SettingsRoute(
                        onBack = { navController.backOrNavigateTopLevel(AppDestination.Home.route) },
                        onLoggedOut = onLoggedOut,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderProfileAction(
    avatarUrl: String?,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUrl.isNullOrBlank()) {
                Icon(
                    Icons.Rounded.AccountCircle,
                    stringResource(R.string.profile_picture),
                    modifier = Modifier.size(27.dp),
                )
            } else {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = stringResource(R.string.profile_picture),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private fun NavController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

private fun NavController.openNowPlaying() {
    if (!popBackStack(NowPlayingRoutePath, inclusive = false)) {
        navigateSingleTop(NowPlayingRoutePath)
    }
}

private fun NavController.backOrNavigateTopLevel(fallbackRoute: String) {
    if (!popBackStack()) navigateTopLevel(fallbackRoute)
}

private fun NavController.backFromChatDetail() {
    val previousRoute = previousBackStackEntry?.destination?.route
    if (previousRoute == null || previousRoute == AppDestination.Home.route) {
        navigateTopLevel(AppDestination.Chat.route)
    } else {
        popBackStack()
    }
}

private fun String.mainDestination(): AppDestination? = when (this) {
    AppDestination.Home.route -> AppDestination.Home
    AppDestination.Search.route -> AppDestination.Search
    AppDestination.Downloads.route -> AppDestination.Downloads
    AppDestination.Playlists.route -> AppDestination.Playlists
    AppDestination.Chat.route -> AppDestination.Chat
    AppDestination.Profile.route -> AppDestination.Profile
    else -> null
}

private fun songRoute(songId: String) = "song/${Uri.encode(songId)}"
private fun playlistRoute(playlistId: String) = "playlist/${Uri.encode(playlistId)}"
private fun chatRoute(userId: String) = "chat/${Uri.encode(userId)}"
private fun shareSongRoute(songId: String) = "chat/share/${Uri.encode(songId)}"
private fun userProfileRoute(userId: String) = "social/user/${Uri.encode(userId)}"
private fun userListRoute(userId: String, kind: String) = "social/users/${Uri.encode(userId)}/${Uri.encode(kind)}"
private fun libraryRoute(kind: String) = "library/${Uri.encode(kind)}"
private fun artistRoute(artistId: String) = "artist/${Uri.encode(artistId)}"

private const val DefaultArtistId = "7yOZJB"
