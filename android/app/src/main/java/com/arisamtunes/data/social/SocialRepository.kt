package com.arisamtunes.data.social

import com.arisamtunes.data.auth.UserDto
import com.arisamtunes.data.catalog.CatalogUrlNormalizer
import com.arisamtunes.data.catalog.PlaylistDto
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import javax.inject.Inject
import javax.inject.Singleton
import com.arisamtunes.data.local.dao.CachedUserProfileDao
import com.arisamtunes.data.local.entity.CachedUserProfileEntity

@Singleton
class SocialRepository @Inject constructor(
    private val client: HttpClient,
    private val cachedUserProfileDao: CachedUserProfileDao,
) {
    suspend fun currentUser(): PublicUserDto {
        val me = client.get("auth/me").body<UserDto>()
        return user(me.id)
    }

    suspend fun user(id: String): PublicUserDto = runCatching { client.get("users/$id").body<PublicUserDto>() }
        .fold(
            onSuccess = { user -> cachedUserProfileDao.upsert(user.toCacheEntity()); user },
            onFailure = { error -> cachedUserProfileDao.profile(id)?.toPublicUser() ?: throw error },
        )

    suspend fun searchUsers(query: String, page: Int = 0, size: Int = 30): List<PublicUserDto> =
        client.get("users/search") {
            parameter("q", query)
            parameter("page", page)
            parameter("size", size)
        }.body<PublicUserListDto>().items

    fun searchUsersPager(query: String) = Pager(
        config = PagingConfig(pageSize = 30, initialLoadSize = 30, prefetchDistance = 8),
        pagingSourceFactory = { SocialPagingSource { page, size -> searchUsersPage(query, page, size) } },
    ).flow

    suspend fun following(userId: String? = null, page: Int = 0, size: Int = 50): List<PublicUserDto> =
        client.get(if (userId == null) "users/me/following" else "users/$userId/following") {
            parameter("page", page)
            parameter("size", size)
        }.body<PublicUserListDto>().items

    fun followingPager(userId: String? = null) = Pager(
        config = PagingConfig(pageSize = 30, initialLoadSize = 30, prefetchDistance = 8),
        pagingSourceFactory = { SocialPagingSource { page, size -> followingPage(userId, page, size) } },
    ).flow

    suspend fun followers(userId: String? = null, page: Int = 0, size: Int = 50): List<PublicUserDto> =
        client.get(if (userId == null) "users/me/followers" else "users/$userId/followers") {
            parameter("page", page)
            parameter("size", size)
        }.body<PublicUserListDto>().items

    fun followersPager(userId: String? = null) = Pager(
        config = PagingConfig(pageSize = 30, initialLoadSize = 30, prefetchDistance = 8),
        pagingSourceFactory = { SocialPagingSource { page, size -> followersPage(userId, page, size) } },
    ).flow

    suspend fun follow(userId: String): PublicUserDto = client.post("users/$userId/follow").body<FollowDto>().user.also {
        cachedUserProfileDao.upsert(it.toCacheEntity())
    }

    suspend fun unfollow(userId: String): PublicUserDto = client.delete("users/$userId/follow").body<FollowDto>().user.also {
        cachedUserProfileDao.upsert(it.toCacheEntity())
    }

    suspend fun publicPlaylists(userId: String): List<PlaylistDto> =
        client.get("users/$userId/playlists") {
            parameter("size", 100)
        }.body<PublicPlaylistListDto>().items.map(CatalogUrlNormalizer::playlist)

    fun publicPlaylistsPager(userId: String) = Pager(
        config = PagingConfig(pageSize = 20, initialLoadSize = 20, prefetchDistance = 6),
        pagingSourceFactory = { PlaylistPagingSource(userId, this) },
    ).flow

    private suspend fun searchUsersPage(query: String, page: Int, size: Int): PublicUserListDto = client.get("users/search") {
        parameter("q", query)
        parameter("page", page)
        parameter("size", size)
    }.body<PublicUserListDto>().also { cachedUserProfileDao.upsertAll(it.items.map(PublicUserDto::toCacheEntity)) }

    private suspend fun followingPage(userId: String?, page: Int, size: Int): PublicUserListDto =
        client.get(if (userId == null) "users/me/following" else "users/$userId/following") {
            parameter("page", page); parameter("size", size)
        }.body<PublicUserListDto>().also { cachedUserProfileDao.upsertAll(it.items.map(PublicUserDto::toCacheEntity)) }

    private suspend fun followersPage(userId: String?, page: Int, size: Int): PublicUserListDto =
        client.get(if (userId == null) "users/me/followers" else "users/$userId/followers") {
            parameter("page", page); parameter("size", size)
        }.body<PublicUserListDto>().also { cachedUserProfileDao.upsertAll(it.items.map(PublicUserDto::toCacheEntity)) }

    private suspend fun publicPlaylistsPage(userId: String, page: Int, size: Int): PublicPlaylistListDto =
        client.get("users/$userId/playlists") { parameter("page", page); parameter("size", size) }.body()

    private class SocialPagingSource(
        private val loadPage: suspend (Int, Int) -> PublicUserListDto,
    ) : PagingSource<Int, PublicUserDto>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PublicUserDto> {
            val page = params.key ?: 0
            return runCatching { loadPage(page, params.loadSize.coerceAtMost(100)) }.fold(
                onSuccess = { response -> LoadResult.Page(response.items, if (page == 0) null else page - 1, if (page + 1 < response.pagination.totalPages) page + 1 else null) },
                onFailure = { error -> LoadResult.Error<Int, PublicUserDto>(error) },
            )
        }
        override fun getRefreshKey(state: PagingState<Int, PublicUserDto>): Int? = state.anchorPosition?.let { position ->
            state.closestPageToPosition(position)?.let { it.prevKey?.plus(1) ?: it.nextKey?.minus(1) }
        }
    }

    private class PlaylistPagingSource(
        private val userId: String,
        private val repository: SocialRepository,
    ) : PagingSource<Int, PlaylistDto>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PlaylistDto> {
            val page = params.key ?: 0
            return runCatching { repository.publicPlaylistsPage(userId, page, params.loadSize.coerceAtMost(100)) }.fold(
                onSuccess = { response -> LoadResult.Page(response.items.map(CatalogUrlNormalizer::playlist), if (page == 0) null else page - 1, if (page + 1 < response.pagination.totalPages) page + 1 else null) },
                onFailure = { error -> LoadResult.Error<Int, PlaylistDto>(error) },
            )
        }
        override fun getRefreshKey(state: PagingState<Int, PlaylistDto>): Int? = state.anchorPosition?.let { position ->
            state.closestPageToPosition(position)?.let { it.prevKey?.plus(1) ?: it.nextKey?.minus(1) }
        }
    }
}

private fun PublicUserDto.toCacheEntity() = CachedUserProfileEntity(
    userId = id,
    displayName = displayName,
    avatarUrl = avatarUrl,
    bio = bio,
    followerCount = followersCount,
    followingCount = followingCount,
    isFollowing = isFollowing,
    cachedAt = System.currentTimeMillis(),
)

private fun CachedUserProfileEntity.toPublicUser() = PublicUserDto(
    id = userId,
    displayName = displayName,
    avatarUrl = avatarUrl,
    bio = bio,
    followersCount = followerCount,
    followingCount = followingCount,
    isFollowing = isFollowing,
)
