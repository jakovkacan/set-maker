package hr.jkacan.setmaker

import android.app.Application
import hr.jkacan.setmaker.data.dao.SetRepository
import hr.jkacan.setmaker.data.dao.SongRepository
import hr.jkacan.setmaker.data.dao.getSetGraphRepository
import hr.jkacan.setmaker.data.dao.getSetRepository
import hr.jkacan.setmaker.data.dao.getSongRepository
import hr.jkacan.setmaker.services.local.LocalMusicService
import hr.jkacan.setmaker.services.soundcloud.SoundcloudService
import hr.jkacan.setmaker.services.spotify.SpotifyService
import hr.jkacan.setmaker.utils.AudioPreviewManager

class SetMakerApplication : Application() {
    // Repositories
    val songRepository by lazy { getSongRepository(this) }
    val setRepository by lazy { getSetRepository(this) }
    val setGraphRepository by lazy { getSetGraphRepository(this) }

    // Services
    val spotifyService by lazy { SpotifyService() }
    val soundcloudService by lazy { SoundcloudService() }
    val localMusicService by lazy { LocalMusicService(this) }

    // Managers
    val audioPreviewManager by lazy { AudioPreviewManager(this) }
}
