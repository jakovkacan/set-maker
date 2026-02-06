package hr.jkacan.setmaker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.databinding.ItemSetBinding
import hr.jkacan.setmaker.models.set.SetItem

class SetAdapter(
    private val sets: List<SetItem>,
    private val onItemClick: (SetItem) -> Unit
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
        }

        fun bind(set: SetItem) {
            binding.setName.text = set.name

            // Load cover image
            if (set.coverUrl.isNullOrBlank()) {
                binding.setCoverImage.setImageResource(R.drawable.placeholder_set_cover)
            } else {
                binding.setCoverImage.load(set.coverUrl) {
                    crossfade(true)
                    placeholder(R.drawable.placeholder_set_cover)
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
}
