package com.arisamtunes.data.social

import com.arisamtunes.data.catalog.PlaylistDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicUserDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerialName("is_premium") val isPremium: Boolean = false,
    @SerialName("followers_count") val followersCount: Long = 0,
    @SerialName("following_count") val followingCount: Long = 0,
    @SerialName("is_following") val isFollowing: Boolean = false,
)

@Serializable
data class PublicUserListDto(
    val items: List<PublicUserDto>,
)

@Serializable
data class FollowDto(val user: PublicUserDto)

@Serializable
data class PublicPlaylistListDto(
    val owner: PublicUserDto,
    val items: List<PlaylistDto>,
)
