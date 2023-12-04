package cat.jason.sharecard

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


const val MAX_HEIGHT = 468
const val SPACING = 4
const val MARGIN = 30
const val MAX_DISTANCE = MARGIN + SPACING + SPACING
const val MIN_HEIGHT = 424

fun drawSvgPath(width: Int, height: Int, resources: Resources): Bitmap {
    // 创建一个空的Bitmap
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 创建绘制路径的画笔
    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    // 计算缩放比例
    val scaleX = width / 300f
    val scaleY = height / 468f


    val path = Path().apply {
        moveTo(0f, 9f)
        cubicTo(0f, 4.02944f, 4.02943f, 0f, 8.99999f, 0f)
        lineTo(291f, 0f)
        cubicTo(295.971f, 0f, 300f, 4.02944f, 300f, 9f)
        lineTo(300f, 371.048f)
        cubicTo(300f, 375.864f, 295.73f, 379.662f, 292.275f, 383.017f)
        cubicTo(289.965f, 385.26f, 288.524f, 388.434f, 288.524f, 391.953f)
        cubicTo(288.524f, 395.472f, 289.965f, 398.646f, 292.275f, 400.889f)
        cubicTo(295.73f, 404.244f, 300f, 408.042f, 300f, 412.858f)
        lineTo(300f, 516f)
        cubicTo(300f, 520.971f, 295.971f, 525f, 291f, 525f)
        lineTo(9f, 525f)
        cubicTo(4.02944f, 525f, 0f, 520.971f, 0f, 516f)
        lineTo(0f, 413.258f)
        cubicTo(0f, 408.287f, 4.54687f, 404.406f, 8.15129f, 400.983f)
        cubicTo(10.5182f, 398.736f, 11.9999f, 395.522f, 11.9999f, 391.953f)
        cubicTo(11.9999f, 388.384f, 10.5182f, 385.17f, 8.15129f, 382.923f)
        cubicTo(4.54688f, 379.5f, 0f, 375.619f, 0f, 370.649f)
        lineTo(0f, 9f)
        close()
    }

    // 创建矩阵并应用缩放变换
    val matrix = Matrix()
    matrix.postScale(scaleX, scaleY, 0f, 0f)
    path.transform(matrix)

    // 创建渐变填充
    val gradient = LinearGradient(
        150f, 0f, 150f, 544.016f,
        intArrayOf(
            Color.parseColor("#3B3147"),
            Color.parseColor("#FEBBFF"),
            Color.parseColor("#693FD5")
        ),
        floatArrayOf(0f, 0.0001f, 1f),
        Shader.TileMode.CLAMP
    )

    paint.shader = gradient
    canvas.drawPath(path, paint)

    val topInPixels = 392.dpToPx(resources)
    val lineWidthInPixels = 226.dpToPx(resources)
    val lineHeightInPixels = 1.dpToPx(resources)

    val startX = (width - lineWidthInPixels) / 2
    val endX = startX + lineWidthInPixels
    val startY = topInPixels
    val endY = startY + lineHeightInPixels

    paint.shader = null
    paint.color = Color.parseColor("#66FFFFFF")
    paint.strokeWidth = lineHeightInPixels.toFloat()
    paint.style = Paint.Style.STROKE

    val dashLength = 5.dpToPx(resources)
    val gapLength = 5.dpToPx(resources)
    paint.pathEffect = DashPathEffect(floatArrayOf(dashLength.toFloat(), gapLength.toFloat()), 0f)

    canvas.drawLine(
        startX.toFloat(),
        startY.toFloat(),
        endX.toFloat(),
        endY.toFloat(),
        paint
    )
    return bitmap
}

fun getBitmapFromGlide(
    lifecycleOwner: LifecycleOwner,
    context: Context,
    url: String
): Deferred<Bitmap?> {
    /**
     * 由于 Glide 的加载操作在后台执行，所以这里的 suspendCancellableCoroutine 并不会阻塞主线程。它只是在等待 Glide 回调被触发时暂停协程，而不是暂停整个主线程。
     */
    return lifecycleOwner.lifecycleScope.async(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val requestOptions: RequestOptions = RequestOptions()
                .transform(RoundedCorners(44.dpToPx(context.resources)))
            Glide.with(context)
                .asBitmap()
                .load(url)
                .apply(requestOptions)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        continuation.resume(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // 这里可以处理占位图逻辑
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        continuation.resume(null) // 在加载失败时返回null
                    }
                })
        }
    }
}

fun Int.dpToPx(resources: Resources): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        resources.displayMetrics
    ).toInt()
}