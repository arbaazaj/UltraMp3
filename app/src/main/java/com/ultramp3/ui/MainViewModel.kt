package com.ultramp3.ui

import android.content.ComponentName
import android.content.Context
import android.media.audiofx.Visualizer
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.ultramp3.data.model.Song
import com.ultramp3.data.repository.SongRepository
import com.ultramp3.service.MusicService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: SongRepository) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _currentSongIndex = MutableStateFlow(0)
    val currentSongIndex: StateFlow<Int> = _currentSongIndex.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0L)
    val playbackProgress: StateFlow<Long> = _playbackProgress.asStateFlow()

    private val _fftData = MutableStateFlow(ByteArray(0))
    val fftData: StateFlow<ByteArray> = _fftData.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _bitrate = MutableStateFlow(128)
    val bitrate: StateFlow<Int> = _bitrate.asStateFlow()

    private val _sampleRate = MutableStateFlow(44100)
    val sampleRate: StateFlow<Int> = _sampleRate.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var visualizer: Visualizer? = null

    init {
        viewModelScope.launch {
            repository.songs.collect {
                _songs.value = it
            }
        }
    }

    fun initController(context: Context) {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            mediaController?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        requestAudioSessionId()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val index = mediaController?.currentMediaItemIndex ?: 0
                    _currentSongIndex.value = index
                    val currentId = mediaItem?.mediaId?.toLongOrNull()
                    _currentSong.value = _songs.value.find { it.id == currentId }
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    _repeatMode.value = repeatMode
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    _shuffleModeEnabled.value = shuffleModeEnabled
                }

                @OptIn(UnstableApi::class)
                override fun onTracksChanged(tracks: Tracks) {
                    for (group in tracks.groups) {
                        for (i in 0 until group.length) {
                            if (group.isTrackSelected(i)) {
                                val format = group.getTrackFormat(i)
                                if (format.sampleRate != Format.NO_VALUE) {
                                    _sampleRate.value = format.sampleRate
                                }
                                if (format.bitrate != Format.NO_VALUE) {
                                    _bitrate.value = format.bitrate / 1000
                                }
                            }
                        }
                    }
                }
            })
            startProgressUpdate()
        }, MoreExecutors.directExecutor())
    }

    @OptIn(UnstableApi::class)
    private fun requestAudioSessionId() {
        mediaController?.let { controller ->
            val customCommand = androidx.media3.session.SessionCommand(
                MusicService.CUSTOM_COMMAND_GET_AUDIO_SESSION_ID,
                Bundle.EMPTY
            )
            val future = controller.sendCustomCommand(customCommand, Bundle.EMPTY)
            future.addListener({
                try {
                    val result = future.get()
                    val sessionId = result.extras.getInt("audio_session_id", 0)
                    if (sessionId != 0) {
                        setupVisualizer(sessionId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
        }
    }

    private fun setupVisualizer(sessionId: Int) {
        if (visualizer != null && visualizer?.enabled == true) return
        try {
            visualizer?.release()
            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        v: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                    }

                    override fun onFftDataCapture(
                        v: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        _fftData.value = fft ?: ByteArray(0)
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startProgressUpdate() {
        viewModelScope.launch {
            while (true) {
                _playbackProgress.value = mediaController?.currentPosition ?: 0L
                delay(500)
            }
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            repository.refreshSongs()
        }
    }

    @OptIn(UnstableApi::class)
    fun playSong(song: Song) {
        mediaController?.let { controller ->
            val mediaItems = _songs.value.map { s ->
                MediaItem.Builder()
                    .setMediaId(s.id.toString())
                    .setUri(s.contentUri)
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(s.title)
                            .setArtist(s.artist)
                            .setAlbumTitle(s.album)
                            .build()
                    )
                    .build()
            }
            controller.setMediaItems(mediaItems)
            val index = _songs.value.indexOf(song)
            if (index != -1) {
                controller.seekTo(index, 0L)
            }
            controller.prepare()
            controller.play()
            _currentSong.value = song
            _currentSongIndex.value = if (index != -1) index else 0
        }
    }

    fun togglePlayPause() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun playNext() {
        mediaController?.seekToNext()
    }

    fun playPrevious() {
        mediaController?.seekToPrevious()
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    fun toggleRepeatMode() {
        mediaController?.let {
            val nextMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
            it.repeatMode = nextMode
        }
    }

    fun toggleShuffleMode() {
        mediaController?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
        }
    }

    override fun onCleared() {
        super.onCleared()
        visualizer?.release()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
