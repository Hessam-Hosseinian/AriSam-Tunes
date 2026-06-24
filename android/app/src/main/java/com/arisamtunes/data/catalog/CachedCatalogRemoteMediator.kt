package com.arisamtunes.data.catalog

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.arisamtunes.data.local.AriSamDatabase
import com.arisamtunes.data.local.dao.RemoteKeyDao
import com.arisamtunes.data.local.entity.CachedSongEntity
import com.arisamtunes.data.local.entity.RemoteKeyEntity
import com.arisamtunes.data.local.toCachedSongEntity

@OptIn(ExperimentalPagingApi::class)
class CachedCatalogRemoteMediator(
    private val scope: String,
    private val cacheKey: String,
    private val database: AriSamDatabase,
    private val remoteKeyDao: RemoteKeyDao,
    private val loadPage: suspend (page: Int, size: Int) -> SongPageDto,
) : RemoteMediator<Int, CachedSongEntity>() {
    override suspend fun load(loadType: LoadType, state: PagingState<Int, CachedSongEntity>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 0
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> remoteKeyDao.latestFor(scope, cacheKey)?.nextKey
                ?: return MediatorResult.Success(endOfPaginationReached = true)
        }
        return runCatching {
            val pageSize = state.config.pageSize.coerceAtLeast(1)
            val response = loadPage(page, pageSize)
            val now = System.currentTimeMillis()
            val endReached = page + 1 >= response.pagination.totalPages
            database.withTransaction {
                if (loadType == LoadType.REFRESH) remoteKeyDao.clear(scope, cacheKey)
                database.cachedSongDao().upsertAll(response.items.map { it.toCachedSongEntity(now) })
                remoteKeyDao.upsertAll(response.items.map { song ->
                    RemoteKeyEntity(
                        scope = scope,
                        cacheKey = cacheKey,
                        itemId = song.id,
                        prevKey = if (page == 0) null else page - 1,
                        currentPage = page,
                        nextKey = if (endReached) null else page + 1,
                        createdAt = now,
                    )
                })
            }
            MediatorResult.Success(endOfPaginationReached = endReached || response.items.isEmpty())
        }.getOrElse { error -> MediatorResult.Error(error) }
    }
}
