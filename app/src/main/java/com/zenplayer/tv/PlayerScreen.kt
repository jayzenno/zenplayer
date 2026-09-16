package com.zenplayer.tv

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.zenplayer.tv.domain.model.Channel
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

private val PlayerText = Color(0xFFF5F6FA)
private val PlayerSecondary = Color(0xFF9EA3B3)
private val PlayerPanel = Color(0xE011141D)

@Composable
fun ZenPlayerScreen(
    channel: Channel,
    settings: SettingsStore,
    onBack: () -> Unit,
    onRemoteKey: (Key) -> Boolean = { false },
    showChrome: Boolean = true
) {
    var controls by remember(channel.streamUrl) { mutableStateOf(false) }
    var info by remember(channel.streamUrl) { mutableStateOf(false) }
    var lastAction by remember { mutableStateOf("") }
    var actionUntil by remember { mutableLongStateOf(0L) }
    val focusRequester = remember { FocusRequester() }

    fun showAction(text: String) {
        if (!showChrome) return
        lastAction = text
        actionUntil = System.currentTimeMillis() + 1400L
    }

    BackHandler {
        when {
            info -> info = false
            controls -> controls = false
            else -> onBack()
        }
    }

    LaunchedEffect(channel.streamUrl) { runCatching { focusRequester.requestFocus() } }
    LaunchedEffect(actionUntil) {
        if (actionUntil > System.currentTimeMillis()) {
            kotlinx.coroutines.delay((actionUntil - System.currentTimeMillis()).coerceAtLeast(1L))
            actionUntil = 0L
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black).focusRequester(focusRequester).focusable().onKeyEvent { event ->
            if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
            when (event.key) {
                Key.Back -> { onBack(); true }
                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> if (showChrome) { controls = !controls; true } else onRemoteKey(event.key)
                Key.MediaPlayPause -> { showAction("Play / Pause"); onRemoteKey(event.key) }
                Key.MediaPlay -> { showAction("Wiedergabe"); onRemoteKey(event.key) }
                Key.MediaPause -> { showAction("Pause"); onRemoteKey(event.key) }
                Key.DirectionLeft -> { showAction("−10 Sekunden"); onRemoteKey(event.key) }
                Key.DirectionRight -> { showAction("+10 Sekunden"); onRemoteKey(event.key) }
                Key.DirectionUp -> { showAction("Nächster Sender"); onRemoteKey(event.key) }
                Key.DirectionDown -> { showAction("Vorheriger Sender"); onRemoteKey(event.key) }
                else -> onRemoteKey(event.key)
            }
        }
    ) {
        when (settings.player.engine) {
            PlaybackEngine.EXO -> ExoPlayerView(channel, settings)
            PlaybackEngine.VLC -> VlcPlayerView(channel, settings)
            PlaybackEngine.EXTERNAL -> {
                val context = LocalContext.current
                LaunchedEffect(channel.streamUrl, settings.player.externalPlayerPackage) {
                    launchExternalPlayer(context, channel.streamUrl, settings.player.externalPlayerPackage)
                    onBack()
                }
                Box(Modifier.fillMaxSize().background(Color.Black))
            }
        }

        if (showChrome && controls) PlayerControls(channel, playerAccent(settings), onClose = { controls = false }, onInfo = { info = true }, onAction = ::showAction)
        if (showChrome && info) PlayerInfo(channel, playerAccent(settings), onDismiss = { info = false })
        if (showChrome && actionUntil > System.currentTimeMillis()) {
            Box(Modifier.fillMaxWidth().padding(top = 28.dp), contentAlignment = Alignment.TopCenter) {
                Text(lastAction, color = PlayerText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.background(PlayerPanel, RoundedCornerShape(18.dp)).border(1.dp, playerAccent(settings).copy(.35f), RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 10.dp))
            }
        }
    }
}

@Composable private fun PlayerControls(channel: Channel, accent: Color, onClose: () -> Unit, onInfo: () -> Unit, onAction: (String) -> Unit) { val first = remember { FocusRequester() }; LaunchedEffect(Unit) { runCatching { first.requestFocus() } }; Box(Modifier.fillMaxSize().background(Color.Black.copy(.22f))) { Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(22.dp).background(PlayerPanel, RoundedCornerShape(24.dp)).border(1.dp, Color.White.copy(.09f), RoundedCornerShape(24.dp)).padding(18.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(channel.name, color = PlayerText, fontSize = 19.sp, fontWeight = FontWeight.Bold); Text(channel.group ?: "Live TV", color = PlayerSecondary, fontSize = 11.sp) }; Text("LIVE", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(12.dp)); Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(.10f), RoundedCornerShape(2.dp))); Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { PlayerButton(Icons.Default.SkipPrevious, "−10s", accent, first) { onAction("−10 Sekunden") }; PlayerButton(Icons.Default.PlayArrow, "Play / Pause", accent) { onAction("Play/Pause") }; PlayerButton(Icons.Default.SkipNext, "+10s", accent) { onAction("+10 Sekunden") }; PlayerButton(Icons.Default.Info, "Info", accent) { onInfo() }; PlayerButton(Icons.Default.Settings, "Optionen", accent) { onAction("Optionen") }; PlayerButton(Icons.Default.Pause, "Schließen", accent) { onClose() } }; Spacer(Modifier.height(8.dp)); Text("←/→ 10s · ↑/↓ Sender · OK Overlay · Zurück schließen", color = PlayerSecondary, fontSize = 10.sp) } } }
@Composable private fun PlayerButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, accent: Color, requester: FocusRequester? = null, onClick: () -> Unit) { var focused by remember { mutableStateOf(false) }; Box(Modifier.width(104.dp).height(52.dp).then(if (requester != null) Modifier.focusRequester(requester) else Modifier).background(if (focused) accent.copy(.18f) else Color.White.copy(.055f), RoundedCornerShape(14.dp)).border(if (focused) 2.dp else 1.dp, if (focused) accent else Color.White.copy(.07f), RoundedCornerShape(14.dp)).focusable().onFocusChanged { focused = it.isFocused }.onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && (e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter)) { onClick(); true } else false }, Alignment.Center) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = if (focused) PlayerText else accent); Spacer(Modifier.width(7.dp)); Text(label, color = PlayerText, fontSize = 10.sp, fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal) } } }
@Composable private fun PlayerInfo(channel: Channel, accent: Color, onDismiss: () -> Unit) { Box(Modifier.fillMaxSize().background(Color.Black.copy(.65f)), contentAlignment = Alignment.Center) { Column(Modifier.width(480.dp).background(PlayerPanel, RoundedCornerShape(24.dp)).border(1.dp, accent.copy(.35f), RoundedCornerShape(24.dp)).padding(22.dp)) { Text(channel.name, color = PlayerText, fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("Senderinformationen", color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(14.dp)); Text("Gruppe: ${channel.group ?: "Live TV"}", color = PlayerSecondary, fontSize = 12.sp); Text("Stream: ${channel.streamUrl.take(55)}${if (channel.streamUrl.length > 55) "…" else ""}", color = PlayerSecondary, fontSize = 10.sp); Spacer(Modifier.height(16.dp)); Text("OK / Zurück schließen", color = PlayerText, fontSize = 11.sp) } } }
private fun playerAccent(settings: SettingsStore): Color = when (settings.ui.theme) { ZenTheme.AURORA -> Color(0xFF70E6FF); ZenTheme.OBSIDIAN -> Color(0xFFB7B9FF); ZenTheme.FROST -> Color(0xFFB9E9FF); ZenTheme.AMBER -> Color(0xFFFFC66D) }

@Composable private fun ExoPlayerView(channel: Channel, settings: SettingsStore) { val context = LocalContext.current; val player = remember(channel.streamUrl) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(Uri.parse(channel.streamUrl))); prepare(); playWhenReady = settings.player.startLiveImmediately } }; DisposableEffect(player) { onDispose { player.release() } }; AndroidView(factory = { ctx -> PlayerView(ctx).apply { useController = false; this.player = player; isFocusable = false; isFocusableInTouchMode = false } }, update = { it.player = player }, modifier = Modifier.fillMaxSize()) }
@Composable private fun VlcPlayerView(channel: Channel, settings: SettingsStore) { val context = LocalContext.current; val caching = networkCaching(settings); val hardwareAcceleration = settings.player.hardwareAcceleration; val libVlc = remember(caching) { LibVLC(context, arrayListOf("--audio-time-stretch", "--network-caching=$caching")) }; val mediaPlayer = remember(libVlc) { MediaPlayer(libVlc) }; val videoLayout = remember { VLCVideoLayout(context) }; DisposableEffect(mediaPlayer) { onDispose { runCatching { mediaPlayer.stop() }; runCatching { mediaPlayer.detachViews() }; runCatching { mediaPlayer.release() }; runCatching { libVlc.release() } } }; LaunchedEffect(channel.streamUrl, caching, hardwareAcceleration) { runCatching { mediaPlayer.attachViews(videoLayout, null, false, false); val media = Media(libVlc, Uri.parse(channel.streamUrl)); media.setHWDecoderEnabled(hardwareAcceleration, false); media.addOption(":network-caching=$caching"); mediaPlayer.media = media; media.release(); mediaPlayer.play() }.onFailure { Toast.makeText(context, "VLC konnte den Stream nicht starten", Toast.LENGTH_LONG).show() } }; AndroidView(factory = { videoLayout }, modifier = Modifier.fillMaxSize()) }
private fun networkCaching(settings: SettingsStore): Int = when (settings.player.bufferMode) { BufferMode.LOW -> 300; BufferMode.BALANCED -> 800; BufferMode.HIGH -> 1800; BufferMode.AUTO -> 800 }
fun launchExternalPlayer(context: Context, url: String, packageName: String?): Boolean { val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { type = "video/*"; addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); if (!packageName.isNullOrBlank()) setPackage(packageName) }; return runCatching { if (intent.resolveActivity(context.packageManager) == null) return false; context.startActivity(intent); true }.getOrDefault(false) }
