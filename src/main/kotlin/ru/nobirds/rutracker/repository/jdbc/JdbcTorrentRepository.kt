package ru.nobirds.rutracker.repository.jdbc

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter
import org.springframework.jdbc.core.RowMapper
import ru.nobirds.rutracker.Torrent
import ru.nobirds.rutracker.repository.TorrentRepository
import ru.nobirds.rutracker.utils.Batcher
import ru.nobirds.rutracker.utils.SimpleJdbcBatcher
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.annotation.PostConstruct
import kotlin.collections.emptyList
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.toTypedArray
import kotlin.text.isEmpty
import kotlin.text.split
import kotlin.text.trim

class JdbcTorrentRepository(val jdbcTemplate: JdbcTemplate) : TorrentRepository {
    private val rowMapper = RowMapper { rs: ResultSet, rowNum: Int ->
        Torrent(
                rs.getLong("id"), rs.getLong("category_id"),
                rs.getString("hash"), rs.getString("name"),
                rs.getLong("size"), rs.getDate("created")
        )
    }

    private val setter:ParameterizedPreparedStatementSetter<Torrent> =
            ParameterizedPreparedStatementSetter { ps: PreparedStatement, t: Torrent ->
                ps.setLong(1, t.id)
                ps.setLong(2, t.categoryId)
                ps.setString(3, t.hash)
                ps.setString(4, t.name)
                ps.setLong(5, t.size)
                ps.setDate(6, Date(t.created.time))
            }

    @PostConstruct
    private fun initialize() {
        createTable()
    }

    override fun batcher(size:Int): Batcher<Torrent> {
        return SimpleJdbcBatcher(
                size.toLong(), "INSERT INTO torrent (id, category_id, hash, name, size, created) VALUES (?, ?, ?, ?, ?, ?)", jdbcTemplate, setter)
    }

    override fun search(name: String): List<Torrent> {
        if(name.isEmpty())
            return emptyList()

        val parts = name.split(" ")

        val whereSql = parts.map { "UPPER(name) like UPPER(?)" }.joinToString(" AND ")

        val parameters = parts.map { it.trim() }.map { "%$it%" }.toTypedArray()

        return jdbcTemplate.query("SELECT id, category_id, hash, name, size, created FROM torrent WHERE $whereSql", rowMapper, *parameters)
    }

    override fun clear() {
        dropTable()
        createTable()
    }

    override fun count(): Int {
        return jdbcTemplate.queryForObject("SELECT count(id) FROM torrent", Int::class.java)
    }

    private fun createTable() {
        jdbcTemplate.update("CREATE TABLE IF NOT EXISTS torrent (id BIGINT PRIMARY KEY, category_id BIGINT, hash VARCHAR, name VARCHAR, size BIGINT, created VARCHAR)")
    }

    private fun dropTable() {
        jdbcTemplate.update("DROP TABLE torrent")
    }
}