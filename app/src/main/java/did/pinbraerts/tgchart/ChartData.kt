package did.pinbraerts.tgchart

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.graphics.Color
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.coroutines.CoroutineContext

enum class DataType {
    Line,
    Dot
}

data class Column(
    var type: DataType = DataType.Dot,
    var name: String = "",
    var color: Int = 0,
    var data: ArrayList<Long> = arrayListOf(),
    var min: Long = Long.MAX_VALUE,
    var max: Long = Long.MIN_VALUE
) {
    val interval
        get() = max - min
}

data class Parser(
    val reader: InputStreamReader
) {
    var ilast: Int = 0
    var last: Char = ' '

    fun skipSpaces() {
        while(arrayOf(' ', '\t', '\n', '\r').contains(last)) {
            next()
        }
    }

    fun eof() = ilast == -1

    fun next() {
        ilast = reader.read()
        last = ilast.toChar()
    }

    fun expect(c: Char) {
        skipSpaces()
        if(last != c) throw IllegalArgumentException("Expected char '$c'")
        next()
    }
    fun till(c: Char): String {
        skipSpaces()
        val res = StringBuilder()
        while(last != c) {
            res.append(last)
            next()
        }
        next()
        return res.toString()
    }
    fun between(begin: Char, end: Char = begin): String {
        expect(begin)
        return till(end)
    }
    inline fun block(begin: Char, end: Char, delim: Char = ',', block: Parser.() -> Unit) {
        expect(begin)
        skipSpaces()
        block()
        skipSpaces()
        while(last == delim) {
            next()
            skipSpaces()
            block()
            skipSpaces()
        }
        expect(end)
    }
    fun readLong(radix: Int = 10): Long {
        val res = StringBuilder()
        while (last.isDigit() || 'a'.rangeTo('f').contains(last) || 'A'.rangeTo('F').contains(last)) {
            res.append(last)
            next()
        }
        return res.toString().toLong(radix)
    }
}

typealias Page = HashMap<String, Column>
typealias Pages = ArrayList<Page>

class LoadScope: CoroutineScope {
    override val coroutineContext = Job()

    fun load(stream: InputStream, update: (Page) -> Unit) = launch {
        Parser(stream.reader()).block('[', ']') {
            val page = Page()
            block('{', '}') {
                val name = between('"')
                expect(':')
                when (name) {
                    "columns" -> block('[', ']') {
                        var column: Column? = null
                        block('[', ']') {
                            skipSpaces()
                            if (last == '"') {
                                val id = between('"')
                                column = page.getOrPut(id) { Column() }
                            } else {
                                val x = readLong()
                                column?.apply {
                                    if(x < min) min = x
                                    else if(x > max) max = x
                                    data.add(x)
                                }
                            }
                        }
                    }
                    "types" -> block('{', '}') {
                        val id = between('"')
                        expect(':')
                        page.getOrPut(id) { Column() }.type = when (between('"')) {
                            "x" -> DataType.Dot
                            "line" -> DataType.Line
                            else -> throw IllegalArgumentException("No such enum")
                        }
                    }
                    "names" -> block('{', '}') {
                        val id = between('"')
                        expect(':')
                        page.getOrPut(id) { Column() }.name = between('"')
                    }
                    "colors" -> block('{', '}') {
                        val id = between('"')
                        expect(':')
                        page.getOrPut(id) { Column() }.color = Color.parseColor(between('"'))
                    }
                    else -> throw IllegalArgumentException("No such field")
                }
            }
            update(page)
        }
    }
}
