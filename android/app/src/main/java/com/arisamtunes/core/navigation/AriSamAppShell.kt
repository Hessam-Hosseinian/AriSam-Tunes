package com.arisamtunes.core.navigation

import androidx.compose.foundation.layout.Box
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
import com.arisamtunes.feature.home.HomeRoute
import com.arisamtunes.feature.search.SearchRoute

private const val SettingsRoute = "settings"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AriSamAppShell() {
    val navController = rememberNavController()
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
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(contentPadding),
        ) {
            composable(AppDestination.Home.route) {
                HomeRoute(
                    onSongClick = { },
                    onPlaylistClick = { navController.navigate(AppDestination.Playlists.route) },
                    onQuickAction = { action ->
                        when (action) {
                            HomeQuickAction.Playlists -> navController.navigate(AppDestination.Playlists.route)
                            HomeQuickAction.Artists -> navController.navigate(AppDestination.Search.route)
                            HomeQuickAction.Liked, HomeQuickAction.Recent -> navController.navigate(AppDestination.Downloads.route)
                        }
                    },
                )
            }
            composable(AppDestination.Search.route) { SearchRoute(onSongClick = { }) }
            MainDestinations.filterNot { it == AppDestination.Home || it == AppDestination.Search }.forEach { destination ->
                composable(destination.route) { DestinationPlaceholder(destination.labelRes) }
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
