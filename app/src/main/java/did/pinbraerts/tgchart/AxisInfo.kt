package did.pinbraerts.tgchart

data class AxisInfo (
    var min: Long = DEFAULT_MIN,
    var max: Long = DEFAULT_MAX,
    var num: Int = DEFAULT_NUM
): Iterable<Long> {
    override fun iterator(): Iterator<Long> = iter.iterator()

    val tick
        get() = (max - min) / num

    val range
        get() = min..max

    val iter
        get() = range step tick

    companion object {
        const val DEFAULT_MIN = 0L
        const val DEFAULT_MAX = 100L
        const val DEFAULT_NUM = 6
    }
}
