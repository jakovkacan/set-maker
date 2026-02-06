package hr.jkacan.setmaker.data.state

import hr.jkacan.setmaker.models.song.Song

sealed class SearchResultState {
    object Loading : SearchResultState()
    data class Success(val songs: List<Song>, val savedSongsIds: List<String>? = null) :
        SearchResultState()

    data class Error(val message: String) : SearchResultState()
}