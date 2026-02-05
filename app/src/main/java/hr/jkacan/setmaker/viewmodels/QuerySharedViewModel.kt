package hr.jkacan.setmaker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.jkacan.setmaker.activities.MainActivity
import hr.jkacan.setmaker.data.dao.SongRepository
import hr.jkacan.setmaker.data.dao.getSongRepository
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.services.soundcloud.SoundcloudService
import hr.jkacan.setmaker.services.spotify.SpotifyService
import kotlinx.coroutines.launch

// Sealed class to represent the state of the search results
sealed class SearchResultState {
    object Loading : SearchResultState()
    data class Success(val songs: List<Song>, val savedSongsIds: List<String>? = null) :
        SearchResultState()

    data class Error(val message: String) : SearchResultState()
}

class QuerySharedViewModel(private val songRepository: SongRepository) : ViewModel() {

    // Instantiate your services here
    private val spotifyService = SpotifyService()
    private val soundcloudService = SoundcloudService()

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
                    SongProvider.LOCAL -> {
                        // TODO: Implement local search logic
                        emptyList()
                    }
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
}
