package hr.jkacan.setmaker.services.spotify

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

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