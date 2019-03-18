package did.pinbraerts.tgchart

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
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

    var ordinate = Axis(0, 100, -Axis.DEFAULT_NUM)
    var abscissa = Axis()
    var minimap = Axis(num=0)

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

    var textPadding = 40.0f

    var mode = MotionMode.None
    var startX = 0f
    var startY = 0f

    fun init() {
        updateSizes()
        updateRects()

        ordinate.paint.apply {
            color = Color.GRAY
            style = Paint.Style.FILL
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18f, resources.displayMetrics)
        }

        abscissa.paint = ordinate.paint

        minimap.paint.apply {
            color = adjustAlpha(Color.GRAY, 0.1)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            style = Paint.Style.FILL
        }
        windowPaint.apply {
            color = adjustAlpha(Color.GRAY, 0.3)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            style = Paint.Style.FILL
        }
    }

    fun updateSizes() {
        val hSize = (width - paddingRight - paddingLeft).toFloat()
        val vSize = (height - paddingTop - paddingBottom).toFloat()

        minimap.rect.apply {
            bottom = vSize
            left = 0f
            right = hSize
            top = bottom - vSize / 9
        }

        abscissa.rect.apply {
            bottom = minimap.rect.top - textPadding
            left = 0f
            right = hSize
            top = bottom + abscissa.paint.fontMetrics.run { top - bottom }
        }

        ordinate.rect.apply {
            left = 0f
            top = 0.0f
            right = hSize
            bottom = abscissa.rect.top - textPadding
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
            drawLine(0.0f, (ordinate.rect.bottom - y), abscissa.rect.right, (ordinate.rect.bottom - y), ordinate.paint)
        }
        drawValues(abscissa, ordinate, ordinate.rect)
    }

    fun Canvas.drawAbscissa() {
        abscissa.range.forEach { v ->
            val x = abscissa.toWorld(v)
            val date = Date(v)
            val txt = dateFormat.format(date)
            val txtWidth: Float = abscissa.paint.measureText(txt)
            if(abscissa.rect.left.rangeTo(abscissa.rect.right - txtWidth).contains(x).not()) return@forEach
            drawText(
                txt,
                x + textPadding,
                abscissa.rect.bottom,
                abscissa.paint
            )
        }
    }

    fun Canvas.drawValues(axis: Axis, y: Axis, rect: RectF) {
        save()
//        clipRect(rect)
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

    var ordinateAnimator: ValueAnimator = ValueAnimator()

    fun updateDimensions(xColumn: Column) {
        minimap.start = xColumn.min
        minimap.end = xColumn.max

        val max = yColumns.filterIndexed { i, _ -> columnsToShow[i] }.map { it.max }.max() ?: 1000
//        ordinate.end = max

        if(ordinate.end != max) {
            ordinateAnimator.cancel()
            ordinateAnimator = ValueAnimator.ofFloat(ordinate.end.toFloat(), max.toFloat())
            ordinateAnimator.duration = 500
            ordinateAnimator.addUpdateListener {
                ordinate.end = (it.animatedValue as Float).toLong()
                invalidate()
            }
            ordinateAnimator.start()
        }

        if(abscissa.start == Axis.DEFAULT_MIN)
            abscissa.start = xColumn.min
        if(abscissa.end == Axis.DEFAULT_MAX)
            abscissa.end = xColumn.max
    }

    fun checkMinimapMode(rx: Float, ry: Float) {
        if (minimap.rect.top.rangeTo(minimap.rect.bottom).contains(ry)) {
            mode = when {
                rx < windowRect.left -> MotionMode.MinimapClick
                rx <= windowRect.left + thikness * 2 -> MotionMode.MinimapLeft
                rx < windowRect.right - thikness * 2 -> MotionMode.MinimapCenter
                rx <= windowRect.right -> MotionMode.MinimapRight
                else -> MotionMode.MinimapClick
            }
        }
    }

    fun Axis.addStart(dx: Long, last: Long = end - minimap.worldDiff(250f)) {
        start = clamp(
            start + dx,
            minimap.start,
            last
        )
    }

    fun Axis.addEnd(dx: Long, first: Long = start + minimap.worldDiff(250f)) {
        end = clamp(
            end + dx,
            first,
            minimap.end
        )
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        val currentX = event.rawX - loc[0] - paddingLeft
        val currentY = event.rawY - loc[1] - paddingTop
        val res = when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                checkMinimapMode(currentX, currentY)
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = minimap.worldDiff(currentX - startX)
                when (mode) {
                    MotionMode.MinimapCenter ->
                        abscissa.run {
                            val lastInt = interval
                            addStart(dx, minimap.end - lastInt)
                            addEnd(dx, minimap.start + lastInt)
                        }
                    MotionMode.MinimapLeft -> abscissa.addStart(dx)
                    MotionMode.MinimapRight -> abscissa.addEnd(dx)
                    MotionMode.MinimapClick -> {
                        val mdx = minimap.worldDiff(currentX - windowRect.centerX())
                        abscissa.run {
                            val lastInt = interval
                            addStart(mdx, minimap.end - interval)
                            addEnd(mdx, minimap.start + lastInt)
                        }
                        mode = MotionMode.MinimapCenter
                    }
                    else -> {
                        return true
                    }
                }
                updateRects()
                invalidate()
                true
            }
            MotionEvent.ACTION_UP -> {
                if(mode == MotionMode.MinimapClick) {
                    val mdx = minimap.worldDiff(currentX - windowRect.centerX())
                    abscissa.run {
                        val lastInt = interval
                        addStart(mdx, minimap.end - interval)
                        addEnd(mdx, minimap.start + lastInt)
                    }
                    performClick()
                }
                mode = MotionMode.None
                true
            }
            else -> super.onTouchEvent(event)
        }
        startX = currentX
        startY = currentY
        return res
    }
}