package hr.jkacan.setmaker.services.spotify

import android.util.Base64
import hr.jkacan.setmaker.BuildConfig
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SpotifyService {
    private val api: SpotifyApiService = Retrofit.Builder()
        .baseUrl("https://api.spotify.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SpotifyApiService::class.java)

    private var cachedToken: String? = null

    private suspend fun getToken(): String {
        if (cachedToken != null) return cachedToken!!

        val authString = "${BuildConfig.SPOTIFY_CLIENT_ID}:${BuildConfig.SPOTIFY_CLIENT_SECRET}"
        // Use standard Android Base64 to avoid Firebase/Codec errors
        val encodedAuth = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)

        val response = api.getAccessToken("Basic $encodedAuth")
        cachedToken = response.accessToken
        return cachedToken!!
    }

    suspend fun query(queryString: String): List<Song> {
        return try {
            val token = getToken()
            val response = api.searchTracks("Bearer $token", queryString)

            coroutineScope {
                response.tracks.items.mapIndexed { index, dto ->
                    async {
                        val preview = fetchPreviewUrl(dto.id)
                        Song(
                            id = index + 1,
                            platformId = dto.id,
                            title = dto.name,
                            artist = dto.artists.joinToString(", ") { it.name },
                            coverUrl = dto.album.images.firstOrNull()?.url,
                            provider = SongProvider.SPOTIFY,
                            previewUrl = preview,
                            songUrl = dto.externalUrls["spotify"],
                            dateAdded = null
                        )
                    }
                }.awaitAll()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
