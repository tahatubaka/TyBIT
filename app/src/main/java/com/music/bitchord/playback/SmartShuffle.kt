package com.music.bitchord.playback

import com.music.bitchord.data.model.QueueTier
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.isUnresolvedSpotify
import com.music.bitchord.data.settings.HiddenMusic
import com.music.bitchord.playback.QueueCoordinator.asQueueEntry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Spotify's Smart Shuffle: recommendations to mix into a shuffled album or
 * playlist, about one for every three of its tracks. Drawn from the radios of
 * a few of its tracks picked at random, so they sound like the list rather than
 * like any one song on it. Tagged as context, not AutoPlay, so the shuffle
 * mixes them in instead of leaving them at the tail.
 */
suspend fun smartShufflePicks(songs: List<Song>): List<Song> = coroutineScope {
    val want = (songs.size / 3).coerceIn(MIN_PICKS, MAX_PICKS)
    val seeds = songs.filter { !it.isUnresolvedSpotify && it.localUri == null }.shuffled().take(SEEDS)
    val have = songs.mapTo(HashSet()) { it.videoId }
    seeds.map { seed ->
        async { loadAutoplayTracks(listOf(seed), seed, limit = want).getOrDefault(emptyList()) }
    }.awaitAll()
        .flatten()
        .filter { it.videoId !in have && !HiddenMusic.isHidden(it) }
        .distinctBy { it.videoId }
        .shuffled()
        .take(want)
        .map { it.copy(queueEntryId = null).asQueueEntry(QueueTier.CONTEXT) }
}

private const val SEEDS = 3
private const val MIN_PICKS = 3
private const val MAX_PICKS = 30
