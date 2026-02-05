package hr.jkacan.setmaker.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.models.song.Song
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import hr.jkacan.setmaker.activities.MainActivity
import hr.jkacan.setmaker.utils.showToast
import androidx.core.net.toUri
import hr.jkacan.setmaker.services.spotify.fetchPreviewUrl

class SongOptionsBottomSheet : BottomSheetDialogFragment() {

    private lateinit var song: Song
    var onSongDeleted: (() -> Unit)? = null

    companion object {
        private const val ARG_SONG = "song"

        fun newInstance(song: Song): SongOptionsBottomSheet {
            val fragment = SongOptionsBottomSheet()
            val args = Bundle()
            args.putSerializable(ARG_SONG, song)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            song = it.getSerializable(ARG_SONG) as Song
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_song_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.option_add_to_set).setOnClickListener {
            // Add to set
            dismiss()
        }

        view.findViewById<TextView>(R.id.option_external_player).setOnClickListener {
            song.songUrl?.let { url ->
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    // Handle case where no app can handle the URL
                    showToast("No app found to open this link", requireContext())
                }
            }
            dismiss()
        }

        view.findViewById<TextView>(R.id.option_share).setOnClickListener {
            dismiss()
        }

        view.findViewById<TextView>(R.id.option_delete).setOnClickListener {
            (requireActivity() as MainActivity).songRepository.delete(song.id!!)
            onSongDeleted?.invoke()
            dismiss()
        }
    }
}
