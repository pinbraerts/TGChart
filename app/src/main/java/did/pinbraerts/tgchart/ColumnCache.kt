package did.pinbraerts.tgchart

data class ColumnCache(
    var color: Int,
    val max: Long,
    val min: Long,
    val lines: LongArray,
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
        LongArray(y.data.size * 4 - 2) {
            when(it % 4) {
                0 -> x.data[it / 4]
                1 -> y.data[it / 4]
                2 -> x.data[it / 4 + 1]
                else -> y.data[it / 4 + 1]
            }
        }
    )
}