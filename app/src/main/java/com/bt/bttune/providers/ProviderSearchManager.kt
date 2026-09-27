package com.bt.bttune.providers

import android.content.Context
import android.util.Log
import com.bt.bttune.constants.EnableJioSaavnKey
import com.bt.bttune.constants.HideExplicitKey
import com.bt.bttune.innertube.YouTube
import com.bt.bttune.innertube.models.SongItem
import com.bt.bttune.innertube.models.filterExplicit
import com.bt.bttune.jiosaavn.JioSaavnApi
import com.bt.bttune.models.toMediaMetadata
import com.bt.bttune.songs.IdentifiedSong
import com.bt.bttune.songs.SongMatchEngine
import com.bt.bttune.utils.dataStore
import com.bt.bttune.utils.getSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProviderSearchManager(private val context: Context) {

    companion object {
        private const val TAG = "ProviderSearchManager"
    }

    suspend fun searchProviders(song: IdentifiedSong): List<ProviderSong> = withContext(Dispatchers.IO) {
        val query = "${song.title} ${song.artist.orEmpty()}".trim()
        if (query.isBlank()) return@withContext emptyList()

        val hideExplicit = context.dataStore.getSuspend(HideExplicitKey, false)
        val enableJioSaavn = context.dataStore.getSuspend(EnableJioSaavnKey, false)

        val rawCandidates = mutableListOf<ProviderSong>()

        // 1. Search YouTube Music
        try {
            val ytResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
            ytResult?.items
                ?.filterIsInstance<SongItem>()
                ?.filterExplicit(hideExplicit)
                ?.forEach { item ->
                    rawCandidates.add(
                        ProviderSong(
                            id = item.id,
                            title = item.title,
                            artists = item.artists.map { it.name },
                            duration = item.duration ?: -1,
                            thumbnailUrl = item.thumbnail,
                            mediaMetadata = item.toMediaMetadata(),
                            provider = "YT"
                        )
                    )
                }
        } catch (e: Exception) {
            Log.w(TAG, "YouTube search failed for query: $query", e)
        }

        // 2. Search JioSaavn if enabled or if YT returned no results
        if (enableJioSaavn || rawCandidates.isEmpty()) {
            try {
                val jioResult = JioSaavnApi.searchSongs(query).getOrNull()
                jioResult?.forEach { item ->
                    rawCandidates.add(
                        ProviderSong(
                            id = item.id,
                            title = item.title,
                            artists = item.artists.map { it.name },
                            duration = item.duration ?: -1,
                            thumbnailUrl = item.thumbnail,
                            mediaMetadata = item.toMediaMetadata(),
                            provider = "JIOSAAVN"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "JioSaavn search failed for query: $query", e)
            }
        }

        // 3. Rank results with SongMatchEngine
        SongMatchEngine.rankCandidates(song, rawCandidates)
    }
}
