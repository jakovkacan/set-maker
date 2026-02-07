package hr.jkacan.setmaker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.adapters.SongAdapter.SongDiffCallback
import hr.jkacan.setmaker.databinding.ItemSetBinding
import hr.jkacan.setmaker.models.set.SetItem
import hr.jkacan.setmaker.models.song.Song

class SetAdapter(
    private var sets: List<SetItem>,
    private val onItemClick: (SetItem) -> Unit,
    private val onItemLongPress: (SetItem) -> Unit,
) : RecyclerView.Adapter<SetAdapter.SetViewHolder>() {

    inner class SetViewHolder(private val binding: ItemSetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(sets[position])
                }
            }

            binding.root.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongPress(sets[position])
                }
                true
            }
        }

        fun bind(set: SetItem) {
            binding.setName.text = set.name

            // Load cover image
            if (set.coverPath.isNullOrBlank()) {
                binding.setCoverImage.setImageResource(R.drawable.placeholder_set_cover)
            } else {
                binding.setCoverImage.load(set.coverPath) {
                    crossfade(true)
                    placeholder(R.drawable.placeholder_set_cover)
                    error(R.drawable.placeholder_set_cover)
                    transformations(
                        RoundedCornersTransformation(
                            16f,
                            16f,
                            0f,
                            0f
                        )
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetViewHolder {
        val binding = ItemSetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SetViewHolder, position: Int) {
        holder.bind(sets[position])
    }

    override fun getItemCount(): Int = sets.size

    fun updateSets(newSets: List<SetItem>) {
        val diffCallback = SetDiffCallback(sets, newSets)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        sets = newSets
        diffResult.dispatchUpdatesTo(this)
    }

    private class SetDiffCallback(
        private val oldList: List<SetItem>,
        private val newList: List<SetItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
