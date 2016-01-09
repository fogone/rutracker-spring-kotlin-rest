package ru.nobirds.rutracker.utils

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter
import java.io.Closeable
import java.util.ArrayList
import java.util.HashSet
import kotlin.collections.isNotEmpty

interface Batcher<T> : Closeable {

    fun add(value:T)

    fun flush()

    override fun close() {
        flush()
    }
}

abstract class AbstractBatcher<T>(val batchSize:Long) : Batcher<T> {

    private val batch = ArrayList<T>()

    override fun add(value: T) {
        batch.add(value)

        if (batch.size == batchSize.toInt()) {
            flush()
        }
    }

    override fun flush() {
        if (batch.isNotEmpty()) {
            flushImpl(batch)
            batch.clear()
        }
    }

    abstract fun flushImpl(batch: List<T>)

}

class SimpleJdbcBatcher<T>(batchSize:Long, val sql: String,
                           val jdbcTemplate: JdbcTemplate,
                           val setter:ParameterizedPreparedStatementSetter<T>) : AbstractBatcher<T>(batchSize) {

    override fun flushImpl(batch: List<T>) {
        jdbcTemplate.batchUpdate(sql, batch, batch.size, setter)
    }

}

class UniqueKeyBatcherWrapper<K, T>(
        val batcher:Batcher<T>,
        val keyFetcher: (T)->K,
        val uniqueChecker:(K)->Boolean) : Batcher<T> {

    private val keys = HashSet<K>()

    override fun add(value: T) {
        val key = keyFetcher(value)
        if (isUnique(key)) {
            batcher.add(value)
            keys.add(key)
        }
    }

    private fun isUnique(key: K): Boolean = key !in keys && !uniqueChecker(key)

    override fun flush() {
        batcher.flush()
        keys.clear()
    }
}

fun <T, K> Batcher<T>.withUniqueKeys(keyFetcher: (T)->K, uniqueChecker:(K)->Boolean):Batcher<T>
        = UniqueKeyBatcherWrapper(this, keyFetcher, uniqueChecker)
