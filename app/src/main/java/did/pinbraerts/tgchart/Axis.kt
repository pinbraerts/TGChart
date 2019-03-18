package did.pinbraerts.tgchart

import android.graphics.Paint
import android.graphics.RectF

data class Axis(
    var start: Long = DEFAULT_MIN,
    var end: Long = DEFAULT_MAX,
    var num: Int = DEFAULT_NUM,
    var rect: RectF = RectF(),
    var vertical: Boolean = true,
    var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
) {
    constructor(
        start: Long = DEFAULT_MIN,
        end: Long = DEFAULT_MAX,
        num: Int = DEFAULT_NUM,
        left: Float = 0f,
        top: Float = 0f,
        right: Float = 0f,
        bottom: Float = 0f,
        vert: Boolean = true
    ): this(start, end, num, RectF(left, top, right, bottom), vert)

    val step
        get() = getStep(start, end, num)
    val interval
        get() = end - start + step
    val range
        get() = start..end step step

    companion object {
        fun getStep(start: Long, end: Long, num: Int) =
            if(num != 0) ((end - start).toFloat() / (num - 1)).toLong()
            else 0L

        const val DEFAULT_MIN = 0L
        const val DEFAULT_MAX = 100L
        const val DEFAULT_NUM = 6
    }

    fun fromWorld(x: Float, size: Float = (if(vertical) rect.width() else rect.height())): Long
            = (x * interval / size + start).toLong()
    fun toWorld(x: Long, size: Float = (if(vertical) rect.width() else rect.height())): Float
            = (x - start) * size / interval
    fun worldDiff(diff: Float, size: Float = (if(vertical) rect.width() else rect.height())): Long
            = (diff * interval / size).toLong()
}
