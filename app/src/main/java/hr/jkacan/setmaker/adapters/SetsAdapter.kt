package hr.jkacan.setmaker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.models.SetItem

class SetsAdapter(
    private val sets: List<SetItem>,
    private val onItemClick: (SetItem) -> Unit
) : RecyclerView.Adapter<SetsAdapter.SetViewHolder>() {

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
        // Load cover image using your preferred image loading library (Glide, Picasso, etc.)
        // Example: Glide.with(holder.itemView.context).load(set.coverUrl).into(holder.coverImage)
    }

    override fun getItemCount(): Int = sets.size
}
