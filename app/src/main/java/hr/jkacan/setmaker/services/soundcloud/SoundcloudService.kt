package hr.jkacan.setmaker.services.soundcloud

import android.util.Base64
import android.util.Log
import hr.jkacan.setmaker.BuildConfig
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

    suspend fun getToken(): String {
        if (cachedToken != null) return cachedToken!!

        val authString =
            "${BuildConfig.SOUNDCLOUD_CLIENT_ID}:${BuildConfig.SOUNDCLOUD_CLIENT_SECRET}"
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
                response.mapIndexed { index, dto ->
                    async {
                        val preview = fetchPreviewUrl(dto.streamUrl + 's')
                        val artist = if (dto.metadataArtist?.isNotBlank()
                                ?: false
                        ) dto.metadataArtist else dto.user.username
                        Song(
                            id = index,
                            platformId = dto.urn,
                            title = dto.title,
                            artist = artist,
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
