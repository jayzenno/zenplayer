package com.zenplayer.tv

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.zenplayer.tv.domain.model.Channel
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

/** Playback surface. Navigation/control chrome is owned by the TV shell. */
@Composable
fun ZenPlayerScreen(channel: Channel, settings: SettingsStore, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    when (settings.player.engine) {
        PlaybackEngine.EXO -> ExoPlayerView(channel, settings)
        PlaybackEngine.VLC -> VlcPlayerView(channel, settings)
        PlaybackEngine.EXTERNAL -> {
            val context = LocalContext.current
            LaunchedEffect(channel.streamUrl, settings.player.externalPlayerPackage) {
                launchExternalPlayer(context, channel.streamUrl, settings.player.externalPlayerPackage)
                onBack()
            }
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {}
        }
    }
}

@Composable
private fun ExoPlayerView(channel: Channel, settings: SettingsStore) {
    val context = LocalContext.current
    val player = remember(channel.streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(channel.streamUrl)))
            prepare()
            playWhenReady = settings.player.startLiveImmediately
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { useController = false; this.player = player; isFocusable = false; isFocusableInTouchMode = false } },
        update = { it.player = player },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun VlcPlayerView(channel: Channel, settings: SettingsStore) {
    val context = LocalContext.current
    val caching = networkCaching(settings)
    val hardwareAcceleration = settings.player.hardwareAcceleration
    val libVlc = remember(caching) { LibVLC(context, arrayListOf("--audio-time-stretch", "--network-caching=$caching")) }
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
    LaunchedEffect(channel.streamUrl, caching, hardwareAcceleration) {
        runCatching {
            mediaPlayer.attachViews(videoLayout, null, false, false)
            val media = Media(libVlc, Uri.parse(channel.streamUrl))
            media.setHWDecoderEnabled(hardwareAcceleration, false)
            media.addOption(":network-caching=$caching")
            mediaPlayer.media = media
            media.release()
            mediaPlayer.play()
        }.onFailure { Toast.makeText(context, "VLC konnte den Stream nicht starten", Toast.LENGTH_LONG).show() }
    }
    AndroidView(factory = { videoLayout }, modifier = Modifier.fillMaxSize())
}

private fun networkCaching(settings: SettingsStore): Int = when (settings.player.bufferMode) {
    BufferMode.LOW -> 300
    BufferMode.BALANCED -> 800
    BufferMode.HIGH -> 1800
    BufferMode.AUTO -> 800
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