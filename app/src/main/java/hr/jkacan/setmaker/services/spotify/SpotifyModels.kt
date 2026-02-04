package hr.jkacan.setmaker.services.spotify

import com.google.gson.annotations.SerializedName

data class SpotifyTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int
)

data class SpotifySearchResponse(val tracks: SpotifyTracks)
data class SpotifyTracks(val items: List<SpotifyTrackDto>)
data class SpotifyTrackDto(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtistDto>,
    val album: SpotifyAlbumDto,
    @SerializedName("preview_url") val previewUrl: String?,
    @SerializedName("external_urls") val externalUrls: Map<String, String>
)

data class SpotifyArtistDto(val name: String)
data class SpotifyAlbumDto(val images: List<SpotifyImageDto>)
data class SpotifyImageDto(val url: String)
