package com.arisamtunes.core.navigation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arisamtunes.R
import com.arisamtunes.feature.home.HomeQuickAction
import com.arisamtunes.feature.downloads.DownloadsRoute
import com.arisamtunes.feature.chat.ChatDetailRoute
import com.arisamtunes.feature.chat.ChatListRoute
import com.arisamtunes.feature.home.HomeRoute
import com.arisamtunes.feature.library.LibraryCollectionRoute
import com.arisamtunes.feature.search.SearchRoute
import com.arisamtunes.feature.playlists.PlaylistDetailRoute
import com.arisamtunes.feature.playlists.PlaylistsRoute
import com.arisamtunes.feature.player.MiniPlayer
import com.arisamtunes.feature.player.NowPlayingRoute
import com.arisamtunes.feature.player.PlayerViewModel
import com.arisamtunes.feature.settings.SettingsRoute
import com.arisamtunes.feature.songdetail.SongDetailRoute
import com.arisamtunes.feature.social.SocialProfileRoute
import com.arisamtunes.feature.social.SocialUsersRoute

private const val SettingsRoutePath = "settings"
private const val PlaylistDetailRoutePattern = "playlist/{playlistId}"
private const val SongDetailRoutePattern = "song/{songId}"
private const val NowPlayingRoutePath = "now-playing"
private const val UserProfileRoutePattern = "social/user/{userId}"
private const val UserListRoutePattern = "social/users/{userId}/{kind}"
private const val ChatDetailRoutePattern = "chat/{userId}"
private const val LibraryCollectionRoutePattern = "library/{kind}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AriSamAppShell() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentMainDestination = currentRoute?.mainDestination()
    val showMainChrome = currentRoute in MainDestinations.map(AppDestination::route)

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            if (showMainChrome) {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = com.arisamtunes.core.design.theme.AriSamThemeTokens.spacing.sm)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = .16f))
                                .padding(com.arisamtunes.core.design.theme.AriSamThemeTokens.spacing.sm),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = stringResource(R.string.app_logo_description),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .88f),
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    actions = {
                        IconButton(onClick = { }) {
                            Icon(Icons.Rounded.NotificationsNone, stringResource(R.string.notifications))
                        }
                        IconButton(onClick = { navController.navigateSingleTop(SettingsRoutePath) }) {
                            Icon(Icons.Rounded.Settings, stringResource(R.string.settings))
                        }
                        IconButton(onClick = { navController.navigateTopLevel(AppDestination.Profile.route) }) {
                            Icon(Icons.Rounded.AccountCircle, stringResource(R.string.profile_picture))
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showMainChrome) {
                Column {
                    MiniPlayer(onOpen = { navController.navigate(NowPlayingRoutePath) })
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .94f),
                        tonalElevation = NavigationBarDefaults.Elevation,
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
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = .07f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.secondary.copy(alpha = .05f),
                        ),
                    ),
                )
                .padding(contentPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = AppDestination.Home.route,
            ) {
                composable(AppDestination.Home.route) {
                    HomeRoute(
                        onSongClick = { navController.navigate(songRoute(it.id)) },
                        onPlaylistClick = { navController.navigate(playlistRoute(it.id)) },
                        onQuickAction = { action ->
                            when (action) {
                                HomeQuickAction.Playlists -> navController.navigateTopLevel(AppDestination.Playlists.route)
                                HomeQuickAction.Artists -> navController.navigateTopLevel(AppDestination.Search.route)
                                HomeQuickAction.Liked -> navController.navigate(libraryRoute("liked"))
                                HomeQuickAction.Recent -> navController.navigate(libraryRoute("recent"))
                            }
                        },
                    )
                }
                composable(AppDestination.Search.route) { SearchRoute(onSongClick = { navController.navigate(songRoute(it.id)) }) }
                composable(AppDestination.Playlists.route) {
                    PlaylistsRoute(onPlaylistClick = { navController.navigate(playlistRoute(it.id)) })
                }
                composable(AppDestination.Downloads.route) { DownloadsRoute() }
                composable(AppDestination.Chat.route) {
                    ChatListRoute(onConversationClick = { navController.navigate(chatRoute(it)) })
                }
                composable(ChatDetailRoutePattern) {
                    ChatDetailRoute(onBack = navController::popBackStack)
                }
                composable(PlaylistDetailRoutePattern) {
                    PlaylistDetailRoute(onBack = navController::popBackStack, onSongClick = { navController.navigate(songRoute(it.id)) })
                }
                composable(LibraryCollectionRoutePattern) {
                    LibraryCollectionRoute(
                        onBack = navController::popBackStack,
                        onSongClick = { navController.navigate(songRoute(it)) },
                    )
                }
                composable(SongDetailRoutePattern) {
                    SongDetailRoute(
                        onBack = navController::popBackStack,
                        onPlay = {
                            playerViewModel.play(it)
                            navController.navigate(NowPlayingRoutePath)
                        },
                    )
                }
                composable(NowPlayingRoutePath) {
                    NowPlayingRoute(onBack = navController::popBackStack)
                }
                composable(AppDestination.Profile.route) {
                    SocialProfileRoute(
                        onFollowersClick = { navController.navigate(userListRoute(it, "followers")) },
                        onFollowingClick = { navController.navigate(userListRoute(it, "following")) },
                        onPlaylistClick = { navController.navigate(playlistRoute(it.id)) },
                    )
                }
                composable(UserProfileRoutePattern) {
                    SocialProfileRoute(
                        onBack = navController::popBackStack,
                        onFollowersClick = { navController.navigate(userListRoute(it, "followers")) },
                        onFollowingClick = { navController.navigate(userListRoute(it, "following")) },
                        onPlaylistClick = { navController.navigate(playlistRoute(it.id)) },
                    )
                }
                composable(UserListRoutePattern) {
                    SocialUsersRoute(
                        onBack = navController::popBackStack,
                        onUserClick = { navController.navigate(userProfileRoute(it.id)) },
                    )
                }
                composable(SettingsRoutePath) { SettingsRoute(onBack = navController::popBackStack) }
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
private fun userProfileRoute(userId: String) = "social/user/${Uri.encode(userId)}"
private fun userListRoute(userId: String, kind: String) = "social/users/${Uri.encode(userId)}/${Uri.encode(kind)}"
private fun libraryRoute(kind: String) = "library/${Uri.encode(kind)}"
