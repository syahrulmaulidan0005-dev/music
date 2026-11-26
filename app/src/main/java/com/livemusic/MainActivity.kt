package com.livemusic

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { App() } }
    }
}

@Composable
fun App() {
    var started by remember { mutableStateOf(false) }
    if (started) LiveScreen() else SetupScreen { started = true }
}

@Composable
fun SetupScreen(onStart: () -> Unit) {
    val ctx = LocalContext.current
    var user by remember { mutableStateOf("") }
    val status by Store.status.collectAsState()
    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) AudioTap.start() }
    Column(Modifier.fillMaxSize().background(Color(0xFF0D0D10)).statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Live Music", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Siapkan dulu, lalu bagikan layar ini di TikTok LIVE.", color = Color.White.copy(.7f))
        Button(onClick = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, Modifier.fillMaxWidth()) {
            Text("1. Izinkan akses notifikasi (baca lagu)")
        }
        Button(onClick = { mic.launch(Manifest.permission.RECORD_AUDIO) }, Modifier.fillMaxWidth()) {
            Text("2. Izinkan audio (gelombang asli, opsional)")
        }
        OutlinedTextField(user, { user = it }, label = { Text("3. Username TikTok (tanpa @)") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = { if (user.isNotBlank()) LiveComments.connect(user); onStart() }, Modifier.fillMaxWidth()) {
            Text("Mulai tampilan live")
        }
        TextButton(onClick = {
            listOf("rina" to "lagunya enak banget", "dodi" to "salam dari Bandung", "sari" to "request dong kak", "agus" to "mantap")
                .forEach { Store.addComment(it.first, it.second) }; onStart()
        }) { Text("Coba dengan komentar demo") }
        if (status.isNotBlank()) Text(status, color = Color.White.copy(.7f), fontSize = 12.sp)
    }
}

private val glow = Brush.linearGradient(listOf(Color.White.copy(.26f), Color.White.copy(.08f)))
private fun Modifier.glass(shape: RoundedCornerShape) = this.clip(shape).background(glow, shape)
    .border(1.dp, Color.White.copy(.28f), shape)

@Composable
fun LiveScreen() {
    val track by Store.track.collectAsState()
    val playing by Store.playing.collectAsState()
    val pos by Store.position.collectAsState()
    val artistUrl by Store.artistUrl.collectAsState()
    val lyrics by Store.lyrics.collectAsState()
    val comments by Store.comments.collectAsState()
    val level by Store.level.collectAsState()
    val cover: Any? = artistUrl ?: track?.art

    Box(Modifier.fillMaxSize().background(Color(0xFF0D0D10))) {
        // Latar mengikuti gambar artis
        if (cover != null) AsyncImage(cover, null, Modifier.fillMaxSize().blur(40.dp), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x26000000), Color(0xA6000000)))))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.glass(RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF3B5C)))
                Spacer(Modifier.width(8.dp))
                Text("SEDANG DIPUTAR", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)
            }
            Spacer(Modifier.height(30.dp))
            Row(Modifier.fillMaxWidth().height(200.dp), verticalAlignment = Alignment.CenterVertically) {
                SleeveAndVinyl(cover, playing)
                Spacer(Modifier.width(16.dp))
                Lyrics(lyrics, pos, Modifier.weight(1f).fillMaxHeight())
            }
            Spacer(Modifier.height(20.dp))
            Text(track?.title ?: "Putar lagu di aplikasi musik", fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(track?.artist ?: "", fontSize = 15.sp, color = Color.White.copy(.8f))
            Spacer(Modifier.height(24.dp))
            Bars(playing, level)
            Row(Modifier.width(260.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmt(pos), fontSize = 11.sp, color = Color.White.copy(.7f))
                Text(fmt(track?.durationMs ?: 0), fontSize = 11.sp, color = Color.White.copy(.7f))
            }
            Spacer(Modifier.weight(1f))
            Comments(comments)
        }
    }
}

private fun fmt(ms: Long) = "%d:%02d".format(ms / 60000, ms / 1000 % 60)

@Composable
fun SleeveAndVinyl(cover: Any?, playing: Boolean) {
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) {
        var last = 0L
        while (playing) withFrameNanos { t -> if (last != 0L) angle = (angle + (t - last) / 1e9f * 45f) % 360f; last = t }
    }
    Box(Modifier.width(196.dp).height(130.dp)) {
        Canvas(Modifier.offset(x = 68.dp).size(128.dp).graphicsLayer { rotationZ = angle }) {
            val c = center; val r = size.minDimension / 2
            drawCircle(Color(0xFF0B0B0B), r, c)
            var g = r - 3f; while (g > r * 0.4f) { drawCircle(Color(0xFF1D1D1D), g, c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f)); g -= 4f }
            drawCircle(Color(0xFFC0603A), r * 0.36f, c)
            drawCircle(Color(0xFF0D0D10), r * 0.06f, c)
            drawArc(Color.White.copy(.14f), 20f, 40f, true, Offset.Zero, size)
        }
        Box(Modifier.size(130.dp).shadow(14.dp, RoundedCornerShape(5.dp)).clip(RoundedCornerShape(5.dp)).background(Color(0xFF2D3A66))) {
            if (cover != null) AsyncImage(cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
fun Lyrics(lines: List<LyricLine>, pos: Long, modifier: Modifier) {
    val idx = lines.indexOfLast { it.ms <= pos }
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        if (lines.isEmpty()) { Text("Lirik tidak tersedia", color = Color.White.copy(.4f), fontSize = 13.sp); return@Box }
        AnimatedContent(idx, transitionSpec = {
            (slideInVertically { it / 4 } + fadeIn()) togetherWith (slideOutVertically { -it / 4 } + fadeOut())
        }, label = "lyrics") { i ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (k in -2..2) lines.getOrNull(i + k)?.let {
                    Text(it.text, fontSize = if (k == 0) 17.sp else 13.sp, fontWeight = if (k == 0) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White.copy(when (abs(k)) { 0 -> 1f; 1 -> .5f; else -> .3f }), maxLines = 3, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
fun Bars(playing: Boolean, level: Float?) {
    val n = 27
    val rnd = remember { Random(7) }
    val env = remember { FloatArray(n) { i -> 0.3f + 0.7f * abs(sin(i / n.toFloat() * 6.9f + 1.1f)) } }
    val sp = remember { FloatArray(n) { 2f + rnd.nextFloat() * 3f } }
    val ph = remember { FloatArray(n) { rnd.nextFloat() * 6f } }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (true) withFrameMillis { t = it / 1000f } }
    val amp by animateFloatAsState(if (!playing) 0.08f else (level ?: 0.65f).coerceAtLeast(0.25f), tween(120), label = "amp")
    Canvas(Modifier.size(260.dp, 110.dp)) {
        val pitch = size.width / n; val bw = 5.5.dp.toPx()
        for (i in 0 until n) {
            val h = (size.height * env[i] * (0.35f + 0.65f * abs(sin(t * sp[i] + ph[i]))) * amp).coerceAtLeast(12.dp.toPx())
            drawRoundRect(Color.White, Offset(i * pitch + (pitch - bw) / 2, (size.height - h) / 2), Size(bw, h), CornerRadius(bw / 2))
        }
    }
}

@Composable
fun Comments(list: List<Comment>) {
    val shown = list.takeLast(4).reversed()   // terbaru di paling bawah
    LazyColumn(reverseLayout = true, userScrollEnabled = false, verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 230.dp)) {
        items(shown, key = { it.id }) { CommentCard(it, Modifier.animateItem()) }
    }
}

/** Transisi tiap komentar: meluncur naik, membesar, dan memudar masuk; yang lama bergeser dan memudar keluar. */
@Composable
fun CommentCard(c: Comment, modifier: Modifier) {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { on = true }
    val a by animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow), label = "in")
    Row(modifier.fillMaxWidth().graphicsLayer {
        alpha = a.coerceIn(0f, 1f); translationY = (1f - a) * 90f
        val s = 0.92f + 0.08f * a; scaleX = s; scaleY = s
    }.glass(RoundedCornerShape(22.dp)).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(.3f)), contentAlignment = Alignment.Center) {
            Text(c.user.take(1).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text("@${c.user}  ${c.text}", fontSize = 13.sp, maxLines = 2)
    }
}
