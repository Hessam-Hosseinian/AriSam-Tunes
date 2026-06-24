package com.arisamtunes.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.arisamtunes.R

sealed class AppDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    data object Home : AppDestination("home", R.string.nav_home, Icons.Rounded.Home)
    data object Search : AppDestination("search", R.string.nav_search, Icons.Rounded.Search)
    data object Downloads : AppDestination("downloads", R.string.nav_downloads, Icons.Rounded.Download)
    data object Playlists : AppDestination("playlists", R.string.nav_playlists, Icons.Rounded.LibraryMusic)
    data object Chat : AppDestination("chat", R.string.nav_chat, Icons.Rounded.Chat)
    data object Profile : AppDestination("profile", R.string.nav_profile, Icons.Rounded.Person)
}

val MainDestinations = listOf(
    AppDestination.Home,
    AppDestination.Search,
    AppDestination.Downloads,
    AppDestination.Playlists,
    AppDestination.Chat,
    AppDestination.Profile,
)
