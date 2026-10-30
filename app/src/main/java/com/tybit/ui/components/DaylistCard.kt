package com.tybit.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tybit.ui.theme.TyBitGold
import com.tybit.ui.theme.TyBitNavy
import com.tybit.R
import com.tybit.data.model.Song
import com.tybit.data.stats.ListeningStats
import com.tybit.data.stats.dayPart
import com.tybit.playback.buildDaylist
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
    val gold = TyBitGold
    val sky = PART_SKY[part]
    Row(
        modifier = Modifier
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            // Midnight like the rest of TyBIT, lit in one corner by the sky of
            // this part of the day.
            .background(Brush.linearGradient(listOf(Color(0xFF1C2744), Color(0xFF0A1120))))
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(sky.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(0f, size.height),
                        radius = size.width * 0.6f,
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(0f, size.height),
                )
            }
            .border(1.dp, gold.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
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
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(CircleShape).background(sky.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(PART_ICONS[part], contentDescription = null, tint = sky, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "DAYLIST",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp,
                color = gold,
            )
            Text(
                text = "$day ${stringResource(PART_NAMES[part])}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W800,
                color = Color.White,
            )
            Text(
                text = stringResource(if (loading) R.string.daylist_loading else R.string.daylist_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 2,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(gold),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(22.dp), color = TyBitNavy, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.play), tint = TyBitNavy)
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

/** The sky of each part of the day: sunrise, noon, dusk, evening, night. */
private val PART_SKY = listOf(
    Color(0xFFFFB347),
    Color(0xFF56CCF2),
    Color(0xFFF2994A),
    Color(0xFFB07CFF),
    Color(0xFF6C8CD5),
)

private val PART_ICONS = listOf(
    Icons.Rounded.WbSunny,
    Icons.Rounded.LightMode,
    Icons.Rounded.WbTwilight,
    Icons.Rounded.NightsStay,
    Icons.Rounded.Bedtime,
)
