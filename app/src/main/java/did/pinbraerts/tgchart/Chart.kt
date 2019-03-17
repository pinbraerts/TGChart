package did.pinbraerts.tgchart

import android.content.Context
import android.graphics.*
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.*

class Chart : View {
    constructor(context: Context?): super(context)
    constructor(context: Context?, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int): super(context, attrs, defStyle)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int):
            super(context, attrs, defStyleAttr, defStyleRes)

    var columnsToShow = BitSet()
    var xColumn: Column = Column()
    var yColumns: Array<Column> = arrayOf()

    var ordinate = Ordinate()
    var abscissa = Abscissa()
    var minimap = Minimap()

    var textPadding = 16.0f
    var minimapPadding = 80.0f
    var size = 1000.0f

    var yMax = 0.0f
    var yMin = 0.0f

    var mode = MotionMode.None

    val minimapStart
        get() = minimap.toWorld(abscissa.axis.min)
    val minimapEnd
        get() = minimap.toWorld(abscissa.axis.max)

    fun init() {
        updateSizes()
        ordinate.top = 0.0f

        ordinate.paint.apply {
            color = Color.GRAY
            style = Paint.Style.FILL
            textSize = 40.0f
        }

        abscissa.paint = ordinate.paint

        minimap.fillPaint.apply {
            color = adjustAlpha(Color.LTGRAY, 0.5)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            style = Paint.Style.FILL
        }
        minimap.windowPaint.apply {
            color = Color.LTGRAY
            style = Paint.Style.FILL
        }
    }

    fun updateSizes() {
        size = (Math.min(width - paddingRight - paddingLeft, height - paddingTop - paddingBottom)).toFloat()
        abscissa.setSize(size)
        ordinate.setSize()
        minimap.setSize(size)

        minimap.bottom = size
        minimap.top = minimap.bottom - 160

        abscissa.bottom = minimap.top - minimapPadding
        abscissa.top = abscissa.bottom + abscissa.paint.fontMetrics.top

        ordinate.bottom = abscissa.bottom + textPadding - minimapPadding
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

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.translate(paddingLeft.toFloat(), paddingTop.toFloat())
        ordinate.draw(c, this)
        abscissa.draw(c, this)
        minimap.draw(c, this)
    }

    override fun performClick(): Boolean {
        val x = 3
        return super.performClick()
    }

    enum class MotionMode {
        MinimapRight,
        MinimapLeft,
        MinimapCenter,
        None
    }

    fun updatePage(page: Page) {
        columnsToShow = BitSet(page.size - 1)
        xColumn = page.get("x")!!
        yColumns = page.filter { it.key != "x" }.values.toTypedArray()
        minimap.axis.max = xColumn.max
        minimap.axis.min = xColumn.min
        minimap.ordinate.max = yColumns.map { it.max }.max()!!
        minimap.ordinate.min = yColumns.map { it.min }.min()!!
        updateSizes()
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.rawX - x - paddingLeft
        val y = event.rawY - y - paddingTop
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (minimap.top.rangeTo(minimap.bottom).contains(y)) {
                    if (x < minimapStart - minimap.thikness) {
                        mode = MotionMode.MinimapCenter
                    } else if (x <= minimapStart + minimap.thikness) {
                        mode = MotionMode.MinimapLeft
                    } else if (x < minimapEnd - minimap.thikness) {
                        mode = MotionMode.MinimapCenter
                    } else if (x <= minimapEnd + minimap.thikness) {
                        mode = MotionMode.MinimapRight
                    } else {
                        mode = MotionMode.MinimapCenter
                    }
                }
                true
            }
            MotionEvent.ACTION_MOVE -> {
                when (mode) {
                    MotionMode.MinimapCenter -> {

                    }
                    MotionMode.MinimapLeft -> {
                        abscissa.axis.min = minimap.fromWorld(x)
                    }
                    MotionMode.MinimapRight -> {
                        abscissa.axis.max = minimap.fromWorld(x)
                    }
                    MotionMode.None -> {
                        return true
                    }
                }
                abscissa.setSize(size)
                invalidate()
                true
            }
            MotionEvent.ACTION_UP -> {
                mode = MotionMode.None
                performClick()
            }
            else -> super.onTouchEvent(event)
        }
    }
}