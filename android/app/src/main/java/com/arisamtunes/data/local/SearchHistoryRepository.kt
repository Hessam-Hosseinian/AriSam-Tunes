package com.arisamtunes.data.local

import com.arisamtunes.data.local.dao.SearchHistoryDao
import com.arisamtunes.data.local.entity.SearchHistoryEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SearchHistoryEntry(
    val id: Long,
    val query: String,
    val filter: String,
)

@Singleton
class SearchHistoryRepository @Inject constructor(
    private val dao: SearchHistoryDao,
) {
    fun observeRecent(limit: Int = RecentSearchLimit): Flow<List<SearchHistoryEntry>> =
        dao.observeRecent(limit).map { entries -> entries.map { it.toEntry() } }

    suspend fun record(query: String, filter: String, resultCount: Long? = null) {
        val displayQuery = query.trim().takeIf(String::isNotEmpty) ?: return
        dao.record(
            SearchHistoryEntity(
                query = displayQuery,
                filter = filter,
                resultCount = resultCount,
                lastSearchedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun clear() = dao.clear()

    private fun SearchHistoryEntity.toEntry() = SearchHistoryEntry(
        id = id,
        query = query,
        filter = filter,
    )

    private companion object {
        const val RecentSearchLimit = 20
    }
}
