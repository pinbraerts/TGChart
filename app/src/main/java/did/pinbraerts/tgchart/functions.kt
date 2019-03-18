package did.pinbraerts.tgchart

import android.graphics.*
import android.os.Build

fun Canvas.clipOut(r: RectF) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        clipOutRect(r.left, r.top, r.right - 0.5f, r.bottom)
    else
        @Suppress("DEPRECATION")
        clipRect(r.left, r.top, r.right - 0.5f, r.bottom, Region.Op.DIFFERENCE)
}

fun clamp(v: Long, min: Long, max: Long) = when {
    v < min -> min
    v > max -> max
    else -> v
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

fun Long.prettyToString(): String = when {
    this < 1000 -> this.toString()
    this < 1000000 -> (this / 1000).toString() + "K"
    this < 1000000000 -> (this / 1000000).toString() + "M"
    else -> this.toString()
}
