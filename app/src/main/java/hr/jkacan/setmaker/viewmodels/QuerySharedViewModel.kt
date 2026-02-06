package hr.jkacan.setmaker.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.jkacan.setmaker.data.dao.SongRepository
import hr.jkacan.setmaker.data.state.SearchResultState
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.services.local.LocalMusicService
import hr.jkacan.setmaker.services.soundcloud.SoundcloudService
import hr.jkacan.setmaker.services.spotify.SpotifyService
import kotlinx.coroutines.launch

class QuerySharedViewModel(
    private val songRepository: SongRepository,
    private val spotifyService: SpotifyService,
    private val soundcloudService: SoundcloudService,
    private val localMusicService: LocalMusicService,
) : ViewModel() {
    private val _searchResults = MutableLiveData<SearchResultState>()
    val searchResults: LiveData<SearchResultState> = _searchResults

    fun search(query: String, provider: SongProvider) {
        viewModelScope.launch {
            // Set state to Loading before starting the search
            _searchResults.value = SearchResultState.Loading
            try {
                val results = when (provider) {
                    SongProvider.SPOTIFY -> spotifyService.query(query)
                    SongProvider.SOUNDCLOUD -> soundcloudService.query(query)
                    SongProvider.LOCAL -> localMusicService.query(query)
                }
                val savedSongsIds: List<String> =
                    songRepository.getAll().mapNotNull { it.platformId }
                _searchResults.value = SearchResultState.Success(results, savedSongsIds)
            } catch (e: Exception) {
                // Handle exceptions and set an error state
                _searchResults.value =
                    SearchResultState.Error("Failed to fetch results: ${e.message}")
            }
        }
    }

    fun loadAllLocalFiles() {
        search("", SongProvider.LOCAL)
    }
}
