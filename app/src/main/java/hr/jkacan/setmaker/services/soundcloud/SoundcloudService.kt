package hr.jkacan.setmaker.services.soundcloud

import android.util.Base64
import android.util.Log
import hr.jkacan.setmaker.BuildConfig
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.services.spotify.SpotifySearchResponse
import hr.jkacan.setmaker.services.spotify.SpotifyTokenResponse
import hr.jkacan.setmaker.services.spotify.fetchPreviewUrl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import kotlin.text.get

interface SoundcloudApiService {
    @FormUrlEncoded
    @POST("https://secure.soundcloud.com/oauth/token")
    suspend fun getAccessToken(
        @Header("Authorization") auth: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ): SoundcloudTokenResponse

    @GET("tracks")
    suspend fun searchTracks(
        @Header("Authorization") auth: String,
        @Query("q") query: String,
        @Query("access") access: String = "",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): List<SoundcloudTrackDto>

    @GET
    suspend fun getStreamInfo(
        @Url url: String,
        @Header("Authorization") auth: String
    ): SoundcloudStreamResponse
}

class SoundcloudService {
    private val api: SoundcloudApiService = Retrofit.Builder()
        .baseUrl("https://api.soundcloud.com/")
        .client(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    val response = chain.proceed(request)
                    val body = response.body?.string()

                    Log.d("Soundcloud", "Request: ${request.url}")
                    Log.d("Soundcloud", "Response: $body")

                    response.newBuilder()
                        .body(body?.toResponseBody(response.body?.contentType()))
                        .build()
                }
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SoundcloudApiService::class.java)

    private var cachedToken: String? = null

    private suspend fun getToken(): String {
        if (cachedToken != null) return cachedToken!!

        val authString =
            "${BuildConfig.SOUNDCLOUD_CLIENT_ID}:${BuildConfig.SOUNDCLOUD_CLIENT_SECRET}"
        // Use standard Android Base64 to avoid Firebase/Codec errors
        val encodedAuth = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)

        val response = api.getAccessToken("Basic $encodedAuth")
        cachedToken = response.accessToken
        return cachedToken!!
    }

    suspend fun getAuthHeadersForStreaming(): Map<String, String> {
        val token = getToken()
        return mapOf("Authorization" to "Bearer $token")
    }

    suspend fun query(queryString: String): List<Song> {
        return try {
            val token = getToken()
            val response = api.searchTracks("Bearer $token", queryString)

            coroutineScope {
                response.mapIndexed { index, dto ->
                    async {
                        val preview = fetchPreviewUrl(dto.streamUrl + 's')
                        Song(
                            id = index,
                            platformId = dto.urn,
                            title = dto.title,
                            artist = dto.metadataArtist ?: "",
                            coverUrl = dto.artworkUrl,
                            provider = SongProvider.SOUNDCLOUD,
                            previewUrl = preview,
                            songUrl = dto.permalinkUrl,
                            dateAdded = null
                        )
                    }
                }.awaitAll()
            }

        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchPreviewUrl(streamUrl: String?): String? {
        if (streamUrl == null) return null
        return try {
            val token = getToken()
            val response = api.getStreamInfo(streamUrl, "Bearer $token")
            response.previewUrl
        } catch (e: Exception) {
            Log.e("Soundcloud", "Stream error: ${e.message}")
            null
        }
    }
}
