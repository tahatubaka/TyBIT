package com.music.bitchord.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.bitchord.R
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.stats.ListeningStats
import com.music.bitchord.data.stats.dayPart
import com.music.bitchord.playback.buildDaylist
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * The daylist card at the top of Home. Names this part of the day — "daylist ·
 * thursday morning" — and plays a fresh mix of it when tapped, built then
 * rather than ahead of time so it is always this hour's. Not shown until there
 * is listening history to make one from.
 */
@Composable
fun DaylistCard(onPlay: (songs: List<Song>, title: String) -> Unit) {
    val context = LocalContext.current
    val now = remember { LocalDateTime.now() }
    val part = dayPart(now.hour)
    // The language the strings around it actually resolved to. Neither the
    // phone's locale nor the configuration's will do: Android matches
    // Indonesian resources as "in", so an id-ID phone can be reading English.
    val locale = Locale.forLanguageTag(stringResource(R.string.daylist_locale))
    val day = now.dayOfWeek.getDisplayName(TextStyle.FULL, locale).lowercase(locale)
    val title = stringResource(R.string.daylist_title, day, stringResource(PART_NAMES[part]))
    val hasHistory by produceState(false, part) {
        value = ListeningStats.daylistSeeds(part, limit = 1).isNotEmpty()
    }
    if (!hasHistory) return

    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(PART_COLORS[part]))
            .clickable(enabled = !loading) {
                loading = true
                scope.launch {
                    val songs = buildDaylist(part)
                    loading = false
                    if (songs.isEmpty()) {
                        Toast.makeText(context, R.string.couldnt_load_tracks, Toast.LENGTH_SHORT).show()
                    } else {
                        onPlay(songs, title)
                    }
                }
            }
            .padding(20.dp),
    ) {
        Column(Modifier.align(Alignment.CenterStart).padding(end = 64.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(if (loading) R.string.daylist_loading else R.string.daylist_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.play), tint = Color.Black)
            }
        }
    }
}

private val PART_NAMES = listOf(
    R.string.daylist_part_morning,
    R.string.daylist_part_midday,
    R.string.daylist_part_afternoon,
    R.string.daylist_part_evening,
    R.string.daylist_part_night,
)

/** A sky for each part of the day: sunrise, noon, dusk, evening, night. */
private val PART_COLORS = listOf(
    listOf(Color(0xFFFFB347), Color(0xFFFF6F61)),
    listOf(Color(0xFF56CCF2), Color(0xFF2F80ED)),
    listOf(Color(0xFFF2994A), Color(0xFFC0392B)),
    listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
    listOf(Color(0xFF16222A), Color(0xFF3A6073)),
)
