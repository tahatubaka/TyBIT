package com.tybit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tybit.R
import com.tybit.data.settings.HiddenMusic
import java.text.DateFormat
import java.util.Date

/** Asks how long to hide a song: 30 days (Spotify's snooze) or for good. */
@Composable
fun HideSongDialog(onChoose: (days: Int?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.hide_song_question)) },
        confirmButton = {
            TextButton(onClick = { onChoose(null) }) { Text(stringResource(R.string.hide_forever)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                TextButton(onClick = { onChoose(30) }) { Text(stringResource(R.string.hide_30_days)) }
            }
        },
    )
}

/** What is hidden, each with a way to bring it back. Opened from Settings. */
@Composable
fun HiddenMusicDialog(onDismiss: () -> Unit) {
    val entries by HiddenMusic.entries.collectAsStateWithLifecycle()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.hidden_music)) },
        text = {
            if (entries.isEmpty()) {
                Text(stringResource(R.string.hidden_music_empty))
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(entries, key = { it.id }) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val detail = when {
                                    entry.isArtist -> stringResource(R.string.hidden_artist_label)
                                    entry.until > 0L -> stringResource(
                                        R.string.hidden_until,
                                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.until)),
                                    )
                                    else -> entry.subtitle.orEmpty()
                                }
                                Text(
                                    detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            TextButton(onClick = { HiddenMusic.unhide(entry.id) }) {
                                Text(stringResource(R.string.unhide))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}
