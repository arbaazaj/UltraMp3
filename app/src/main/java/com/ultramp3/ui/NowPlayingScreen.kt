package com.ultramp3.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.ultramp3.data.model.Song
import com.ultramp3.ui.components.RetroFrame
import com.ultramp3.ui.theme.DarkPanel
import com.ultramp3.ui.theme.MetallicGray
import com.ultramp3.ui.theme.MetallicLightGray
import com.ultramp3.ui.theme.NeonCyan
import com.ultramp3.ui.theme.NeonGreen
import com.ultramp3.ui.theme.PixelFont
import com.ultramp3.ui.theme.RetroBlack
import java.util.Locale
import kotlin.math.hypot

@Composable
fun NowPlayingScreen(
    viewModel: MainViewModel,
    onOpenPlaylist: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val currentSongIndex by viewModel.currentSongIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    val fftData by viewModel.fftData.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsState()
    val bitrate by viewModel.bitrate.collectAsState()
    val sampleRate by viewModel.sampleRate.collectAsState()

    val displaySong = currentSong ?: songs.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MetallicGray, RetroBlack, MetallicGray)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            RetroStatusBar(
                song = displaySong,
                progress = progress,
                totalTracks = songs.size,
                currentIndex = if (songs.isNotEmpty()) currentSongIndex + 1 else 0,
                bitrate = bitrate,
                sampleRate = sampleRate
            )

            Spacer(modifier = Modifier.height(8.dp))

            RetroFrame(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left: Album Art Area
                    Box(
                        modifier = Modifier
                            .weight(0.75f)
                            .fillMaxHeight()
                            .border(1.dp, MetallicGray.copy(alpha = 0.5f))
                    ) {
                        if (displaySong?.albumArtUri != null) {
                            AsyncImage(
                                model = displaySong.albumArtUri,
                                contentDescription = "Album Art",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                alpha = 0.6f
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.DarkGray.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ULTRA MP3",
                                    color = NeonGreen.copy(alpha = 0.15f),
                                    fontFamily = PixelFont,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Background Visualizer with the requested Green -> Blue -> Red gradient
                        RetroSpectrumVisualizer(
                            fftData = fftData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .height(120.dp)
                                .padding(bottom = 4.dp)
                        )
                    }

                    // Right: Vertical Audio Level Meters
                    Box(
                        modifier = Modifier
                            .weight(0.25f)
                            .fillMaxHeight()
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        VerticalLevelMeter(fftData)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Seek Bar
            RetroSeekBar(
                progress = progress,
                duration = displaySong?.duration ?: 0L,
                onSeek = { viewModel.seekTo(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            RetroInfoPanel(displaySong)

            Spacer(modifier = Modifier.height(8.dp))

            RetroControls(
                isPlaying = isPlaying,
                repeatMode = repeatMode,
                shuffleEnabled = shuffleModeEnabled,
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNext() },
                onPrev = { viewModel.playPrevious() },
                onToggleRepeat = { viewModel.toggleRepeatMode() },
                onToggleShuffle = { viewModel.toggleShuffleMode() },
                onMenu = onOpenPlaylist,
                onExit = onExit
            )
        }
    }
}

@Composable
fun RetroSeekBar(
    progress: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    val sliderValue = if (duration > 0) progress.toFloat() / duration else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(DarkPanel)
            .border(1.dp, MetallicGray)
    ) {
        Slider(
            value = sliderValue,
            onValueChange = { onSeek((it * duration).toLong()) },
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonGreen,
                inactiveTrackColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = MetallicGray,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
fun VerticalLevelMeter(fftData: ByteArray) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val meterCount = 14
        val meterGap = 3.dp.toPx()
        val meterWidth = size.width
        val meterHeight = (size.height - (meterCount - 1) * meterGap) / meterCount

        var sum = 0.0
        if (fftData.isNotEmpty()) {
            val range = (fftData.size / 8).coerceAtLeast(3)
            for (i in 2 until range) {
                val index = i * 2
                if (index + 1 < fftData.size) {
                    val r = fftData[index].toInt()
                    val im = fftData[index + 1].toInt()
                    sum += hypot(r.toDouble(), im.toDouble())
                }
            }
            val avgMagnitude = (sum / (range - 2)).coerceIn(0.0, 128.0)
            val activeMeters = ((avgMagnitude / 128.0) * meterCount).toInt()

            for (i in 0 until meterCount) {
                val isActive = i < activeMeters
                val color = when {
                    i > 11 -> Color.Red
                    i > 8 -> Color.Yellow
                    else -> NeonGreen
                }

                drawRect(
                    color = if (isActive) color else color.copy(alpha = 0.1f),
                    topLeft = Offset(0f, size.height - (i + 1) * (meterHeight + meterGap)),
                    size = Size(meterWidth, meterHeight)
                )
            }
        } else {
            for (i in 0 until meterCount) {
                val color = when {
                    i > 11 -> Color.Red
                    i > 8 -> Color.Yellow
                    else -> NeonGreen
                }
                drawRect(
                    color = color.copy(alpha = 0.1f),
                    topLeft = Offset(0f, size.height - (i + 1) * (meterHeight + meterGap)),
                    size = Size(meterWidth, meterHeight)
                )
            }
        }
    }
}

@Composable
fun RetroSpectrumVisualizer(
    fftData: ByteArray,
    modifier: Modifier = Modifier
) {
    val barsCount = 32
    val peaks = remember { mutableStateListOf<Float>().apply { repeat(barsCount) { add(0f) } } }

    LaunchedEffect(fftData) {
        if (fftData.isNotEmpty()) {
            for (i in 0 until barsCount) {
                val index = (i * 2) + 2
                if (index + 1 >= fftData.size) break
                val r = fftData[index].toInt()
                val im = fftData[index + 1].toInt()
                val magnitude = hypot(r.toDouble(), im.toDouble()).toFloat()
                val normalizedHeight = (magnitude / 128f).coerceIn(0f, 1f)

                if (normalizedHeight > peaks[i]) {
                    peaks[i] = normalizedHeight
                } else {
                    peaks[i] = (peaks[i] - 0.03f).coerceAtLeast(0f)
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        val barWidth = size.width / barsCount
        val maxAmplitude = 128f

        for (i in 0 until barsCount) {
            val index = (i * 2) + 2
            if (index + 1 >= fftData.size) break

            val r = fftData[index].toInt()
            val im = fftData[index + 1].toInt()
            val magnitude = hypot(r.toDouble(), im.toDouble()).toFloat()
            val normalizedHeight = (magnitude / maxAmplitude).coerceIn(0.05f, 1f)
            val barHeight = size.height * normalizedHeight

            // Gradient: Green -> Blue -> Red as requested
            val barBrush = Brush.verticalGradient(
                colors = listOf(Color.Red, Color.Blue, NeonGreen),
                startY = size.height - barHeight,
                endY = size.height
            )

            drawRect(
                brush = barBrush,
                topLeft = Offset(i * barWidth + 1.dp.toPx(), size.height - barHeight),
                size = Size(barWidth - 2.dp.toPx(), barHeight)
            )

            // Peak Dot
            val peakY = size.height - (peaks[i] * size.height)
            drawRect(
                color = Color.White.copy(alpha = 0.7f),
                topLeft = Offset(i * barWidth + 1.dp.toPx(), peakY),
                size = Size(barWidth - 2.dp.toPx(), 1.5.dp.toPx())
            )
        }
    }
}

@Composable
fun RetroStatusBar(
    song: Song?,
    progress: Long,
    totalTracks: Int,
    currentIndex: Int,
    bitrate: Int,
    sampleRate: Int
) {
    val totalDuration = song?.duration ?: 0L
    val remaining = if (totalDuration > progress) totalDuration - progress else 0L

    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkPanel)
            .border(1.dp, NeonGreen.copy(alpha = 0.2f))
            .padding(4.dp)
    ) {
        Text(
            text = "$currentIndex/$totalTracks ${song?.title ?: "---"}",
            color = NeonGreen,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "$bitrate kbit",
                    color = NeonGreen,
                    fontSize = 10.sp,
                    fontFamily = PixelFont
                )
                val sampleRateKhz = sampleRate / 1000.0
                Text(
                    text = String.format(Locale.US, "%.1f kHz", sampleRateKhz),
                    color = NeonGreen,
                    fontSize = 10.sp,
                    fontFamily = PixelFont
                )
            }
            Text(
                text = "-${formatTime(remaining)}",
                color = NeonGreen,
                fontSize = 48.sp,
                fontFamily = PixelFont,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatTime(totalDuration),
                color = NeonGreen,
                fontSize = 16.sp,
                fontFamily = PixelFont,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

@Composable
fun RetroInfoPanel(song: Song?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .border(1.dp, MetallicLightGray.copy(alpha = 0.3f))
            .background(DarkPanel)
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            InfoRow("Title:", song?.title ?: "Unknown Track")
            InfoRow("Author:", song?.artist ?: "Unknown Artist")
            InfoRow("Album:", song?.album ?: "Unknown Album")
            InfoRow("Year:", song?.year ?: "2004")
            InfoRow("Genre:", song?.genre ?: "MP3")
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row {
        Text(
            text = label,
            color = NeonGreen,
            fontSize = 13.sp,
            fontFamily = PixelFont,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = value,
            color = NeonGreen,
            fontSize = 13.sp,
            fontFamily = PixelFont,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RetroControls(
    isPlaying: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> "[R1]"
                    Player.REPEAT_MODE_ALL -> "[RA]"
                    else -> " R "
                },
                color = if (repeatMode != Player.REPEAT_MODE_OFF) NeonCyan else Color.Gray,
                fontFamily = PixelFont,
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable { onToggleRepeat() }
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (shuffleEnabled) "[SH]" else " S ",
                color = if (shuffleEnabled) NeonCyan else Color.Gray,
                fontFamily = PixelFont,
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable { onToggleShuffle() }
                    .padding(4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MetallicLightGray, MetallicGray, RetroBlack)
                    )
                )
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MENU",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = PixelFont,
                modifier = Modifier.clickable { onMenu() }
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏮",
                    color = Color.White,
                    fontSize = 22.sp,
                    modifier = Modifier.clickable { onPrev() })
                Text(
                    text = if (isPlaying) "⏸" else "▶",
                    color = if (isPlaying) NeonCyan else Color.White,
                    fontSize = 28.sp,
                    modifier = Modifier.clickable { onPlayPause() }
                )
                Text(
                    text = "⏭",
                    color = Color.White,
                    fontSize = 22.sp,
                    modifier = Modifier.clickable { onNext() })
            }

            Text(
                text = "EXIT",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = PixelFont,
                modifier = Modifier.clickable { onExit() }
            )
        }
    }
}
