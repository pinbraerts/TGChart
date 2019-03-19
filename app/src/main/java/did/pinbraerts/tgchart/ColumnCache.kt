package did.pinbraerts.tgchart

data class ColumnCache(
    var color: Int,
    val max: Long,
    val min: Long,
    val lines: FloatArray,
    val range: Long = max - min,
    var doCount: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ColumnCache

        if (!lines.contentEquals(other.lines)) return false
        if (color != other.color) return false
        if (max != other.max) return false
        if (min != other.min) return false

        return true
    }
    override fun hashCode(): Int {
        var result = lines.contentHashCode()
        result = 31 * result + color
        result = 31 * result + max.hashCode()
        result = 31 * result + min.hashCode()
        return result
    }

    constructor(x: Column, y: Column): this(
        y.color,
        y.max,
        y.min,
        FloatArray(y.data.size * 4 - 2) {
            when(it % 4) {
                0 -> ((x.data[it / 4] - x.min).toDouble() / x.interval).toFloat()
                1 -> (1 - y.data[it / 4].toDouble() / y.max).toFloat()
                2 -> ((x.data[it / 4 + 1] - x.min).toDouble() / x.interval).toFloat()
                else -> (1 - y.data[it / 4 + 1].toDouble() / y.max).toFloat()
            }
        }
    )
}