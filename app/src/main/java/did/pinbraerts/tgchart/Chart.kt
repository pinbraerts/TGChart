package did.pinbraerts.tgchart

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.text.SimpleDateFormat
import java.util.*

class Chart : View {
    constructor(context: Context?): super(context)
    constructor(context: Context?, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int): super(context, attrs, defStyle)

    var columnsToShow = BitSet()
    var yColumns: Array<ColumnCache> = arrayOf()

    var ordinate = Axis()
    var abscissa = Axis()
    var minimap = Axis()

    val dateFormat: SimpleDateFormat = SimpleDateFormat("MMM d", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    var windowRect = RectF()
    var windowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    var clipRect = RectF()

    val dataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
    }

    var thikness: Float = 10.0f

    var textPadding = 16.0f
    var minimapPadding = 80.0f
    var size = 1000.0f

    var mode = MotionMode.None
    var startX = 0f
    var startY = 0f

    fun init() {
        minimap.num = 0
        ordinate.num *= -1

        updateSizes()
        updateRects()

        ordinate.paint.apply {
            color = Color.GRAY
            style = Paint.Style.FILL
            textSize = 40.0f
        }

        abscissa.paint = ordinate.paint

        minimap.paint.apply {
            color = adjustAlpha(Color.LTGRAY, 0.5)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            style = Paint.Style.FILL
        }
        windowPaint.apply {
            color = Color.LTGRAY
            style = Paint.Style.FILL
        }
    }

    fun updateSizes() {
        size = (Math.min(width - paddingRight - paddingLeft, height - paddingTop - paddingBottom)).toFloat()

        minimap.rect.apply {
            bottom = size
            left = 0f
            right = size
            top = bottom - size / 10
        }

        abscissa.rect.apply {
            bottom = minimap.rect.top - minimapPadding
            left = 0f
            right = size
            top = bottom + abscissa.paint.fontMetrics.top
        }

        ordinate.rect.apply {
            left = 0f
            top = 0.0f
            right = size
            bottom = abscissa.rect.bottom + textPadding - minimapPadding
        }
        ordinate.vertical = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = Math.min(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        init()
    }

    fun updateRects() {
        val start = minimap.toWorld(abscissa.start)
        val end = minimap.toWorld(abscissa.end)

        minimap.rect.apply {
            clipRect = RectF(start + thikness, top + thikness, end - thikness, bottom - thikness)
            windowRect = RectF(start - thikness, top, end + thikness, bottom)
        }
    }

    fun Canvas.drawOrdinate() {
        ordinate.range.forEach { v ->
            val y = ordinate.toWorld(v)
            drawText(v.prettyToString(), 0.0f + textPadding, ordinate.rect.bottom - y - textPadding, ordinate.paint)
            drawLine(0.0f, (ordinate.rect.bottom - y), size, (ordinate.rect.bottom - y), ordinate.paint)
        }
        drawValues(abscissa, ordinate, ordinate.rect)
    }

    fun Canvas.drawAbscissa() {
        abscissa.range.forEach { v ->
            val x = abscissa.toWorld(v)
            val date = Date(v)
            val txt = dateFormat.format(date)
            val txtHalf: Float = abscissa.paint.measureText(txt) / 2.0f
            if(x < txtHalf || x > size - txtHalf) return@forEach
            drawText(
                txt,
                x + textPadding - txtHalf,
                abscissa.rect.bottom,
                abscissa.paint
            )
        }
    }

    fun Canvas.drawValues(axis: Axis, y: Axis, rect: RectF) {
        save()
        clipRect(rect)
        translate(rect.left, rect.top)
        yColumns.filterIndexed { i, _ -> columnsToShow[i] }.forEach { yv ->
            dataPaint.color = yv.color
            drawLines(yv.lines.mapIndexed { i, fl ->
                if(i % 2 == 0) axis.toWorld(fl, rect.width())
                else rect.height() - y.toWorld(fl, rect.height())
            }.toFloatArray(), dataPaint)
        }
        restore()
    }

    fun Canvas.drawMinimap() {
        save()
        clipOut(clipRect)
        drawRect(windowRect, windowPaint)
        restore()

        drawValues(minimap, ordinate, minimap.rect)

        save()
        clipOut(windowRect)
        drawRect(minimap.rect, minimap.paint)
        restore()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.translate(paddingLeft.toFloat(), paddingTop.toFloat())
        c.drawOrdinate()
        c.drawAbscissa()
        c.drawMinimap()
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    enum class MotionMode {
        MinimapRight,
        MinimapLeft,
        MinimapCenter,
        MinimapClick,
        None
    }

    fun updatePage(page: Page) {
        columnsToShow = BitSet(page.size - 1)
        columnsToShow.set(0, page.size - 1)
        val xColumn = page["x"]!!
        yColumns = page.filter { it.key != "x" }.values.map { ColumnCache(xColumn, it) }.toTypedArray()

        updateDimensions(xColumn)
        updateSizes()
        updateRects()
        invalidate()
    }

    fun updateDimensions(xColumn: Column) {
        minimap.start = xColumn.min
        minimap.end = xColumn.max

        ordinate.start = 0
        ordinate.end = yColumns.filterIndexed { i, _ -> columnsToShow[i] }.map { it.max }.max() ?: 100

        if(abscissa.start == Axis.DEFAULT_MIN)
            abscissa.start = xColumn.min
        if(abscissa.end == Axis.DEFAULT_MAX)
            abscissa.end = xColumn.max
    }

    fun checkMinimapMode(rx: Float, ry: Float) {
        if (minimap.rect.top.rangeTo(minimap.rect.bottom).contains(ry)) {
            mode = when {
                rx >= windowRect.left && rx <= windowRect.left + thikness -> MotionMode.MinimapLeft
                rx < windowRect.right - thikness -> MotionMode.MinimapCenter
                rx <= windowRect.right -> MotionMode.MinimapRight
                else -> MotionMode.MinimapClick
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val currentX = event.rawX - x - paddingLeft
        val currentY = event.rawY - y - paddingTop
        val res = when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                checkMinimapMode(currentX, currentY)
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = minimap.worldDiff(currentX - startX)
                when (mode) {
                    MotionMode.MinimapCenter -> {
                        abscissa.start = clamp(
                            abscissa.start + dx,
                            minimap.start,
                            minimap.end - abscissa.interval
                        )
                        abscissa.end = clamp(
                            abscissa.end + dx,
                            minimap.start + abscissa.interval,
                            minimap.end
                        )
                    }
                    MotionMode.MinimapLeft -> abscissa.start = clamp(
                        abscissa.start + dx,
                        minimap.start,
                        minimap.fromWorld(windowRect.right - 250)
                    )
                    MotionMode.MinimapRight -> abscissa.end = clamp(
                        abscissa.end + dx,
                        minimap.fromWorld(windowRect.left + 250),
                        minimap.end
                    )
                    else -> {
                        return true
                    }
                }
                updateRects()
                invalidate()
                true
            }
            MotionEvent.ACTION_UP -> {
                mode = MotionMode.None
                performClick()
            }
            else -> super.onTouchEvent(event)
        }
        startX = currentX
        startY = currentY
        return res
    }
}