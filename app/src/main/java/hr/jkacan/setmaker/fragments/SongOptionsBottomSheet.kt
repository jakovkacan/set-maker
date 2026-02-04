package hr.jkacan.setmaker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.models.song.Song
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SongOptionsBottomSheet : BottomSheetDialogFragment() {

    private lateinit var song: Song

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

        view.findViewById<TextView>(R.id.option_pin).setOnClickListener {
            // Toggle pin
            dismiss()
        }

        view.findViewById<TextView>(R.id.option_share).setOnClickListener {
            // Share song
            dismiss()
        }

        view.findViewById<TextView>(R.id.option_delete).setOnClickListener {
            // Delete song
            dismiss()
        }
    }
}
