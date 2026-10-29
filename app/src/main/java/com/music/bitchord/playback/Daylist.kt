package com.music.bitchord.playback

import com.music.bitchord.data.model.QueueTier
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.HiddenMusic
import com.music.bitchord.data.stats.ListeningStats
import com.music.bitchord.data.stats.TrackEntry
import com.music.bitchord.playback.QueueCoordinator.asQueueEntry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Spotify's daylist, from this device's own listening: what gets played in
 * this [part] of the day (see [com.music.bitchord.data.stats.dayPart]), with a
 * new track from those tracks' radios after every two familiar ones. Empty
 * until something has been listened to.
 */
suspend fun buildDaylist(part: Int): List<Song> = coroutineScope {
    val familiar = ListeningStats.daylistSeeds(part, FAMILIAR)
        .map { it.toSong() }
        .filterNot(HiddenMusic::isHidden)
        .shuffled()
    if (familiar.isEmpty()) return@coroutineScope emptyList()
    val have = familiar.mapTo(HashSet()) { it.videoId }
    val fresh = familiar.take(SEEDS).map { seed ->
        async { loadAutoplayTracks(listOf(seed), seed, limit = FRESH).getOrDefault(emptyList()) }
    }.awaitAll()
        .flatten()
        .filter { it.videoId !in have && !HiddenMusic.isHidden(it) }
        .distinctBy { it.videoId }
        .shuffled()
        .take(FRESH)
    val mixed = ArrayList<Song>(familiar.size + fresh.size)
    val newOnes = fresh.iterator()
    familiar.forEachIndexed { i, song ->
        mixed += song
        if (i % 2 == 1 && newOnes.hasNext()) mixed += newOnes.next()
    }
    newOnes.forEachRemaining { mixed += it }
    mixed.map { it.copy(queueEntryId = null).asQueueEntry(QueueTier.CONTEXT) }
}

private fun TrackEntry.toSong() = Song(
    videoId = id,
    title = title,
    artist = artist,
    thumbnailUrl = art,
    artistId = artistId,
    albumId = albumId,
    albumName = album,
)

private const val FAMILIAR = 20
private const val SEEDS = 3
private const val FRESH = 10
