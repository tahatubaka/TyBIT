package com.tybit.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.tybit.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The running order the listener dragged an album or playlist into, kept on
 * this device per page. It works the same for an album, a YouTube playlist or a
 * playlist on the phone, because nothing is written back anywhere: the page
 * shows its songs in this order and plays them in it.
 *
 * Songs are named by [keysOf] — id plus which occurrence of it this is — so a
 * playlist holding the same track twice still has two places to put it.
 */
object CustomOrder {

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }

    private val _orders = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val orders: StateFlow<Map<String, List<String>>> = _orders.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("tybit_custom_order", Context.MODE_PRIVATE)
        _orders.value = runCatching {
            json.decodeFromString<Map<String, List<String>>>(prefs.getString(KEY, null) ?: "{}")
        }.getOrDefault(emptyMap())
    }

    fun has(browseId: String): Boolean = browseId in _orders.value

    /** Saves [songs]' order, as dragged, for [browseId]. */
    fun set(browseId: String, songs: List<Song>) = save(_orders.value + (browseId to keysOf(songs)))

    fun clear(browseId: String) = save(_orders.value - browseId)

    /**
     * [songs] in [browseId]'s saved order. Songs the order doesn't know — added
     * to the playlist since — keep their relative order after the known ones.
     */
    fun arrange(browseId: String, songs: List<Song>, orders: Map<String, List<String>> = _orders.value): List<Song> {
        val saved = orders[browseId] ?: return songs
        val rank = HashMap<String, Int>(saved.size).apply { saved.forEachIndexed { i, k -> put(k, i) } }
        val keys = keysOf(songs)
        return songs.indices.sortedBy { rank[keys[it]] ?: (saved.size + it) }.map { songs[it] }
    }

    /** Each song's id and occurrence ("abc#0", "abc#1" for a repeat). */
    fun keysOf(songs: List<Song>): List<String> {
        val seen = HashMap<String, Int>()
        return songs.map { song ->
            val id = song.setVideoId ?: song.videoId
            val n = seen.getOrDefault(id, 0)
            seen[id] = n + 1
            "$id#$n"
        }
    }

    private fun save(orders: Map<String, List<String>>) {
        _orders.value = orders
        if (::prefs.isInitialized) prefs.edit().putString(KEY, json.encodeToString(orders)).apply()
    }

    private const val KEY = "orders"
}
