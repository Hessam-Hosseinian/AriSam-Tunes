package com.arisamtunes.data.local

import com.arisamtunes.data.local.dao.FollowedArtistDao
import com.arisamtunes.data.local.entity.FollowedArtistEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FollowedArtistsRepository @Inject constructor(
    private val dao: FollowedArtistDao,
) {
    fun observeAll() = dao.observeAll()

    fun observeIsFollowed(artistId: String) = dao.observeIsFollowed(artistId)

    suspend fun setFollowed(artistId: String, name: String, imageUri: String, followed: Boolean) {
        if (followed) {
            dao.follow(
                FollowedArtistEntity(
                    artistId = artistId,
                    name = name,
                    imageUri = imageUri,
                    followedAt = System.currentTimeMillis(),
                ),
            )
        } else {
            dao.unfollow(artistId)
        }
    }
}
