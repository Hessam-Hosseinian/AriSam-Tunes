package com.arisamtunes.feature.social

import com.arisamtunes.data.social.PublicUserDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialFollowStateTest {
    @Test
    fun `optimistic follow increments followers and marks user followed`() {
        val updated = user(isFollowing = false, followers = 9).optimisticFollowToggle()

        assertTrue(updated.isFollowing)
        assertEquals(10, updated.followersCount)
    }

    @Test
    fun `optimistic unfollow decrements followers without going negative`() {
        val updated = user(isFollowing = true, followers = 0).optimisticFollowToggle()

        assertFalse(updated.isFollowing)
        assertEquals(0, updated.followersCount)
    }

    @Test
    fun `follow then unfollow restores original relationship state`() {
        val original = user(isFollowing = false, followers = 21)

        assertEquals(original, original.optimisticFollowToggle().optimisticFollowToggle())
    }

    private fun user(isFollowing: Boolean, followers: Long) = PublicUserDto(
        id = "listener-id",
        displayName = "Listener",
        followersCount = followers,
        isFollowing = isFollowing,
    )
}
