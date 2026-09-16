package com.zenplayer.tv

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.zenplayer.tv.domain.model.Channel
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

@Composable
fun ZenPlayerScreen(channel: Channel, settings: SettingsStore, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    when (settings.player.engine) {
        PlaybackEngine.EXO -> ExoPlayerView(channel, settings, onBack)
        PlaybackEngine.VLC -> VlcPlayerView(channel, settings, onBack)
        PlaybackEngine.EXTERNAL -> {
            LaunchedEffect(channel.streamUrl, settings.player.externalPlayerPackage) {
                launchExternalPlayer(LocalContext.current, channel.streamUrl, settings.player.externalPlayerPackage)
                onBack()
            }
            PlayerLaunchFallback(channel, settings.player.externalPlayerPackage)
        }
    }
}

@Composable
private fun ExoPlayerView(channel: Channel, settings: SettingsStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(channel.streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(channel.streamUrl)))
            prepare()
            playWhenReady = settings.player.startLiveImmediately
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { PlayerView(it).apply { useController = true; this.player = player; requestFocus() } },
            modifier = Modifier.fillMaxSize()
        )
        PlayerTitle(channel, "ExoPlayer", Modifier.align(Alignment.TopStart))
    }
}

@Composable
private fun VlcPlayerView(channel: Channel, settings: SettingsStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val libVlc = remember { LibVLC(context, arrayListOf("--audio-time-stretch", "--network-caching=${networkCaching(settings)}")) }
    val mediaPlayer = remember(libVlc) { MediaPlayer(libVlc) }
    val videoLayout = remember { VLCVideoLayout(context) }

    DisposableEffect(mediaPlayer) {
        onDispose {
            runCatching { mediaPlayer.stop() }
            runCatching { mediaPlayer.detachViews() }
            runCatching { mediaPlayer.release() }
            runCatching { libVlc.release() }
        }
    }
    LaunchedEffect(channel.streamUrl) {
        runCatching {
            mediaPlayer.attachViews(videoLayout, null, false, false)
            val media = Media(libVlc, Uri.parse(channel.streamUrl))
            media.setHWDecoderEnabled(settings.player.hardwareAcceleration, false)
            media.addOption(":network-caching=${networkCaching(settings)}")
            mediaPlayer.media = media
            media.release()
            mediaPlayer.play()
        }.onFailure { Toast.makeText(context, "VLC konnte den Stream nicht starten", Toast.LENGTH_LONG).show() }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { videoLayout }, modifier = Modifier.fillMaxSize())
        PlayerTitle(channel, "VLC / LibVLC", Modifier.align(Alignment.TopStart))
    }
}

private fun networkCaching(settings: SettingsStore): Int = when (settings.player.bufferMode) {
    BufferMode.LOW -> 300
    BufferMode.BALANCED -> 800
    BufferMode.HIGH -> 1800
    BufferMode.AUTO -> 800
}

@Composable
private fun PlayerTitle(channel: Channel, engine: String, modifier: Modifier) {
    Row(modifier.padding(24.dp).background(Color.Black.copy(.48f)).padding(horizontal = 14.dp, vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(300.dp)) {
            Text(channel.name, color = Color.White, fontSize = 18.sp)
            Text(engine, color = Color.White.copy(.65f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun PlayerLaunchFallback(channel: Channel, selectedPackage: String?) {
    Box(Modifier.fillMaxSize().background(Color.Black).focusable(), Alignment.Center) {
        Text("Starte externen Player…", color = Color.White, fontSize = 20.sp)
    }
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
