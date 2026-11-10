package com.orbitmusic.app

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalTextStyle
import androidx.compose.ui.platform.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.AsyncImage
import com.google.common.util.concurrent.MoreExecutors

private val Background = Color(0xFF090B12)
private val Panel = Color(0xFF171A27)
private val Accent = Color(0xFF9C8BFF)
private val Aqua = Color(0xFF63E5DF)
private val SecondaryText = Color(0xFF969BAD)

class MainActivity : ComponentActivity() {
    private var controller by mutableStateOf<MediaController?>(null)
    private var tracks by mutableStateOf<List<Track>>(emptyList())
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refreshLibrary() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(13, 15, 25)
        window.navigationBarColor = android.graphics.Color.rgb(9, 11, 18)
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.READ_MEDIA_AUDIO)
            else add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) permissionLauncher.launch(permissions)
        else refreshLibrary()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener({ controller = future.get() }, MoreExecutors.directExecutor())
        setContent { OrbitScreen(tracks, controller, ::playTrack) }
    }

    private fun refreshLibrary() { tracks = Library.load(this) }
    private fun playTrack(track: Track) {
        val c = controller ?: return
        val queue = tracks.map { t ->
            MediaItem.Builder().setUri(t.uri).setMediaMetadata(
                MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist).setAlbumTitle(t.album).build()
            ).build()
        }
        c.setMediaItems(queue, tracks.indexOf(track).coerceAtLeast(0), 0L)
        c.prepare()
        c.play()
    }
    override fun onDestroy() { controller?.release(); super.onDestroy() }
}

@Composable
private fun OrbitScreen(tracks: List<Track>, player: MediaController?, onPlay: (Track) -> Unit) {
    var search by remember { mutableStateOf("") }
    var shuffle by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var scrubbing by remember { mutableStateOf(false) }
    LaunchedEffect(player) {
        while (true) {
            if (!scrubbing) position = player?.currentPosition ?: 0L
            kotlinx.coroutines.delay(350)
        }
    }
    val visibleTracks = tracks.filter { it.title.contains(search, true) || it.artist.contains(search, true) || it.album.contains(search, true) }
    val title = player?.mediaMetadata?.title?.toString().orEmpty()
    val artist = player?.mediaMetadata?.artist?.toString().orEmpty()
    val duration = (player?.duration ?: 0L).coerceAtLeast(0L)
    val isPlaying = player?.isPlaying == true

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Accent, secondary = Aqua, background = Background, surface = Panel,
        onBackground = Color.White, onSurface = Color.White
    )) {
        CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = FontFamily.SansSerif)) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF17162B), Background, Color(0xFF07090F))))) {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Accent, Aqua))), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.GraphicEq, null, tint = Color(0xFF101321), modifier = Modifier.size(25.dp))
                        }
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text("ORBIT", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.5.sp, color = Color.White)
                            Text("YOUR SOUND SPACE", fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.8.sp, color = SecondaryText)
                        }
                        Icon(Icons.Default.Headphones, null, tint = Aqua, modifier = Modifier.size(24.dp))
                    }

                    Text("Медиатека", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 20.dp))
                    Text("${tracks.size} треков на устройстве", fontSize = 12.sp, color = SecondaryText, modifier = Modifier.padding(start = 21.dp, top = 3.dp, bottom = 14.dp))

                    OutlinedTextField(
                        value = search, onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        placeholder = { Text("Поиск треков и исполнителей", color = SecondaryText, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = SecondaryText) },
                        trailingIcon = { if (search.isNotEmpty()) IconButton(onClick = { search = "" }) { Icon(Icons.Default.Close, null, tint = SecondaryText) } },
                        singleLine = true, shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent.copy(.8f), unfocusedBorderColor = Color.White.copy(.09f),
                            focusedContainerColor = Color.White.copy(.045f), unfocusedContainerColor = Color.White.copy(.035f),
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Aqua
                        )
                    )

                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("ВСЕ ТРЕКИ", fontSize = 10.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold, color = SecondaryText)
                        Spacer(Modifier.weight(1f))
                        Text("${visibleTracks.size}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Aqua)
                    }

                    if (visibleTracks.isEmpty()) {
                        Column(Modifier.fillMaxSize().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.LibraryMusic, null, tint = Accent, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(if (tracks.isEmpty()) "Музыка появится здесь" else "Ничего не найдено", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                            Text(if (tracks.isEmpty()) "Добавь аудиофайлы на устройство" else "Измени поисковый запрос", color = SecondaryText, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = if (title.isNotBlank()) 8.dp else 20.dp)
                        ) {
                            items(visibleTracks, key = { it.id }) { track ->
                                val selected = title == track.title
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) Color.White.copy(.065f) else Color.Transparent)
                                        .clickable { onPlay(track) }.padding(horizontal = 8.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFF514A86), Color(0xFF237E83)))), contentAlignment = Alignment.Center) {
                                        Icon(if (selected && isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.size(23.dp))
                                    }
                                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                        Text(track.title, color = if (selected) Aqua else Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(3.dp))
                                        Text(track.artist.takeUnless { it == "<unknown>" } ?: "Неизвестный исполнитель", color = SecondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(formatTime(track.duration), color = SecondaryText, fontSize = 11.sp)
                                    Spacer(Modifier.width(5.dp))
                                    IconButton(onClick = { onPlay(track) }, modifier = Modifier.size(34.dp)) {
                                        Icon(if (selected && isPlaying) Icons.Default.Equalizer else Icons.Default.PlayArrow, null, tint = if (selected) Aqua else SecondaryText, modifier = Modifier.size(19.dp))
                                    }
                                }
                            }
                        }
                    }

                    if (title.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            color = Color(0xFF202235).copy(.96f), shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color.White.copy(.09f)), shadowElevation = 10.dp
                        ) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(listOf(Accent, Aqua))), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Album, null, tint = Background)
                                    }
                                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                        Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(artist, color = SecondaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = { player?.seekToPreviousMediaItem() }, modifier = Modifier.size(38.dp)) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White) }
                                    IconButton(onClick = { if (isPlaying) player?.pause() else player?.play() }, modifier = Modifier.size(42.dp)) {
                                        Surface(color = Color.White, shape = CircleShape) {
                                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Background, modifier = Modifier.padding(8.dp).size(24.dp))
                                        }
                                    }
                                    IconButton(onClick = { player?.seekToNextMediaItem() }, modifier = Modifier.size(38.dp)) { Icon(Icons.Default.SkipNext, null, tint = Color.White) }
                                }
                                Slider(
                                    value = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                                    onValueChange = { scrubbing = true; position = (it * duration).toLong() },
                                    onValueChangeFinished = { player?.seekTo(position); scrubbing = false },
                                    modifier = Modifier.height(22.dp),
                                    colors = SliderDefaults.colors(thumbColor = Aqua, activeTrackColor = Accent, inactiveTrackColor = Color.White.copy(.15f))
                                )
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
