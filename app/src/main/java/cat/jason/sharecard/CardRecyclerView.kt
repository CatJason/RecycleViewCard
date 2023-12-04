package cat.jason.sharecard

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

class CardRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {

    init {
        val imageUrlList = mutableListOf<String>()
        val width = getScreenWidth() - (MAX_DISTANCE * 2).dpToPx(resources)
        val height = MAX_HEIGHT.dpToPx(resources)
        val backgroundBitmap = drawSvgPath(
            width,
            height,
            resources
        )

        imageUrlList.addAll(
            arrayListOf(
                "https://pics0.baidu.com/feed/a5c27d1ed21b0ef499a52471628e5bd781cb3e23.jpeg?token=86089c90f0cc99dd0c4f27a8cb2256ab",
                "https://pics0.baidu.com/feed/8694a4c27d1ed21b21cbc0021224d7c950da3faa.jpeg?token=81c8f546fac606d0f59650d7fb04253c",
                "https://pics0.baidu.com/feed/a5c27d1ed21b0ef499a52471628e5bd781cb3e23.jpeg?token=86089c90f0cc99dd0c4f27a8cb2256ab",
                "https://pics0.baidu.com/feed/8694a4c27d1ed21b21cbc0021224d7c950da3faa.jpeg?token=81c8f546fac606d0f59650d7fb04253c",
                "https://pics0.baidu.com/feed/a5c27d1ed21b0ef499a52471628e5bd781cb3e23.jpeg?token=86089c90f0cc99dd0c4f27a8cb2256ab",
                "https://pics0.baidu.com/feed/8694a4c27d1ed21b21cbc0021224d7c950da3faa.jpeg?token=81c8f546fac606d0f59650d7fb04253c",
                "https://pics0.baidu.com/feed/a5c27d1ed21b0ef499a52471628e5bd781cb3e23.jpeg?token=86089c90f0cc99dd0c4f27a8cb2256ab",
                "https://pics0.baidu.com/feed/8694a4c27d1ed21b21cbc0021224d7c950da3faa.jpeg?token=81c8f546fac606d0f59650d7fb04253c",
                "https://pics0.baidu.com/feed/a5c27d1ed21b0ef499a52471628e5bd781cb3e23.jpeg?token=86089c90f0cc99dd0c4f27a8cb2256ab",
                "https://pics0.baidu.com/feed/8694a4c27d1ed21b21cbc0021224d7c950da3faa.jpeg?token=81c8f546fac606d0f59650d7fb04253c"
            )
        )

        val cardLayoutManager = CardStackLayoutManager(context, LinearLayoutManager.HORIZONTAL, false).apply {
            initialPrefetchItemCount = 3
        }
        val cardAdapter = CardAdapter(backgroundBitmap, imageUrlList)
        val cardDecoration = CardSpacingDecoration(SPACING.dpToPx(resources))
        val snapHelper = PagerSnapHelper()

        this.layoutManager = cardLayoutManager
        adapter = cardAdapter
        addItemDecoration(cardDecoration)
        snapHelper.attachToRecyclerView(this)
        val scrollListener = HeightChangingScrollListener(
            snapHelper,
            cardLayoutManager
        )
        addOnScrollListener(scrollListener)
        scrollListener.onScrollStateChanged(this, SCROLL_STATE_IDLE)

    }

    companion object {
        private const val TAG = "CustomRecyclerView"
    }

    private fun getScreenWidth(): Int {
        val displayMetrics = Resources.getSystem().displayMetrics
        return displayMetrics.widthPixels
    }
}