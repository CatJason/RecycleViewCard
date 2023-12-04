package cat.jason.sharecard

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import cat.jason.composecardactivity.R
import com.bumptech.glide.Glide
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorDodgeBlendFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CardAdapter(
    private val backgroundBitmap: Bitmap,
    private val imageUrlList: List<String>
) :
    RecyclerView.Adapter<CardViewHolder>() {

    private fun bind(imageUrl: String, holder: CardViewHolder) {
        with(holder) {
            val lifecycleOwner = (itemView.context as? LifecycleOwner) ?: return
            lifecycleOwner.lifecycleScope.launch {
                val baseBitmap = withContext(Dispatchers.IO) {
                    getBitmapFromGlide(lifecycleOwner, itemView.context, imageUrl).await()
                }?: return@launch

                cardView.setImageBitmap(baseBitmap)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.card_item_layout, parent, false)
        return CardViewHolder(view)
    }


    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val bitmap = backgroundBitmap
        holder.imageView.setImageBitmap(bitmap)
        val imageUr = imageUrlList[position]
        bind(imageUr, holder)
    }

    override fun getItemCount(): Int {
        return imageUrlList.size
    }
}