package com.tybit.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/** A named group of playlists in the Library, by their browse ids. */
@Serializable
data class PlaylistFolder(val id: String, val name: String, val playlists: List<String> = emptyList())

/** The browse id a folder's own card carries in the Library. */
const val FOLDER_PREFIX = "folder:"

/**
 * Playlist folders, kept on this device. A playlist sits in at most one folder;
 * the Library shows the folder's card in place of the playlists inside it, and
 * the card opens them as a page of their own.
 */
object PlaylistFolders {

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }

    private val _folders = MutableStateFlow<List<PlaylistFolder>>(emptyList())
    val folders: StateFlow<List<PlaylistFolder>> = _folders.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("tybit_playlist_folders", Context.MODE_PRIVATE)
        _folders.value = runCatching {
            json.decodeFromString<List<PlaylistFolder>>(prefs.getString(KEY, null) ?: "[]")
        }.getOrDefault(emptyList())
    }

    fun folderOf(playlistId: String): PlaylistFolder? = _folders.value.firstOrNull { playlistId in it.playlists }

    fun byCardId(cardId: String): PlaylistFolder? =
        _folders.value.firstOrNull { FOLDER_PREFIX + it.id == cardId }

    /** A new folder named [name] holding [playlistId]. */
    fun create(name: String, playlistId: String) {
        val folder = PlaylistFolder(UUID.randomUUID().toString(), name.trim(), listOf(playlistId))
        save(_folders.value.map { it.without(playlistId) } + folder)
    }

    /** Moves [playlistId] into [folderId], out of any other folder. */
    fun moveInto(folderId: String, playlistId: String) = save(
        _folders.value.map { f ->
            if (f.id == folderId) f.copy(playlists = f.playlists.filterNot { it == playlistId } + playlistId) else f.without(playlistId)
        },
    )

    /** Back out to the Library's own shelf. */
    fun takeOut(playlistId: String) = save(_folders.value.map { it.without(playlistId) })

    fun rename(folderId: String, name: String) =
        save(_folders.value.map { if (it.id == folderId) it.copy(name = name.trim()) else it })

    /** Deletes the folder only; its playlists go back to the Library. */
    fun delete(folderId: String) = save(_folders.value.filterNot { it.id == folderId })

    private fun PlaylistFolder.without(playlistId: String) = copy(playlists = playlists.filterNot { it == playlistId })

    private fun save(folders: List<PlaylistFolder>) {
        _folders.value = folders
        if (::prefs.isInitialized) prefs.edit().putString(KEY, json.encodeToString(folders)).apply()
    }

    private const val KEY = "folders"
}
