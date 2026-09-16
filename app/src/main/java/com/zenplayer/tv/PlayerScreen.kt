package com.zenplayer.tv

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.zenplayer.tv.domain.model.Channel
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

private class PlayerController(val playPause: () -> Unit, val seek: (Long) -> Unit)

@Composable
fun ZenPlayerScreen(channel: Channel, settings: SettingsStore, onBack: () -> Unit, onRemoteKey: (Key) -> Boolean = { false }, showChrome: Boolean = false) {
    val focusRequester = remember { FocusRequester() }
    var controller by remember(channel.streamUrl) { mutableStateOf<PlayerController?>(null) }
    var notice by remember(channel.streamUrl) { mutableStateOf("") }
    var noticeUntil by remember(channel.streamUrl) { mutableLongStateOf(0L) }
    fun notify(text: String) { notice = text; noticeUntil = System.currentTimeMillis() + 1200L }
    BackHandler(onBack = onBack)
    LaunchedEffect(channel.streamUrl) { runCatching { focusRequester.requestFocus() } }
    LaunchedEffect(noticeUntil) { if (noticeUntil > System.currentTimeMillis()) { kotlinx.coroutines.delay((noticeUntil - System.currentTimeMillis()).coerceAtLeast(1L)); noticeUntil = 0L } }
    Box(Modifier.fillMaxSize().background(Color.Black).focusRequester(focusRequester).focusable().onKeyEvent { event ->
        if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
        when (event.key) {
            Key.DirectionUp -> { onRemoteKey(Key.DirectionUp) }
            Key.DirectionDown -> { onRemoteKey(Key.DirectionDown) }
            Key.DirectionLeft -> { controller?.seek(-10_000L); notify("−10 Sekunden"); true }
            Key.DirectionRight -> { controller?.seek(10_000L); notify("+10 Sekunden"); true }
            Key.MediaPlayPause -> { controller?.playPause(); notify("Play / Pause"); true }
            Key.MediaPlay -> { controller?.playPause(); notify("Wiedergabe"); true }
            Key.MediaPause -> { controller?.playPause(); notify("Pause"); true }
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { if (showChrome) { notify("Player"); true } else onRemoteKey(event.key) }
            else -> onRemoteKey(event.key)
        }
    }) {
        when (settings.player.engine) {
            PlaybackEngine.EXO -> ExoPlayerView(channel, settings) { controller = it }
            PlaybackEngine.VLC -> VlcPlayerView(channel, settings) { controller = it }
            PlaybackEngine.EXTERNAL -> { val context = LocalContext.current; LaunchedEffect(channel.streamUrl, settings.player.externalPlayerPackage) { launchExternalPlayer(context, channel.streamUrl, settings.player.externalPlayerPackage); onBack() } }
        }
        if (showChrome && noticeUntil > System.currentTimeMillis()) Box(Modifier.fillMaxWidth().padding(top = 28.dp), contentAlignment = Alignment.TopCenter) { Text(notice, color = Color.White, fontSize = 13.sp, modifier = Modifier.background(Color(0xE0161923), RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(.12f), RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 10.dp)) }
    }
}

@Composable private fun ExoPlayerView(channel: Channel, settings: SettingsStore, onReady: (PlayerController) -> Unit) {
    val context = LocalContext.current
    val player = remember(channel.streamUrl) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(Uri.parse(channel.streamUrl))); prepare(); playWhenReady = settings.player.startLiveImmediately } }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(player) { onReady(PlayerController({ if (player.isPlaying) player.pause() else player.play() }, { d -> player.seekTo((player.currentPosition + d).coerceAtLeast(0L)) })) }
    AndroidView(factory = { ctx -> PlayerView(ctx).apply { useController = false; this.player = player; isFocusable = false; isFocusableInTouchMode = false } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
}

@Composable private fun VlcPlayerView(channel: Channel, settings: SettingsStore, onReady: (PlayerController) -> Unit) {
    val context = LocalContext.current
    val caching = when (settings.player.bufferMode) { BufferMode.LOW -> 300; BufferMode.BALANCED, BufferMode.AUTO -> 800; BufferMode.HIGH -> 1800 }
    val libVlc = remember(caching) { LibVLC(context, arrayListOf("--audio-time-stretch", "--network-caching=$caching")) }
    val mediaPlayer = remember(libVlc) { MediaPlayer(libVlc) }
    val videoLayout = remember { VLCVideoLayout(context) }
    DisposableEffect(mediaPlayer) { onDispose { runCatching { mediaPlayer.stop() }; runCatching { mediaPlayer.detachViews() }; runCatching { mediaPlayer.release() }; runCatching { libVlc.release() } } }
    LaunchedEffect(channel.streamUrl, caching) { runCatching { mediaPlayer.attachViews(videoLayout, null, false, false); val media = Media(libVlc, Uri.parse(channel.streamUrl)); media.addOption(":network-caching=$caching"); mediaPlayer.media = media; media.release(); mediaPlayer.play(); onReady(PlayerController({ if (mediaPlayer.isPlaying) mediaPlayer.pause() else mediaPlayer.play() }, { d -> mediaPlayer.time = (mediaPlayer.time + d).coerceAtLeast(0L) })) }.onFailure { Toast.makeText(context, "VLC konnte den Stream nicht starten", Toast.LENGTH_LONG).show() } }
    AndroidView(factory = { videoLayout }, modifier = Modifier.fillMaxSize())
}

fun launchExternalPlayer(context: Context, url: String, packageName: String?): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { type = "video/*"; addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); if (!packageName.isNullOrBlank()) setPackage(packageName) }
    return runCatching { if (intent.resolveActivity(context.packageManager) == null) return false; context.startActivity(intent); true }.getOrDefault(false)
}
