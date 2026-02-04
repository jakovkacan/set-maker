package hr.jkacan.setmaker.services.spotify

import android.util.Base64
import hr.jkacan.setmaker.BuildConfig
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface SpotifyApiService {
    @FormUrlEncoded
    @POST("https://accounts.spotify.com/api/token")
    suspend fun getAccessToken(
        @Header("Authorization") auth: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ): SpotifyTokenResponse

    @GET("v1/search")
    suspend fun searchTracks(
        @Header("Authorization") auth: String,
        @Query("q") query: String,
        @Query("type") type: String = "track"
    ): SpotifySearchResponse
}

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

            var counter = 0;

            response.tracks.items.map { dto ->
                Song(
                    id = ++counter,
                    title = dto.name,
                    // Join artist names as requested
                    artist = dto.artists.joinToString(", ") { it.name },
                    // album[images[0][url]] logic
                    coverUrl = dto.album.images.firstOrNull()?.url,
                    provider = SongProvider.SPOTIFY,
                    previewUrl = dto.previewUrl,
                    // external_urls["spotify"]
                    songUrl = dto.externalUrls["spotify"]
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
