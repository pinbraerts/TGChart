package did.pinbraerts.tgchart

import android.graphics.Canvas

abstract class AxisAdapter(
    val axis: AxisInfo,
    var top: Float = 0.0f,
    var bottom: Float = 0.0f
) {
    var convert = 1.0f

    abstract fun draw(canvas: Canvas, chart: Chart)

    fun setSize(size: Float = bottom - top) {
        convert = size / (axis.max - axis.min + axis.tick)
    }
    fun fromWorld(x: Float): Long = (x / convert + axis.min).toLong()
    fun toWorld(x: Long): Float = (x - axis.min) * convert
}