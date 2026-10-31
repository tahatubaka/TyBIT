package com.tybit

import com.tybit.data.model.Song
import com.tybit.data.settings.CustomOrder
import com.tybit.data.settings.FOLDER_PREFIX
import com.tybit.data.settings.PlayMode
import com.tybit.data.settings.PlaylistFolders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryToolsTest {

    private fun song(id: String) = Song(videoId = id, title = id, artist = "x", thumbnailUrl = null)

    @Test
    fun `a dragged order is kept, and songs added since follow it`() {
        val page = listOf(song("a"), song("b"), song("c"))
        CustomOrder.set("PL1", listOf(song("c"), song("a"), song("b")))
        assertEquals(listOf("c", "a", "b"), CustomOrder.arrange("PL1", page).map { it.videoId })

        val grown = page + song("d")
        assertEquals(listOf("c", "a", "b", "d"), CustomOrder.arrange("PL1", grown).map { it.videoId })

        CustomOrder.clear("PL1")
        assertEquals(listOf("a", "b", "c"), CustomOrder.arrange("PL1", page).map { it.videoId })
    }

    @Test
    fun `a track listed twice keeps two places`() {
        assertEquals(listOf("a#0", "b#0", "a#1"), CustomOrder.keysOf(listOf(song("a"), song("b"), song("a"))))
    }

    @Test
    fun `the album button steps loop, shuffle, repeat one and round again`() {
        assertEquals(PlayMode.SHUFFLE, PlayMode.LOOP_ORDER.next())
        assertEquals(PlayMode.REPEAT_ONE, PlayMode.SHUFFLE.next())
        assertEquals(PlayMode.LOOP_ORDER, PlayMode.REPEAT_ONE.next())
    }

    @Test
    fun `a playlist lives in one folder at a time`() {
        PlaylistFolders.create("Chill", "PLx")
        val chill = PlaylistFolders.folderOf("PLx")!!
        PlaylistFolders.create("Gym", "PLy")
        val gym = PlaylistFolders.folderOf("PLy")!!

        PlaylistFolders.moveInto(gym.id, "PLx")
        assertEquals(gym.id, PlaylistFolders.folderOf("PLx")?.id)
        assertEquals(chill.id, PlaylistFolders.byCardId(FOLDER_PREFIX + chill.id)?.id)

        PlaylistFolders.takeOut("PLx")
        assertNull(PlaylistFolders.folderOf("PLx"))
        PlaylistFolders.folders.value.forEach { PlaylistFolders.delete(it.id) }
    }
}
