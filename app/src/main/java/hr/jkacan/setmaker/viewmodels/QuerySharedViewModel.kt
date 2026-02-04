package hr.jkacan.setmaker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.services.spotify.SpotifyService
import kotlinx.coroutines.launch

// Sealed class to represent the state of the search results
sealed class SearchResultState {
    object Loading : SearchResultState()
    data class Success(val songs: List<Song>) : SearchResultState()
    data class Error(val message: String) : SearchResultState()
}

class QuerySharedViewModel : ViewModel() {

    // Instantiate your services here
    private val spotifyService = SpotifyService()
    // private val soundcloudService = SoundCloudService() // etc.

    private val _searchResults = MutableLiveData<SearchResultState>()
    val searchResults: LiveData<SearchResultState> = _searchResults

    fun search(query: String, provider: SongProvider) {
        viewModelScope.launch {
            // Set state to Loading before starting the search
            _searchResults.value = SearchResultState.Loading
            try {
                val results = when (provider) {
                    SongProvider.SPOTIFY -> spotifyService.query(query)
                    SongProvider.SOUNDCLOUD -> {
                        // TODO: Implement SoundCloudService.query(query)
                        emptyList()
                    }
                    SongProvider.LOCAL -> {
                        // TODO: Implement local search logic
                        emptyList()
                    }
                }
                _searchResults.value = SearchResultState.Success(results)
            } catch (e: Exception) {
                // Handle exceptions and set an error state
                _searchResults.value = SearchResultState.Error("Failed to fetch results: ${e.message}")
            }
        }
    }
}
