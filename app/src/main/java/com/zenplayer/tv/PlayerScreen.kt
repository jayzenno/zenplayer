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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.zenplayer.tv.domain.model.Channel
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private class PlayerController(
    val playPause: () -> Unit,
    val seek: (Long) -> Unit,
    val isPlaying: () -> Boolean,
    val position: () -> Long,
    val duration: () -> Long
)

@Composable
fun ZenPlayerScreen(
    channel: Channel,
    settings: SettingsStore,
    onBack: () -> Unit,
    onRemoteKey: (Key) -> Boolean = { false },
    showChrome: Boolean = true
) {
    val focusRequester = remember { FocusRequester() }
    var controller by remember(channel.streamUrl) { mutableStateOf<PlayerController?>(null) }
    var chrome by remember(channel.streamUrl) { mutableStateOf(showChrome) }
    var notice by remember(channel.streamUrl) { mutableStateOf("") }
    var noticeUntil by remember(channel.streamUrl) { mutableLongStateOf(0L) }
    var playing by remember(channel.streamUrl) { mutableStateOf(true) }
    var positionMs by remember(channel.streamUrl) { mutableLongStateOf(0L) }
    var durationMs by remember(channel.streamUrl) { mutableLongStateOf(0L) }

    fun notify(text: String) {
        notice = text
        noticeUntil = System.currentTimeMillis() + 1400L
        chrome = true
    }

    BackHandler {
        if (chrome) chrome = false else onBack()
    }
    LaunchedEffect(channel.streamUrl) { runCatching { focusRequester.requestFocus() } }
    LaunchedEffect(noticeUntil) {
        val remaining = noticeUntil - System.currentTimeMillis()
        if (remaining > 0L) {
            kotlinx.coroutines.delay(remaining)
            noticeUntil = 0L
        }
    }
    LaunchedEffect(chrome) {
        if (chrome) {
            kotlinx.coroutines.delay(5500L)
            chrome = false
        }
    }
    LaunchedEffect(controller) {
        while (controller != null) {
            val c = controller ?: break
            playing = c.isPlaying()
            positionMs = c.position().coerceAtLeast(0L)
            durationMs = c.duration().coerceAtLeast(0L)
            kotlinx.coroutines.delay(250L)
        }
    }

    val hasTimeline = durationMs > 0L
    val progress = if (hasTimeline) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> return@onKeyEvent onRemoteKey(Key.DirectionUp)
                    Key.DirectionDown -> return@onKeyEvent onRemoteKey(Key.DirectionDown)
                    Key.DirectionLeft -> {
                        controller?.seek(-10_000L)
                        notify("−10 Sekunden")
                        true
                    }
                    Key.DirectionRight -> {
                        controller?.seek(10_000L)
                        notify("+10 Sekunden")
                        true
                    }
                    Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                        controller?.playPause()
                        playing = controller?.isPlaying?.invoke() == true
                        notify(if (playing) "Wiedergabe" else "Pause")
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        chrome = !chrome
                        true
                    }
                    else -> onRemoteKey(event.key)
                }
            }
    ) {
        when (settings.player.engine) {
            PlaybackEngine.EXO -> ExoPlayerView(channel, settings) { controller = it }
            PlaybackEngine.VLC -> VlcPlayerView(channel, settings) { controller = it }
            PlaybackEngine.EXTERNAL -> {
                val context = LocalContext.current
                LaunchedEffect(channel.streamUrl, settings.player.externalPlayerPackage) {
                    launchExternalPlayer(context, channel.streamUrl, settings.player.externalPlayerPackage)
                    onBack()
                }
            }
        }

        if (chrome) {
            Box(Modifier.fillMaxWidth().padding(26.dp), contentAlignment = Alignment.TopStart) {
                Row(
                    Modifier
                        .width(560.dp)
                        .background(Color(0xE8171D29), RoundedCornerShape(26.dp))
                        .border(1.dp, Color.White.copy(.13f), RoundedCornerShape(26.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!channel.logoUrl.isNullOrBlank()) {
                        coil.compose.AsyncImage(channel.logoUrl, channel.name, Modifier.size(72.dp))
                    } else {
                        Box(Modifier.size(72.dp).background(Color.White.copy(.08f), RoundedCornerShape(18.dp)), Alignment.Center) {
                            Text(initialsPlayer(channel.name), color = Color.White, fontSize = 20.sp)
                        }
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(channel.name, color = Color.White, fontSize = 22.sp)
                        Text(channel.group ?: "Live TV", color = Color.White.copy(.58f), fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (channel.isCatchupCapable) "CATCHUP" else "● LIVE", color = Color(0xFF70E6FF), fontSize = 10.sp)
                            Text(SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date()), color = Color.White.copy(.58f), fontSize = 10.sp)
                            if (hasTimeline) Text("${formatPlayerTime(positionMs)} / ${formatPlayerTime(durationMs)}", color = Color.White.copy(.58f), fontSize = 10.sp)
                        }
                    }
                }
            }

            Box(Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, bottom = 28.dp), contentAlignment = Alignment.BottomStart) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xD9121721), RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(.10f), RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    if (hasTimeline) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(formatPlayerTime(positionMs), color = Color.White.copy(.72f), fontSize = 10.sp)
                            Spacer(Modifier.width(10.dp))
                            Box(Modifier.weight(1f).height(6.dp).background(Color.White.copy(.12f), RoundedCornerShape(8.dp))) {
                                Box(Modifier.fillMaxWidth(progress).height(6.dp).background(Color(0xFF70E6FF), RoundedCornerShape(8.dp)))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(formatPlayerTime(durationMs), color = Color.White.copy(.52f), fontSize = 10.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).background(Color.White.copy(.08f), RoundedCornerShape(14.dp)), Alignment.Center) {
                            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White)
                        }
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(if (playing) "Wiedergabe läuft" else "Pausiert", color = Color.White, fontSize = 14.sp)
                            Text("← / →  10 Sek.   ·   ↑ / ↓  Sender   ·   OK  Overlay", color = Color.White.copy(.52f), fontSize = 10.sp)
                        }
                        Text(if (channel.isCatchupCapable) "CATCHUP" else "LIVE", color = Color(0xFF70E6FF), fontSize = 10.sp)
                    }
                }
            }
        }

        if (noticeUntil > System.currentTimeMillis()) {
            Box(Modifier.fillMaxWidth().padding(top = 112.dp), contentAlignment = Alignment.TopCenter) {
                Text(notice, color = Color.White, fontSize = 13.sp, modifier = Modifier.background(Color(0xEE111722), RoundedCornerShape(16.dp)).border(1.dp, Color.White.copy(.14f), RoundedCornerShape(16.dp)).padding(horizontal = 18.dp, vertical = 10.dp))
            }
        }
    }
}

@Composable
private fun ExoPlayerView(channel: Channel, settings: SettingsStore, onReady: (PlayerController) -> Unit) {
    val context = LocalContext.current
    val player = remember(channel.streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(channel.streamUrl)))
            prepare()
            playWhenReady = settings.player.startLiveImmediately
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(player) {
        onReady(PlayerController(
            playPause = { if (player.isPlaying) player.pause() else player.play() },
            seek = { delta ->
                val duration = player.duration
                val target = player.currentPosition + delta
                player.seekTo(if (duration > 0L) target.coerceIn(0L, duration) else target.coerceAtLeast(0L))
            },
            isPlaying = { player.isPlaying },
            position = { player.currentPosition },
            duration = { player.duration.takeIf { it > 0L } ?: 0L }
        ))
    }
    AndroidView(factory = { ctx -> PlayerView(ctx).apply { useController = false; this.player = player; isFocusable = false; isFocusableInTouchMode = false } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun VlcPlayerView(channel: Channel, settings: SettingsStore, onReady: (PlayerController) -> Unit) {
    val context = LocalContext.current
    val caching = when (settings.player.bufferMode) {
        BufferMode.LOW -> 300
        BufferMode.BALANCED, BufferMode.AUTO -> 800
        BufferMode.HIGH -> 1800
    }
    val libVlc = remember(caching) { LibVLC(context, arrayListOf("--audio-time-stretch", "--network-caching=$caching")) }
    val mediaPlayer = remember(libVlc) { MediaPlayer(libVlc) }
    val videoLayout = remember { VLCVideoLayout(context) }
    DisposableEffect(mediaPlayer) {
        onDispose { runCatching { mediaPlayer.stop() }; runCatching { mediaPlayer.detachViews() }; runCatching { mediaPlayer.release() }; runCatching { libVlc.release() } }
    }
    LaunchedEffect(channel.streamUrl, caching) {
        runCatching {
            mediaPlayer.attachViews(videoLayout, null, false, false)
            val media = Media(libVlc, Uri.parse(channel.streamUrl))
            media.addOption(":network-caching=$caching")
            mediaPlayer.media = media
            media.release()
            mediaPlayer.play()
            onReady(PlayerController(
                playPause = { if (mediaPlayer.isPlaying) mediaPlayer.pause() else mediaPlayer.play() },
                seek = { delta ->
                    val length = mediaPlayer.length
                    val target = mediaPlayer.time + delta
                    mediaPlayer.time = if (length > 0L) target.coerceIn(0L, length) else target.coerceAtLeast(0L)
                },
                isPlaying = { mediaPlayer.isPlaying },
                position = { mediaPlayer.time },
                duration = { mediaPlayer.length.takeIf { it > 0L } ?: 0L }
            ))
        }.onFailure { Toast.makeText(context, "VLC konnte den Stream nicht starten", Toast.LENGTH_LONG).show() }
    }
    AndroidView(factory = { videoLayout }, modifier = Modifier.fillMaxSize())
}

fun launchExternalPlayer(context: Context, url: String, packageName: String?): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        type = "video/*"
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!packageName.isNullOrBlank()) setPackage(packageName)
    }
    return runCatching {
        if (intent.resolveActivity(context.packageManager) == null) return false
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

private fun formatPlayerTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) String.format(Locale.GERMANY, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.GERMANY, "%02d:%02d", minutes, seconds)
}

private fun initialsPlayer(name: String): String = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercaseChar().toString() }
