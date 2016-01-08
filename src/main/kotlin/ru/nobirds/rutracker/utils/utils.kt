package ru.nobirds.rutracker.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable

inline fun timed(block:()->Unit):Long {
    val startTime = System.currentTimeMillis()

    block()

    return System.currentTimeMillis() - startTime
}

operator fun <T> List<T>.component6():T = this[5]
operator fun <T> List<T>.component7():T = this[6]
operator fun <T> List<T>.component8():T = this[7]
operator fun <T> List<T>.component9():T = this[8]
operator fun <T> List<T>.component10():T = this[9]

inline fun <reified T:Any> T.logger():Logger = LoggerFactory.getLogger(T::class.java)

infix fun Closeable.and(other:Closeable):Closeable = Closeable {
    this@and.close()
    other.close()
}
