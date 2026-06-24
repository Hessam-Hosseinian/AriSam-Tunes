package com.arisamtunes.social

import com.arisamtunes.model.PaginationMeta
import com.arisamtunes.playlist.PlaylistResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicUserResponse(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String?,
    val bio: String?,
    @SerialName("is_premium") val isPremium: Boolean,
    @SerialName("followers_count") val followersCount: Long,
    @SerialName("following_count") val followingCount: Long,
    @SerialName("is_following") val isFollowing: Boolean,
)

@Serializable
data class PublicUserListResponse(
    val items: List<PublicUserResponse>,
    val pagination: PaginationMeta,
)

@Serializable
data class FollowResponse(val user: PublicUserResponse)

@Serializable
data class PublicPlaylistListResponse(
    val owner: PublicUserResponse,
    val items: List<PlaylistResponse>,
)
