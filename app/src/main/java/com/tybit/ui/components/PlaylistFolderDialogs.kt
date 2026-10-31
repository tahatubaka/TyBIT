package com.tybit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tybit.R
import com.tybit.data.settings.PlaylistFolders

/**
 * Puts a playlist in a folder: one of the existing ones, a new one named here,
 * or — when it is in one already — back out onto the Library's shelf.
 */
@Composable
fun FolderPickerDialog(playlistId: String, playlistTitle: String, onDismiss: () -> Unit) {
    val folders by PlaylistFolders.folders.collectAsStateWithLifecycle()
    val current = remember(folders) { PlaylistFolders.folderOf(playlistId) }
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.move_to_folder)) },
        text = {
            Column {
                Text(playlistTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                LazyColumn(Modifier.heightIn(max = 260.dp)) {
                    items(folders, key = { it.id }) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    PlaylistFolders.moveInto(folder.id, playlistId)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = if (folder.id == current?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(folder.name, color = if (folder.id == current?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.new_folder)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = newName.isNotBlank(),
                onClick = {
                    PlaylistFolders.create(newName, playlistId)
                    onDismiss()
                },
            ) { Text(stringResource(R.string.create_folder)) }
        },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(onClick = {
                        PlaylistFolders.takeOut(playlistId)
                        onDismiss()
                    }) { Text(stringResource(R.string.remove_from_folder)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

/** Rename or delete a folder — deleting puts its playlists back in the Library. */
@Composable
fun FolderManageDialog(folderId: String, onDismiss: () -> Unit) {
    val folders by PlaylistFolders.folders.collectAsStateWithLifecycle()
    val folder = folders.firstOrNull { it.id == folderId } ?: return
    var name by remember(folderId) { mutableStateOf(folder.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folder)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.rename)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && name.trim() != folder.name,
                onClick = {
                    PlaylistFolders.rename(folderId, name)
                    onDismiss()
                },
            ) { Text(stringResource(R.string.save_order)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    PlaylistFolders.delete(folderId)
                    onDismiss()
                }) { Text(stringResource(R.string.delete_folder), color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
