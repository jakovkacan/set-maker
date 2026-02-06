package hr.jkacan.setmaker.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import hr.jkacan.setmaker.models.song.Song
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import hr.jkacan.setmaker.databinding.SheetSongOptionsBinding
import hr.jkacan.setmaker.utils.showToast
import androidx.core.net.toUri
import hr.jkacan.setmaker.SetMakerApplication
import java.io.File

class SongOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: SheetSongOptionsBinding? = null
    private val binding get() = _binding!!

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
    ): View {
        _binding = SheetSongOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionAddToSet.setOnClickListener {
            // Add to set
            dismiss()
        }

        binding.optionExternalPlayer.setOnClickListener {
            song.songUrl?.let { url ->
                val intent = if (url.startsWith("/")) {
                    // Local file path
                    val file = File(url)
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "audio/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    val uri = url.toUri()
                    if (uri.scheme == "content") {
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "audio/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    } else {
                        // Network URL
                        Intent(Intent.ACTION_VIEW, uri)
                    }
                }
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    showToast("No app found to play this audio file", requireContext())
                }
            }
            dismiss()
        }

        binding.optionShare.setOnClickListener {
            dismiss()
        }

        binding.optionDelete.setOnClickListener {
            (requireActivity().application as SetMakerApplication).songRepository.delete(song.id!!)
            onSongDeleted?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
