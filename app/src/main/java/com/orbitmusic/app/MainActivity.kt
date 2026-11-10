package com.orbitmusic.app

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors

private val Ink = Color(0xFF0B0D18)
private val Violet = Color(0xFF9B8CFF)
private val Cyan = Color(0xFF6DE7E2)
private val Muted = Color(0xFF9DA2B8)

class MainActivity : ComponentActivity() {
    private var controller by mutableStateOf<MediaController?>(null)
    private var tracks by mutableStateOf<List<Track>>(emptyList())
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refreshLibrary() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.READ_MEDIA_AUDIO)
            else add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            permissionLauncher.launch(permissions)
        } else refreshLibrary()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener({ controller = future.get() }, MoreExecutors.directExecutor())
        setContent { OrbitScreen(tracks, controller, ::playTrack) }
    }

    private fun refreshLibrary() { tracks = Library.load(this) }

    private fun playTrack(track: Track) {
        val c = controller ?: return
        val items = tracks.map { t ->
            MediaItem.Builder().setUri(t.uri).setMediaMetadata(
                MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist).setAlbumTitle(t.album).build()
            ).build()
        }
        val index = tracks.indexOf(track).coerceAtLeast(0)
        c.setMediaItems(items, index, 0L)
        c.prepare()
        c.play()
    }

    override fun onDestroy() {
        controller?.release()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrbitScreen(tracks: List<Track>, player: MediaController?, onPlay: (Track) -> Unit) {
    var query by remember { mutableStateOf("") }
    var shuffle by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(false) }
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(player) {
        while (true) {
            tick = player?.currentPosition ?: 0L
            kotlinx.coroutines.delay(500)
        }
    }
    val filtered = tracks.filter { it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true) }
    val currentTitle = player?.mediaMetadata?.title?.toString().orEmpty()
    val currentArtist = player?.mediaMetadata?.artist?.toString().orEmpty()
    val duration = (player?.duration ?: 0L).coerceAtLeast(0L)
    val playing = player?.isPlaying == true

    MaterialTheme(colorScheme = darkColorScheme(primary = Violet, secondary = Cyan, background = Ink, surface = Color(0xFF171A2B))) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF242044), Ink, Color(0xFF080A12))))) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(Brush.linearGradient(listOf(Violet, Cyan)), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.GraphicEq, null, tint = Ink)
                    }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("ORBIT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp, letterSpacing = 3.sp)
                        Text("MUSIC PLAYER", color = Muted, fontSize = 9.sp, letterSpacing = 2.sp)
                    }
                    Icon(Icons.Default.Headphones, null, tint = Cyan)
                }
                Text("Твоя музыка. Твоя орбита.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp))
                Text("ЛОКАЛЬНАЯ КОЛЛЕКЦИЯ", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                    placeholder = { Text("Трек, исполнитель или альбом…", color = Muted) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) },
                    singleLine = true, shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Violet, unfocusedBorderColor = Color.White.copy(.12f), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Row(Modifier.padding(horizontal = 22.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("ТРЕКИ", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                    Text("${filtered.size}", color = Cyan, fontSize = 12.sp)
                }
                if (filtered.isEmpty()) {
                    Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.LibraryMusic, null, tint = Violet, modifier = Modifier.size(52.dp))
                        Text(if (tracks.isEmpty()) "Пока здесь тихо" else "Ничего не найдено", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text(if (tracks.isEmpty()) "Добавь аудиофайлы на устройство" else "Попробуй другой запрос", color = Muted, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                        items(filtered, key = { it.id }) { track ->
                            Row(Modifier.fillMaxWidth().clickable { onPlay(track) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(48.dp).background(Brush.linearGradient(listOf(Violet.copy(.8f), Cyan.copy(.6f))), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Album, null, tint = Color.White)
                                }
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(track.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(track.artist, color = Muted, fontSize = 12.sp, maxLines = 1)
                                }
                                if (currentTitle == track.title) Icon(Icons.Default.GraphicEq, null, tint = Cyan)
                                else Text(formatTime(track.duration), color = Muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
                if (currentTitle.isNotBlank()) {
                    Surface(Modifier.fillMaxWidth().padding(12.dp), color = Color.White.copy(.09f), shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(.13f)), shadowElevation = 12.dp) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Album, null, tint = Violet, modifier = Modifier.size(38.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    Text(currentTitle, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(currentArtist, color = Muted, fontSize = 12.sp, maxLines = 1)
                                }
                                IconButton(onClick = { player?.seekToPreviousMediaItem() }) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White) }
                                IconButton(onClick = { if (playing) player?.pause() else player?.play() }) {
                                    Surface(color = Color.White, shape = CircleShape) {
                                        Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Ink, modifier = Modifier.padding(8.dp).size(24.dp))
                                    }
                                }
                                IconButton(onClick = { player?.seekToNextMediaItem() }) { Icon(Icons.Default.SkipNext, null, tint = Color.White) }
                            }
                            Slider(value = if (duration > 0) (tick.toFloat() / duration).coerceIn(0f, 1f) else 0f, onValueChange = { if (duration > 0) player?.seekTo((it * duration).toLong()) }, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Violet, inactiveTrackColor = Color.White.copy(.18f)))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(formatTime(tick), color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { shuffle = !shuffle; player?.shuffleModeEnabled = shuffle }) { Icon(Icons.Default.Shuffle, null, tint = if (shuffle) Cyan else Muted) }
                                IconButton(onClick = { repeat = !repeat; player?.repeatMode = if (repeat) 2 else 0 }) { Icon(Icons.Default.Repeat, null, tint = if (repeat) Cyan else Muted) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
