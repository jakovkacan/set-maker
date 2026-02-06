package hr.jkacan.setmaker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import hr.jkacan.setmaker.SetMakerApplication
import hr.jkacan.setmaker.databinding.SheetSetOptionsBinding
import hr.jkacan.setmaker.models.set.SetItem

class SetOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: SheetSetOptionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var set: SetItem
    var onSetDeleted: (() -> Unit)? = null
    var onEditSet: (() -> Unit)? = null

    companion object {
        private const val ARG_SET = "set"

        fun newInstance(set: SetItem): SetOptionsBottomSheet {
            val fragment = SetOptionsBottomSheet()
            val args = Bundle()
            args.putSerializable(ARG_SET, set)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            set = it.getSerializable(ARG_SET) as SetItem
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetSetOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionEditSet.setOnClickListener {
            onEditSet?.invoke()
            dismiss()
        }

        binding.optionDelete.setOnClickListener {
            (requireActivity().application as SetMakerApplication).setRepository.delete(set.id!!)
            onSetDeleted?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
