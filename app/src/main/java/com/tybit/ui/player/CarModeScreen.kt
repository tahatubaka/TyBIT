package com.tybit.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.tybit.R
import com.tybit.data.model.Song
import com.tybit.data.model.artworkAt
import com.tybit.ui.theme.TyBitGold
import com.tybit.ui.theme.TyBitNavy

/**
 * Car View: what a glance at a dashboard mount can take in — the sleeve, the
 * song in large type, and three controls big enough to hit without looking.
 * The screen stays on while it is up; Back or the cross leaves it.
 */
@Composable
fun CarModeScreen(
    song: Song?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TyBitNavy)
            // Swallows taps so nothing behind the overlay reacts to them.
            .clickable(enabled = false) {}
            .systemBarsPadding()
            .padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.car_mode),
                color = TyBitGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.W700,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close), tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.weight(0.6f))
        AsyncImage(
            model = song?.artworkAt(900),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.06f)),
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = song?.title.orEmpty(),
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.W800,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song?.artist.orEmpty(),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 36.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CarButton(Icons.Rounded.SkipPrevious, stringResource(R.string.previous), 88.dp, onPrevious)
            CarButton(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                stringResource(if (isPlaying) R.string.pause else R.string.play),
                120.dp,
                onPlayPause,
                primary = true,
            )
            CarButton(Icons.Rounded.SkipNext, stringResource(R.string.next), 88.dp, onNext)
        }
    }
}

@Composable
private fun CarButton(icon: ImageVector, label: String, size: Dp, onClick: () -> Unit, primary: Boolean = false) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (primary) TyBitGold else Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = if (primary) TyBitNavy else Color.White, modifier = Modifier.size(size * 0.5f))
    }
}
