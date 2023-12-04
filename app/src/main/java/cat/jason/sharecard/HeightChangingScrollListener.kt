package cat.jason.sharecard

import android.graphics.Rect
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import cat.jason.composecardactivity.R
import kotlin.math.abs
import kotlin.math.min

class HeightChangingScrollListener(
    val snapHelper: SnapHelper,
    val layoutManager: LinearLayoutManager
) : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        val screenWidth = recyclerView.width
        val childCount = recyclerView.childCount
        val screenMidline = recyclerView.width / 2
        for (j in 0 until childCount) {
            val child = recyclerView.getChildAt(j)
            val rect = Rect()
            child.getGlobalVisibleRect(rect)
            val childLeft = rect.left
            val childRight = rect.right
            val childCenter = (childLeft + childRight) / 2

            val distanceToMidline: Float = abs(childCenter.toFloat() - screenMidline)
            val maxDistanceToMidline: Float =
                screenWidth / 2f - MAX_DISTANCE.dpToPx(recyclerView.resources).toFloat()
            val formatDistanceToMidline = min(distanceToMidline, maxDistanceToMidline)

            val maxHeight = MAX_HEIGHT.dpToPx(recyclerView.resources).toFloat()
            val minHeight = MIN_HEIGHT.dpToPx(recyclerView.resources).toFloat()

            val currentHeight =
                maxHeight - (maxHeight - minHeight) * formatDistanceToMidline / maxDistanceToMidline
            val radio = currentHeight / maxHeight

            child.scaleY = radio

            child?.let {
                val layoutParams = it.layoutParams as? ViewGroup.MarginLayoutParams
                layoutParams?.let { params ->
                    params.topMargin = 0 - ((maxHeight - currentHeight) / 2f).toInt()
                    it.layoutParams = params
                }
            }
        }
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            val itemView = snapHelper.findSnapView(layoutManager) ?: return
            val cardView = itemView.findViewById<ImageView>(R.id.card_view) ?: return

        } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
            // 遍历所有可见的 itemView
            for (i in 0 until layoutManager.childCount) {
                val itemView = layoutManager.getChildAt(i) ?: continue
                val cardView = itemView.findViewById<ImageView>(R.id.card_view) ?: continue
            }
        }
    }

}