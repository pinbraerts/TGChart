package did.pinbraerts.tgchart

import android.graphics.*
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

class Ordinate(
    min: Long = AxisInfo.DEFAULT_MIN,
    max: Long = AxisInfo.DEFAULT_MAX,
    num: Int = AxisInfo.DEFAULT_NUM,
    top: Float = 0.0f,
    bottom: Float = 0.0f,
    var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
): AxisAdapter(AxisInfo(min, max, num), top, bottom) {
    override fun draw(canvas: Canvas, chart: Chart) {
        canvas.apply {
            axis.forEach { v ->
                val y = toWorld(v)
                drawText(v.toString(), 0.0f + chart.textPadding, bottom - y - chart.textPadding, paint)
                drawLine(0.0f, (bottom - y), chart.size, (bottom - y), paint)
            }
        }
    }
}

class Abscissa(
    min: Long = AxisInfo.DEFAULT_MIN,
    max: Long = AxisInfo.DEFAULT_MAX,
    num: Int = AxisInfo.DEFAULT_NUM,
    top: Float = 0.0f,
    bottom: Float = 0.0f,
    var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG),
    val dateFormat: SimpleDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
): AxisAdapter(AxisInfo(min, max, num), top, bottom) {

    init {
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun draw(canvas: Canvas, chart: Chart) {
        axis.forEach { v ->
            val x = toWorld(v)
            val date = Date(v)
            val txt = dateFormat.format(date)
            val txtHalf: Float = paint.measureText(txt) / 2.0f
            if(x < txtHalf || x > chart.size - txtHalf) return@forEach;
            canvas.drawText(
                txt,
                x + chart.textPadding - txtHalf,
                bottom,
                paint
            )
        }
    }
}

class Minimap(
    min: Long = AxisInfo.DEFAULT_MIN,
    max: Long = AxisInfo.DEFAULT_MAX,
    top: Float = 0.0f,
    bottom: Float = 0.0f,
    var ordinate: AxisInfo = AxisInfo(),
    var thikness: Float = 10.0f,
    var fillPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG),
    var windowPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
): AxisAdapter(AxisInfo(min, max), top, bottom) {
    fun Canvas.clipOut(r: RectF) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            clipOutRect(r.left, r.top, r.right - 0.5f, r.bottom)
        else
            clipRect(r.left, r.top, r.right - 0.5f, r.bottom, Region.Op.DIFFERENCE)
    }

    override fun draw(canvas: Canvas, chart: Chart) {
        canvas.apply {
            val start = chart.minimapStart
            val end = chart.minimapEnd

            val clip = RectF(start + thikness, top + thikness, end - thikness, bottom - thikness)
            val window = RectF(start - thikness, top, end + thikness, bottom)
            val filled = RectF(0f, top, chart.size, bottom)

            save()
            clipOut(clip)
            drawRect(window, windowPaint)
            restore()
            save()

//            chart.yColumns.filterIndexed { i, _ -> chart.columnsToShow[i] }.forEachIndexed { i, yv ->
//                val arr = FloatArray(yv.data.size * 2) {
//                    if(it % 2 == 0) chart.abscissa.toWorld(chart.xColumn.data[i / 2])
//                    else (yv.data[i / 2]) / (ordinate.max - ordinate.min) * (bottom - top)
//                }
//                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
//                paint.color = yv.color
//                drawLines(arr, paint)
//            }

            clipOut(window)
            drawRect(filled, fillPaint)
            restore()
        }
    }
}

//fun adjustAlpha(base: Int, alpha: Int) = Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base))
//fun adjustAlpha(base: Int, alpha: Float) = Color.argb((alpha * 256).toInt(), Color.red(base), Color.green(base), Color.blue(base))
//fun adjustAlpha(base: Int, alpha: Double) = Color.argb((alpha * 256).toInt(), Color.red(base), Color.green(base), Color.blue(base))
fun <T> adjustAlpha(base: Int, alpha: T) = when(alpha) {
    is Int -> Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base))
    is Number -> Color.argb((alpha.toFloat() * 256).toInt(), Color.red(base), Color.green(base), Color.blue(base))
    is Any -> throw IllegalArgumentException("Type (${alpha.javaClass}) is not supported")
    else -> throw IllegalArgumentException("Type is not supported")
}
