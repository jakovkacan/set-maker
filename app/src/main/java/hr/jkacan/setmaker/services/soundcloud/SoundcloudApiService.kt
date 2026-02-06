package hr.jkacan.setmaker.services.soundcloud

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

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