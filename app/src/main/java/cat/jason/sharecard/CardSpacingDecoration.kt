package cat.jason.sharecard

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class CardSpacingDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        recycleView: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = recycleView.getChildAdapterPosition(view)
        val itemCount = recycleView.adapter?.itemCount ?: 0

        if (position != RecyclerView.NO_POSITION) {
            outRect.left = if (position == 0) {
                spacing + MARGIN.dpToPx(view.resources)
            } else {
                spacing
            }

            outRect.right = if (position == itemCount - 1) {
                spacing + MARGIN.dpToPx(view.resources)
            } else {
                spacing
            }
        }
    }
}
