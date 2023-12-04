package cat.jason.sharecard

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import cat.jason.composecardactivity.R

class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val imageView: ImageView =
        itemView.findViewById(R.id.image_view)
    val cardView: ImageView =
        itemView.findViewById(R.id.card_view)
}
