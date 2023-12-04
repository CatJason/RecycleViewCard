package cat.jason.sharecard

import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.view.View
import java.lang.ref.WeakReference

class GyroscopeSensorEventListener : SensorEventListener {
    private var itemViewRef: WeakReference<View>? = null
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f
    private var ratioX: Float = 10f
    private var ratioY: Float = 5f
    private var ratioZ: Float = 6f
    private var maxRatio: Float = 0.01f
    private val alpha = 0.1f // 可以调整这个值以改变平滑程度

    fun bindItemView(itemView: View) {
        itemViewRef = WeakReference(itemView)
    }

    override fun onSensorChanged(event: SensorEvent) {
        // 应用低通滤波器
        var axisX = alpha * event.values[0] / ratioX + (1 - alpha) * lastX
        var axisY = alpha * event.values[1] / ratioY + (1 - alpha) * lastY

        // 限制 axisX 和 axisY 的值
        axisX = (-maxRatio).coerceAtLeast(axisX.coerceAtMost(maxRatio))
        axisY = (-maxRatio).coerceAtLeast(axisY.coerceAtMost(maxRatio))

        // 更新 lastX, lastY
        lastX = axisX
        lastY = axisY

        // 使用 RecyclerView 引用
        itemViewRef?.get()?.let {
            updateRecyclerViewViews(it, axisX, axisY, 0f)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // 如果需要，可以在这里处理传感器精度的变化
    }

    private fun updateRecyclerViewViews(itemView: View, axisX: Float, axisY: Float, axisZ: Float) {
        itemView.apply {
            // 设置旋转的轴点为视图的中心
            val rect = Rect()
            itemView.getGlobalVisibleRect(rect)

            pivotX = rect.width() / 2f
            pivotY = rect.height() / 2f

            val rotationDegreesX = Math.toDegrees(axisX.toDouble()).toFloat()
            val rotationDegreesY = Math.toDegrees(axisY.toDouble()).toFloat()

            // 应用旋转
            rotationX = rotationDegreesX
            rotationY = rotationDegreesY
        }
    }
}