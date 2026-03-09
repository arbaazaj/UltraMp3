package com.ultramp3

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.ultramp3.data.repository.MusicScanner
import com.ultramp3.data.repository.SongRepository
import com.ultramp3.ui.MainViewModel
import com.ultramp3.ui.MainViewModelFactory
import com.ultramp3.ui.NowPlayingScreen
import com.ultramp3.ui.PlaylistScreen
import com.ultramp3.ui.theme.UltraMp3Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Manual DI
        val musicScanner = MusicScanner(this)
        val songRepository = SongRepository(musicScanner)

        setContent {
            UltraMp3Theme {
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(songRepository)
                )

                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    viewModel.initController(this@MainActivity)
                }

                PermissionWrapper(onPermissionGranted = {
                    viewModel.loadSongs()
                }) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "now_playing",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("now_playing") {
                                NowPlayingScreen(
                                    viewModel = viewModel,
                                    onOpenPlaylist = { navController.navigate("playlist") },
                                    onExit = { finish() }
                                )
                            }
                            composable("playlist") {
                                PlaylistScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onSongSelect = { song ->
                                        viewModel.playSong(song)
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionWrapper(
    onPermissionGranted: () -> Unit,
    content: @Composable () -> Unit
) {
    val permissions = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        add(Manifest.permission.RECORD_AUDIO)
    }

    val permissionsState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            onPermissionGranted()
        }
    }

    if (permissionsState.allPermissionsGranted) {
        content()
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("Grant Permissions (Storage & Audio)")
            }
        }
    }
}
