package com.arisamtunes.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.arisamtunes.feature.search.SearchRoute
import com.arisamtunes.feature.playlists.PlaylistDetailRoute
import com.arisamtunes.feature.playlists.PlaylistsRoute
import com.arisamtunes.feature.player.MiniPlayer
import com.arisamtunes.feature.player.NowPlayingRoute
import com.arisamtunes.feature.player.PlayerViewModel
import com.arisamtunes.feature.songdetail.SongDetailRoute
import com.arisamtunes.feature.social.SocialProfileRoute
import com.arisamtunes.feature.social.SocialUsersRoute
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

private const val SettingsRoute = "settings"
private const val PlaylistDetailRoutePattern = "playlist/{playlistId}"
private const val SongDetailRoutePattern = "song/{songId}"
private const val NowPlayingRoutePath = "now-playing"
private const val UserProfileRoutePattern = "social/user/{userId}"
private const val UserListRoutePattern = "social/users/{userId}/{kind}"
private const val ChatDetailRoutePattern = "chat/{userId}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AriSamAppShell() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = stringResource(R.string.app_logo_description),
                    )
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Rounded.NotificationsNone, stringResource(R.string.notifications))
                    }
                    IconButton(onClick = { navController.navigate(SettingsRoute) }) {
                        Icon(Icons.Rounded.Settings, stringResource(R.string.settings))
                    }
                    IconButton(onClick = { navController.navigate(AppDestination.Profile.route) }) {
                        Icon(Icons.Rounded.AccountCircle, stringResource(R.string.profile_picture))
                    }
                },
            )
        },
        bottomBar = {
            if (currentRoute != SettingsRoute) {
                Column {
                    MiniPlayer(onOpen = { navController.navigate(NowPlayingRoutePath) })
                    NavigationBar {
                        MainDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(destination.icon, null) },
                                label = { Text(stringResource(destination.labelRes)) },
                            )
                        }
                    }
                }
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(contentPadding),
        ) {
            composable(AppDestination.Home.route) {
                HomeRoute(
                    onSongClick = { navController.navigate("song/${it.id}") },
                    onPlaylistClick = { navController.navigate("playlist/${it.id}") },
                    onQuickAction = { action ->
                        when (action) {
                            HomeQuickAction.Playlists -> navController.navigate(AppDestination.Playlists.route)
                            HomeQuickAction.Artists -> navController.navigate(AppDestination.Search.route)
                            HomeQuickAction.Liked, HomeQuickAction.Recent -> navController.navigate(AppDestination.Downloads.route)
                        }
                    },
                )
            }
            composable(AppDestination.Search.route) { SearchRoute(onSongClick = { navController.navigate("song/${it.id}") }) }
            composable(AppDestination.Playlists.route) {
                PlaylistsRoute(onPlaylistClick = { navController.navigate("playlist/${it.id}") })
            }
            composable(AppDestination.Downloads.route) { DownloadsRoute() }
            composable(AppDestination.Chat.route) {
                ChatListRoute(onConversationClick = { navController.navigate("chat/$it") })
            }
            composable(ChatDetailRoutePattern) {
                ChatDetailRoute(onBack = navController::popBackStack)
            }
            composable(PlaylistDetailRoutePattern) {
                PlaylistDetailRoute(onBack = navController::popBackStack, onSongClick = { navController.navigate("song/${it.id}") })
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
                    onFollowersClick = { navController.navigate("social/users/$it/followers") },
                    onFollowingClick = { navController.navigate("social/users/$it/following") },
                    onPlaylistClick = { navController.navigate("playlist/${it.id}") },
                )
            }
            composable(UserProfileRoutePattern) {
                SocialProfileRoute(
                    onBack = navController::popBackStack,
                    onFollowersClick = { navController.navigate("social/users/$it/followers") },
                    onFollowingClick = { navController.navigate("social/users/$it/following") },
                    onPlaylistClick = { navController.navigate("playlist/${it.id}") },
                )
            }
            composable(UserListRoutePattern) {
                SocialUsersRoute(
                    onBack = navController::popBackStack,
                    onUserClick = { navController.navigate("social/user/${it.id}") },
                )
            }
            composable(SettingsRoute) { DestinationPlaceholder(R.string.settings) }
        }
    }
}

@Composable
private fun DestinationPlaceholder(@androidx.annotation.StringRes labelRes: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(labelRes))
    }
}
