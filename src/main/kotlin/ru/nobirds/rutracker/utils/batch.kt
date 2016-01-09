package ru.nobirds.rutracker.utils

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter
import java.io.Closeable
import java.util.*
import kotlin.collections.hashSetOf
import kotlin.collections.isNotEmpty

interface Batcher<T> : Closeable {

    fun add(value:T)

}

interface FilteringBatcher<T> : Batcher<T> {

    fun filter(value:T): Boolean {
        return true
    }

}

abstract open class AbstractBatcher<T>(val batchSize:Long) : FilteringBatcher<T> {

    private val batch = ArrayList<T>()

    override fun add(value: T) {
        if (!filter(value)) {
            return
        }

        batch.add(value)

        if (batch.size == batchSize.toInt()) {
            flush()
        }
    }

    protected open fun flush() {
        if (batch.isNotEmpty()) {
            flushImpl(batch)
            batch.clear()
        }
    }

    override fun close() {
        flush()
    }

    abstract fun flushImpl(batch: List<T>)

}

open class SimpleJdbcBatcher<T>(batchSize:Long, val sql: String,
                           val jdbcTemplate: JdbcTemplate,
                           val setter:ParameterizedPreparedStatementSetter<T>) : AbstractBatcher<T>(batchSize) {

    override fun flushImpl(batch: List<T>) {
        jdbcTemplate.batchUpdate(sql, batch, batch.size, setter)
    }

}

class UniqueKeyJdbcBatcher<K, T>(batchSize:Long,
                                 sql: String,
                                 jdbcTemplate: JdbcTemplate,
                                 setter:ParameterizedPreparedStatementSetter<T>,
                                 val fetcher: (T)->K,
                                 val checker:(K)->Boolean) : SimpleJdbcBatcher<T>(batchSize, sql, jdbcTemplate, setter) {

    private val keys = hashSetOf<K>()

    override fun filter(value: T): Boolean {
        val key = fetcher(value)

        return if (key in keys || checker(key)) {
            false
        } else {
            keys.add(key)
            true
        }
    }

    override protected fun flush() {
        super.flush()
        keys.clear()
    }

}
