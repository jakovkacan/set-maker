package hr.jkacan.setmaker.services.soundcloud

import com.google.gson.annotations.SerializedName

data class SoundcloudTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("scope") val scope: String
)

data class SoundcloudTrackDto(
    val urn: String,
    val title: String,
    @SerializedName("stream_url") val streamUrl: String?,
    @SerializedName("permalink_url") val permalinkUrl: String?,
    @SerializedName("artwork_url") val artworkUrl: String?,
    @SerializedName("metadata_artist") val metadataArtist: String?,
    val user: SoundcloudUser
)

data class SoundcloudUser(
    val username: String,
)

data class SoundcloudStreamResponse(
    @SerializedName("preview_mp3_128_url") val previewUrl: String
)


