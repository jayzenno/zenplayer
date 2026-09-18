package com.zenplayer.tv.domain.provider

/**
 * Abstraction for a VOD (movies/series) source. ZenPlayer's VOD section is built around the
 * Nuvio plugin/collection ecosystem; this interface is the seam a NuvioProvider (or any other
 * VOD backend) plugs into so the UI never depends on a single provider. See AGENTS.md.
 */
interface VodProvider {
    val id: String
    val displayName: String

    suspend fun isAvailable(): Boolean
    suspend fun browseCollections(): Result<List<VodCollection>>
    suspend fun search(query: String): Result<List<VodItem>>
}

data class VodCollection(
    val id: String,
    val title: String,
    val items: List<VodItem>,
)

enum class VodKind { MOVIE, SERIES }

data class VodItem(
    val id: String,
    val title: String,
    val kind: VodKind,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val overview: String? = null,
    val year: Int? = null,
    val genres: List<String> = emptyList(),
)

/**
 * Placeholder until the Nuvio plugin/collection surface is wired up (Phase 5). Reports itself
 * unavailable rather than fabricating data or endpoints.
 */
class NuvioProvider : VodProvider {
    override val id: String = "nuvio"
    override val displayName: String = "Nuvio"

    override suspend fun isAvailable(): Boolean = false
    override suspend fun browseCollections(): Result<List<VodCollection>> =
        Result.failure(UnsupportedOperationException("Nuvio integration not yet implemented"))
    override suspend fun search(query: String): Result<List<VodItem>> =
        Result.failure(UnsupportedOperationException("Nuvio integration not yet implemented"))
}
