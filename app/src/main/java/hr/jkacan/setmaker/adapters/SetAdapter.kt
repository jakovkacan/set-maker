package hr.jkacan.setmaker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.models.Set

class SetAdapter(
    private val sets: List<Set>,
    private val onItemClick: (Set) -> Unit
) : RecyclerView.Adapter<SetAdapter.SetViewHolder>() {

    inner class SetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverImage: ImageView = itemView.findViewById(R.id.set_cover_image)
        val setName: TextView = itemView.findViewById(R.id.set_name)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(sets[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_set, parent, false)
        return SetViewHolder(view)
    }

    override fun onBindViewHolder(holder: SetViewHolder, position: Int) {
        val set = sets[position]
        holder.setName.text = set.name

        // Load cover image
        if (set.coverUrl.isNullOrBlank()) {
            holder.coverImage.setImageResource(R.drawable.placeholder_set_cover)
        } else {
            holder.coverImage.load(set.coverUrl) {
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

    override fun getItemCount(): Int = sets.size
}
