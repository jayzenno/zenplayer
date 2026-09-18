package com.zenplayer.tv.domain.provider

/**
 * Abstraction shared by every music backend (EclipseMusic, Spotify, Apple Music, ...).
 * The Music tile lists providers through this interface so new backends never require
 * UI changes. See AGENTS.md for the provider architecture rule.
 */
interface MusicProvider {
    val id: String
    val displayName: String

    suspend fun isConnected(): Boolean
    suspend fun library(): Result<List<MusicPlaylist>>
    suspend fun search(query: String): Result<List<MusicTrack>>
}

data class MusicPlaylist(val id: String, val title: String, val trackCount: Int)

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val durationMs: Long = 0,
)

/** EclipseMusic has top integration priority (see AGENTS.md) but is not wired up yet. */
class EclipseMusicProvider : MusicProvider {
    override val id: String = "eclipse_music"
    override val displayName: String = "EclipseMusic"
    override suspend fun isConnected(): Boolean = false
    override suspend fun library(): Result<List<MusicPlaylist>> =
        Result.failure(UnsupportedOperationException("EclipseMusic bridge not yet integrated"))
    override suspend fun search(query: String): Result<List<MusicTrack>> =
        Result.failure(UnsupportedOperationException("EclipseMusic bridge not yet integrated"))
}

/** Requires official Spotify Web API / App Remote SDK auth; not implemented yet. */
class SpotifyProvider : MusicProvider {
    override val id: String = "spotify"
    override val displayName: String = "Spotify"
    override suspend fun isConnected(): Boolean = false
    override suspend fun library(): Result<List<MusicPlaylist>> =
        Result.failure(UnsupportedOperationException("Spotify login not yet implemented"))
    override suspend fun search(query: String): Result<List<MusicTrack>> =
        Result.failure(UnsupportedOperationException("Spotify login not yet implemented"))
}

/** Requires MusicKit auth; not implemented yet. */
class AppleMusicProvider : MusicProvider {
    override val id: String = "apple_music"
    override val displayName: String = "Apple Music"
    override suspend fun isConnected(): Boolean = false
    override suspend fun library(): Result<List<MusicPlaylist>> =
        Result.failure(UnsupportedOperationException("Apple Music login not yet implemented"))
    override suspend fun search(query: String): Result<List<MusicTrack>> =
        Result.failure(UnsupportedOperationException("Apple Music login not yet implemented"))
}
