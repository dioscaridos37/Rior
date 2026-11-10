package com.orbitmusic.app

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.*
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

private val Gold = Color(0xFFFFD02F)
private val Black = Color(0xFF050505)
private val Grey = Color(0xFF969696)

class MainActivity : ComponentActivity() {
    private var controller by mutableStateOf<MediaController?>(null)
    private var tracks by mutableStateOf<List<Track>>(emptyList())
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tracks = Library.load(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        val asks = buildList {
            add(if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
        if (asks.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) permissions.launch(asks) else tracks = Library.load(this)
        val future = MediaController.Builder(this, SessionToken(this, ComponentName(this, PlaybackService::class.java))).buildAsync()
        future.addListener({ controller = future.get() }, MoreExecutors.directExecutor())
        setContent { Home(tracks, controller, ::play, ::playWave) }
    }
    private fun queue() = tracks.map { t -> MediaItem.Builder().setUri(t.uri).setMediaMetadata(MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist).setAlbumTitle(t.album).build()).build() }
    private fun play(t: Track) { controller?.let { it.setMediaItems(queue(), tracks.indexOf(t).coerceAtLeast(0), 0L); it.prepare(); it.play() } }
    private fun playWave() { controller?.let { it.setMediaItems(queue().shuffled()); it.prepare(); it.play() } }
    override fun onDestroy() { controller?.release(); super.onDestroy() }
}

@Composable
private fun Home(tracks: List<Track>, player: MediaController?, onPlay: (Track) -> Unit, onWave: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var pos by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    val title = player?.mediaMetadata?.title?.toString().orEmpty()
    val artist = player?.mediaMetadata?.artist?.toString().orEmpty()
    val duration = (player?.duration ?: 0L).coerceAtLeast(0)
    val playing = player?.isPlaying == true
    LaunchedEffect(player) { while (true) { if (!dragging) pos = player?.currentPosition ?: 0L; kotlinx.coroutines.delay(400) } }
    val list = tracks.filter { it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = Black, surface = Color(0xFF202020))) {
        Column(Modifier.fillMaxSize().background(Black).statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color.Magenta, Color(0xFFFF8C45), Gold))), contentAlignment = Alignment.Center) { Icon(Icons.Default.GraphicEq, null, tint = Color.White) }
                if (search) OutlinedTextField(query, { query = it }, Modifier.weight(1f).padding(horizontal = 8.dp), placeholder = { Text("Поиск музыки", color = Grey) }, singleLine = true)
                else Text("ORBIT ✳ Музыка", Modifier.weight(1f), color = Gold, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick = { search = !search; if (!search) query = "" }) { Icon(if (search) Icons.Default.Close else Icons.Default.Search, null, tint = Color.White) }
            }
            Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Для вас", Modifier.clip(RoundedCornerShape(22.dp)).clickable { tab = 0 }.padding(horizontal = 20.dp, vertical = 9.dp), color = if (tab == 0) Color.White else Grey, fontWeight = if (tab == 0) FontWeight.Bold else FontWeight.Normal)
                Text("Тренды", Modifier.clip(RoundedCornerShape(22.dp)).background(if (tab == 1) Color(0xFF292929) else Color.Transparent).clickable { tab = 1 }.padding(horizontal = 20.dp, vertical = 9.dp), color = if (tab == 1) Color.White else Grey, fontWeight = if (tab == 1) FontWeight.Bold else FontWeight.Normal)
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                if (tab == 0 && !search) {
                    item {
                        Box(Modifier.fillMaxWidth().height(340.dp).padding(horizontal = 10.dp, vertical = 6.dp).clip(RoundedCornerShape(18.dp)).background(Black)) {
                            Canvas(Modifier.fillMaxSize()) {
                                val w = size.width; val h = size.height
                                val path = Path().apply { moveTo(w*.07f,h*.78f); cubicTo(w*.12f,h*.35f,w*.25f,h*.08f,w*.5f,h*.08f); cubicTo(w*.75f,h*.08f,w*.88f,h*.35f,w*.93f,h*.78f); cubicTo(w*.90f,h*.98f,w*.68f,h*.99f,w*.5f,h*.99f); cubicTo(w*.32f,h*.99f,w*.10f,h*.98f,w*.07f,h*.78f) }
                                drawPath(path, Gold.copy(.18f), style = Stroke(24.dp.toPx(), cap = StrokeCap.Round))
                                drawPath(path, Gold.copy(.5f), style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))
                                drawPath(path, Gold, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                            }
                            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(42.dp)); Text("Моя волна", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold) }
                                Spacer(Modifier.height(18.dp))
                                Surface(onClick = onWave, color = Color(0xFF202020), shape = RoundedCornerShape(24.dp)) { Row(Modifier.padding(horizontal = 20.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Tune, null, tint = Color.White, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(8.dp)); Text("Запустить волну", color = Color.White) } }
                            }
                            Text("Персональный микс из твоей музыки", Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), color = Grey, fontSize = 11.sp)
                        }
                    }
                    item {
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Для вас" to "Микс из любимого", "Тренды" to "Твоя коллекция").forEachIndexed { i, pair ->
                                Surface(onClick = { tab = i }, color = Color(0xFF242018), shape = RoundedCornerShape(26.dp)) {
                                    Row(Modifier.width(190.dp).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Gold, Color(0xFF573D19)))))
                                        Column(Modifier.padding(start = 9.dp)) { Text(pair.first, color = Color.White, fontWeight = FontWeight.Bold); Text(pair.second, color = Grey, fontSize = 11.sp) }
                                    }
                                }
                            }
                        }
                    }
                    item { Heading("Подборки музыки", "Всё") }
                    item {
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Настроения", "Жанры", "Занятия", "Эпохи").forEachIndexed { i, s ->
                                Column(Modifier.width(112.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF191919)).padding(10.dp)) {
                                    Text(s, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Box(Modifier.fillMaxWidth().padding(top = 12.dp).height(75.dp).clip(RoundedCornerShape(8.dp)).background(listOf(Color(0xFF304C9A), Color(0xFFB95CB7), Color(0xFFB5D833), Color(0xFF7E397A))[i]), contentAlignment = Alignment.Center) { Icon(Icons.Default.GraphicEq, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
                                }
                            }
                        }
                    }
                    item { Heading("Твоя музыка", "Все") }
                } else item { Heading(if (tab == 1) "Тренды твоей библиотеки" else "Результаты поиска", "Все") }
                if (list.isEmpty()) item { Box(Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.Center) { Text(if (tracks.isEmpty()) "Добавь аудиофайлы на устройство" else "Ничего не найдено", color = Grey) } }
                else items(if (tab == 0 && !search) list.take(8) else list, key = { it.id }) { t ->
                    Row(Modifier.fillMaxWidth().clickable { onPlay(t) }.padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(Color(0xFF344B7A), Color(0xFF29202D)))), contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(t.title, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(t.artist, color = Grey, fontSize = 12.sp, maxLines = 1) }
                        Text("⋯", color = Grey, fontSize = 23.sp)
                    }
                }
            }
            if (title.isNotBlank()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 7.dp).clip(RoundedCornerShape(17.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF49372C), Color(0xFF292929), Color(0xFF3C2C40))))) {
                    Box(Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(.15f))) { Box(Modifier.fillMaxWidth(if (duration > 0) (pos.toFloat()/duration).coerceIn(0f,1f) else 0f).fillMaxHeight().background(Gold)) }
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFFB75A7B)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Album, null, tint = Color.White) }
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(artist, color = Color.LightGray, fontSize = 11.sp, maxLines = 1) }
                        IconButton(onClick = { }) { Icon(Icons.Default.Favorite, null, tint = Gold) }
                        IconButton(onClick = { if (playing) player?.pause() else player?.play() }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(27.dp)) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(60.dp).background(Color(0xFF101010)), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.size(25.dp))
                Icon(Icons.Default.Podcasts, null, tint = Grey, modifier = Modifier.size(25.dp))
                Icon(Icons.Default.FavoriteBorder, null, tint = Grey, modifier = Modifier.size(25.dp))
            }
        }
    }
}
@Composable private fun Heading(text: String, action: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text("$action  ›", color = Grey, fontSize = 13.sp)
    }
}
