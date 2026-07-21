package com.arisamtunes.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.arisamtunes.data.local.entity.FollowedArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowedArtistDao {
    @Query("SELECT * FROM followed_artists ORDER BY followedAt DESC")
    fun observeAll(): Flow<List<FollowedArtistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM followed_artists WHERE artistId = :artistId)")
    fun observeIsFollowed(artistId: String): Flow<Boolean>

    @Upsert
    suspend fun follow(artist: FollowedArtistEntity)

    @Query("DELETE FROM followed_artists WHERE artistId = :artistId")
    suspend fun unfollow(artistId: String)
}
