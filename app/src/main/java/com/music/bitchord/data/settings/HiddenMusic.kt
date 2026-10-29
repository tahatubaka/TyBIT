package com.music.bitchord.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * One thing the listener asked not to hear: a track, or everything by an
 * artist. [title] and [subtitle] are only there so the list in Settings can
 * say what it is without a lookup.
 */
@Serializable
data class HiddenEntry(
    /** A video id, or [ARTIST_PREFIX] + the artist's browse id or lower-cased name. */
    val id: String,
    val title: String,
    val subtitle: String? = null,
    /** Epoch ms the entry lapses at; 0 for good. */
    val until: Long = 0L,
) {
    val isArtist: Boolean get() = id.startsWith(ARTIST_PREFIX)
}

const val ARTIST_PREFIX = "artist:"

/**
 * Spotify's "Hide song", "Snooze" and "Don't play this artist", kept on this
 * device. The playback service passes over a hidden track wherever the queue
 * reaches it — album, playlist, radio or AutoPlay — so there is no list to
 * filter at every place a queue gets built. A track the listener starts by
 * tapping it still plays: hiding is about what the app picks, not what they do.
 */
object HiddenMusic {

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }

    private val _entries = MutableStateFlow<List<HiddenEntry>>(emptyList())
    val entries: StateFlow<List<HiddenEntry>> = _entries.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("tubaka_hidden", Context.MODE_PRIVATE)
        val stored = runCatching {
            json.decodeFromString<List<HiddenEntry>>(prefs.getString(KEY, null) ?: "[]")
        }.getOrDefault(emptyList())
        _entries.value = stored.filterNot { it.lapsed() }
    }

    /** Hides [song] for [days], or for good when null. */
    fun hideSong(song: Song, days: Int?) {
        val until = days?.let { System.currentTimeMillis() + it * DAY_MS } ?: 0L
        put(HiddenEntry(song.videoId, song.title, song.artist, until))
    }

    /** Hides everything by [song]'s lead artist, for good. Returns the artist's name. */
    fun hideArtist(song: Song): String {
        val lead = song.artists.firstOrNull()
        val name = lead?.name ?: song.artist.substringBefore(" & ").substringBefore(", ")
        val key = lead?.browseId ?: song.artistId ?: "name:${name.lowercase()}"
        put(HiddenEntry(ARTIST_PREFIX + key, name))
        return name
    }

    fun unhide(id: String) = save(_entries.value.filterNot { it.id == id })

    /** Whether the app should pass over [song]: hidden itself, or by any artist on it. */
    fun isHidden(song: Song): Boolean {
        val live = _entries.value
        if (live.isEmpty()) return false
        val keys = buildSet {
            add(song.videoId)
            song.artistId?.let { add(ARTIST_PREFIX + it) }
            song.artists.forEach { artist ->
                artist.browseId?.let { add(ARTIST_PREFIX + it) }
                add(ARTIST_PREFIX + "name:" + artist.name.lowercase())
            }
            add(ARTIST_PREFIX + "name:" + song.artist.lowercase())
        }
        return live.any { it.id in keys && !it.lapsed() }
    }

    private fun put(entry: HiddenEntry) = save(_entries.value.filterNot { it.id == entry.id } + entry)

    private fun save(list: List<HiddenEntry>) {
        val kept = list.filterNot { it.lapsed() }
        _entries.value = kept
        if (::prefs.isInitialized) prefs.edit().putString(KEY, json.encodeToString(kept)).apply()
    }

    private fun HiddenEntry.lapsed() = until != 0L && until <= System.currentTimeMillis()

    private const val KEY = "entries"
    private const val DAY_MS = 24 * 60 * 60 * 1000L
}
