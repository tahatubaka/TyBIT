package com.tybit

import com.tybit.data.model.ArtistRef
import com.tybit.data.model.Song
import com.tybit.data.settings.HiddenMusic
import com.tybit.data.stats.dayPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenMusicTest {

    private val duet = Song(
        videoId = "v1",
        title = "A",
        artist = "Cheema Y & Gur Sidhu",
        thumbnailUrl = null,
        artists = listOf(ArtistRef("Cheema Y", "UC1"), ArtistRef("Gur Sidhu", "UC2")),
    )
    private val other = Song(videoId = "v2", title = "B", artist = "Someone", thumbnailUrl = null)

    @Test
    fun `a hidden song or artist is passed over and nothing else is`() {
        assertFalse(HiddenMusic.isHidden(duet))

        HiddenMusic.hideSong(duet, days = 30)
        assertTrue(HiddenMusic.isHidden(duet))
        assertFalse(HiddenMusic.isHidden(other))
        HiddenMusic.unhide("v1")
        assertFalse(HiddenMusic.isHidden(duet))

        // Hiding Gur Sidhu from one of his own tracks also hides a duet he is on.
        val solo = Song(videoId = "v3", title = "C", artist = "Gur Sidhu", thumbnailUrl = null,
            artists = listOf(ArtistRef("Gur Sidhu", "UC2")))
        assertEquals("Gur Sidhu", HiddenMusic.hideArtist(solo))
        assertTrue(HiddenMusic.isHidden(duet))
        assertFalse(HiddenMusic.isHidden(other))
        HiddenMusic.entries.value.forEach { HiddenMusic.unhide(it.id) }
    }

    @Test
    fun `hours fall into the daylist's five parts`() {
        assertEquals(listOf(4, 0, 0, 1, 1, 2, 2, 3, 3, 4), listOf(4, 5, 10, 11, 14, 15, 17, 18, 22, 23).map(::dayPart))
    }
}
