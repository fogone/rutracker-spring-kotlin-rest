package ru.nobirds.rutracker.utils

import java.io.BufferedReader
import java.io.Closeable
import java.io.Reader
import java.nio.charset.MalformedInputException
import java.util.ArrayList


fun <A:Appendable> A.appendTimes(char:Char, count:Int):A {
    if(count > 0) {
        var counter = count
        while(counter > 0) {
            append(char)
            counter--
        }
    }
    return this
}

fun StringBuilder.clear():StringBuilder {
    //delete(0, length())
    setLength(0)
    return this
}

fun StringBuilder.toStringAndClear():String {
    val string = toString()
    clear()
    return string
}

public object Chars {
    public val SPACE_CHAR:Int = ' '.toInt()

    public val BOM_CHARS:IntArray = intArrayOf('\uFEFF'.toInt(), '\uFFFE'.toInt())

    public val NEW_LINE_CHAR:Int = '\n'.toInt()
    public val RETURN_CARET_CHAR:Int = '\r'.toInt()
    public val END_FILE_CHAR:Int = -1
}

public fun Int.isEndOfFile(): Boolean = this == Chars.END_FILE_CHAR
public fun Int.isEndOfLine(): Boolean = this == Chars.NEW_LINE_CHAR || this == Chars.RETURN_CARET_CHAR || this == Chars.END_FILE_CHAR
public fun Int.isNewLineChar(): Boolean = this == Chars.NEW_LINE_CHAR
public fun Int.isReturnCaretChar(): Boolean = this == Chars.RETURN_CARET_CHAR

public fun Int.isBomChar(): Boolean = this in Chars.BOM_CHARS

public class CsvFormatException(message: String, val line: Int, val rawLine: Int, val rawColumn: Int, cause:Throwable? = null) :
        RuntimeException(message, cause)

class ArrayBufferedReader(reader: Reader, val bufferSize:Int = 1 * 1024 * 1024) : Reader() {

    private val reader = if(reader is BufferedReader) reader else BufferedReader(reader)

    private var position = 0
    private var size = 0
    private val array = CharArray(bufferSize)

    override fun read(cbuf: CharArray?, off: Int, len: Int): Int {
        throw UnsupportedOperationException()
    }

    override fun read(): Int {
        if(position == size) {
            if(!preRead())
                return -1
        }

        val value = array[position]

        position++

        return value.toInt()
    }

    private fun preRead():Boolean {
        val read = reader.read(array)

        if (read == -1)
            return false

        position = 0
        size = read

        return true
    }

    override fun close() {
        reader.close()
    }
}

public class PeekReader(reader: Reader, val normalizedEndLine:Int = Chars.NEW_LINE_CHAR) : Reader() {

    private val reader = ArrayBufferedReader(reader)

    private var next: Int = 0

    public var line: Int = 1
        private set

    public var column: Int = 1
        private set

    init {
        readNext()
        // skip BOM
        if (next.isBomChar()) {
            readNext()
        }
    }

    public override fun read(): Int {
        if (next.isEndOfLine()) {
            line++
            column=0

            return replaceEndLineChars()
        }

        column++

        val current = next

        readNext()

        return current
    }

    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        throw UnsupportedOperationException()
    }

    public fun peek(): Int {
        return next
    }

    private fun replaceEndLineChars():Int {
        require(next.isEndOfLine())

        val current = next

        readNext()

        if (current.isReturnCaretChar() && next.isNewLineChar()) {
            readNext()
        }

        return normalizedEndLine
    }

    public override fun close() {
        reader.close()
    }

    private fun readNext() {
        next = reader.read()
    }

}

class RecordStream(reader: Reader, quote: Char, delimiter: Char, newLine:Char) : Closeable {

    private enum class State {
        NORMAL,
        QUOTED,
        AFTER_QUOTED
    }

    private val quote: Int = quote.toInt()
    private val delimiter: Int = delimiter.toInt()
    private val newLine:Int = newLine.toInt()

    private val reader by lazy(LazyThreadSafetyMode.NONE) { PeekReader(reader, newLine.toInt()) }

    private val builder = StringBuilder()

    public var record: Int = 0
        private set

    public fun read(result: MutableList<String>): Boolean {
        try {
            return readImpl(result)
        } catch(e: MalformedInputException) {
            throw CsvFormatException("Csv format problem: Wrong charset or binary file",
                    this.record, 0, 0, e)
        }
    }

    @Suppress("UNUSED_VALUE")
    private fun readImpl(result: MutableList<String>): Boolean {
        result.clear()

        if (reader.peek().isEndOfFile())
            return false

        builder.clear()

        var spaces = 0
        var state = State.NORMAL

        record++

        if (reader.peek().isEndOfLine())
            throw createException("Empty line")

        loop@while(true) {

            val c = reader.read()

            when (state) {
                State.NORMAL -> when (c) {
                    delimiter -> {
                        if (spaces > 0) {
                            builder.appendTimes(' ', spaces)
                            spaces = 0
                        }

                        result.add(builder.toStringAndClear())
                    }
                    quote -> {
                        // quote first on line cannot be escaped
                        if (builder.length != 0 || spaces != 0)
                            throw createException("Unexpected character")

                        state = State.QUOTED
                    }
                    Chars.SPACE_CHAR -> spaces++
                    newLine -> {
                        if (spaces > 0) {
                            builder.appendTimes(' ', spaces)
                            spaces = 0
                        }
                        // save token
                        result.add(builder.toStringAndClear())
                        return true
                    }
                    else -> {
                        if (spaces > 0) {
                            builder.appendTimes(' ', spaces)
                            spaces = 0
                        }
                        // if just a normal character
                        builder.append(c.toChar())
                    }
                }

                State.QUOTED -> when (c) {
                    newLine -> {
                        if (reader.peek().isEndOfFile())
                            throw createException("Unexpected end of field")

                        builder.append(c.toChar()) // replace new line with space

                        continue@loop
                    }
                    quote -> {
                        // if next char is quote too
                        if (reader.peek() == quote) {
                            // append quote
                            builder.append(c.toChar())
                            // skip next quote
                            reader.read()
                        } else {
                            // a single quote, just change state
                            spaces = 0
                            result.add(builder.toStringAndClear())
                            state = State.AFTER_QUOTED
                        }
                    }
                    else -> builder.append(c.toChar())
                }

                State.AFTER_QUOTED -> when (c) {
                    delimiter -> state = State.NORMAL
                    newLine -> return true
                    else -> throw createException("Unexpected character")
                }
            }
        }
    }

    protected fun createException(message: String, cause:Throwable? = null): CsvFormatException = CsvFormatException(
            "Csv format problem: ${message} line: ${reader.line} column: ${reader.column}",
            this.record, reader.line, reader.column, cause
    )

    override fun close() {
        reader.close()
    }

}

public class CsvParser(val reader: Reader, val delimiter: Char = ';', val quote: Char = '"', val skipHeader: Boolean = false) :
        Sequence<List<String>>, Closeable {

    private val recordStream = RecordStream(reader, quote, delimiter, '\n')

    private var skip = skipHeader

    private val data = ArrayList<String>(30)

    override fun iterator(): Iterator<List<String>> = object : AbstractIterator<List<String>>() {
        override fun computeNext() {
            if (skip) {
                recordStream.read(data) // skip header
                skip = false
            }

            if (recordStream.read(data)) {
                setNext(data)
            } else {
                done()
            }
        }
    }

    override fun close() {
        recordStream.close()
    }

}

