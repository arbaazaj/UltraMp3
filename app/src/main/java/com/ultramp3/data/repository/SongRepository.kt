package com.ultramp3.data.repository

import com.ultramp3.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SongRepository(private val musicScanner: MusicScanner) {
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: Flow<List<Song>> = _songs.asStateFlow()

    suspend fun refreshSongs() {
        _songs.value = musicScanner.scanMusic()
    }
}
