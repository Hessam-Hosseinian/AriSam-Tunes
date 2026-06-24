package com.arisamtunes.data.social

import com.arisamtunes.data.auth.UserDto
import com.arisamtunes.data.catalog.CatalogUrlNormalizer
import com.arisamtunes.data.catalog.PlaylistDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(private val client: HttpClient) {
    suspend fun currentUser(): PublicUserDto {
        val me = client.get("auth/me").body<UserDto>()
        return user(me.id)
    }

    suspend fun user(id: String): PublicUserDto = client.get("users/$id").body()

    suspend fun searchUsers(query: String, page: Int = 0, size: Int = 30): List<PublicUserDto> =
        client.get("users/search") {
            parameter("q", query)
            parameter("page", page)
            parameter("size", size)
        }.body<PublicUserListDto>().items

    suspend fun following(userId: String? = null, page: Int = 0, size: Int = 50): List<PublicUserDto> =
        client.get(if (userId == null) "users/me/following" else "users/$userId/following") {
            parameter("page", page)
            parameter("size", size)
        }.body<PublicUserListDto>().items

    suspend fun followers(userId: String? = null, page: Int = 0, size: Int = 50): List<PublicUserDto> =
        client.get(if (userId == null) "users/me/followers" else "users/$userId/followers") {
            parameter("page", page)
            parameter("size", size)
        }.body<PublicUserListDto>().items

    suspend fun follow(userId: String): PublicUserDto = client.post("users/$userId/follow").body<FollowDto>().user

    suspend fun unfollow(userId: String): PublicUserDto = client.delete("users/$userId/follow").body<FollowDto>().user

    suspend fun publicPlaylists(userId: String): List<PlaylistDto> =
        client.get("users/$userId/playlists").body<PublicPlaylistListDto>().items.map(CatalogUrlNormalizer::playlist)
}
