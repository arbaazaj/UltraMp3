package com.ultramp3.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultramp3.data.model.Song
import com.ultramp3.ui.theme.MetallicGray
import com.ultramp3.ui.theme.NeonGreen
import com.ultramp3.ui.theme.PixelFont
import com.ultramp3.ui.theme.RetroBlack
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun PlaylistScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSongSelect: (Song) -> Unit
) {
    val songs by viewModel.songs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RetroBlack)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF800000))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PLAYLIST",
                color = Color.White,
                fontFamily = PixelFont,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(songs) { index, song ->
                val isSelected = song.id == currentSong?.id
                PlaylistItem(
                    index = index + 1,
                    song = song,
                    isSelected = isSelected,
                    onClick = { onSongSelect(song) }
                )
            }
        }

        // Bottom Soft Keys
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MetallicGray)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "MENU",
                color = Color.Black,
                fontFamily = PixelFont,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .clickable { onBack() }
            )
            Text(
                text = "EXIT",
                color = Color.Black,
                fontFamily = PixelFont,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .clickable { onBack() }
            )
        }
    }
}

@Composable
fun PlaylistItem(
    index: Int,
    song: Song,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MetallicGray.copy(alpha = 0.5f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$index. ",
            color = if (isSelected) Color.White else NeonGreen,
            fontFamily = PixelFont,
            fontSize = 16.sp,
            modifier = Modifier.width(30.dp)
        )
        Text(
            text = song.title,
            color = if (isSelected) Color.White else NeonGreen,
            fontFamily = PixelFont,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatSize(song.size),
                color = if (isSelected) Color.White else NeonGreen,
                fontFamily = PixelFont,
                fontSize = 12.sp
            )
            Text(
                text = formatDuration(song.duration),
                color = if (isSelected) Color.White else NeonGreen.copy(alpha = 0.7f),
                fontFamily = PixelFont,
                fontSize = 11.sp
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroup = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    return String.format(
        Locale.US,
        "%.1f %s",
        bytes / 1024.0.pow(digitGroup.toDouble()),
        units[digitGroup]
    )
}
