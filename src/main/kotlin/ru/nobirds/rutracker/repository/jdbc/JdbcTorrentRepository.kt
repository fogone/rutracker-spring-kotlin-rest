package ru.nobirds.rutracker.repository.jdbc

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter
import org.springframework.jdbc.core.RowMapper
import ru.nobirds.rutracker.Torrent
import ru.nobirds.rutracker.repository.TorrentRepository
import ru.nobirds.rutracker.utils.Batcher
import ru.nobirds.rutracker.utils.SimpleJdbcBatcher
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.annotation.PostConstruct

class JdbcTorrentRepository(val jdbcTemplate: JdbcTemplate) : TorrentRepository {
    private val rowMapper = RowMapper { rs: ResultSet, rowNum: Int ->
        Torrent(
                rs.getLong("id"), rs.getLong("category_id"),
                rs.getString("hash"), rs.getString("name"),
                rs.getLong("size"), rs.getString("created")
        )
    }

    private val setter:ParameterizedPreparedStatementSetter<Torrent> =
            ParameterizedPreparedStatementSetter { ps: PreparedStatement, t: Torrent ->
                ps.setLong(1, t.id)
                ps.setLong(2, t.categoryId)
                ps.setString(3, t.hash)
                ps.setString(4, t.name)
                ps.setLong(5, t.size)
                ps.setString(6, t.created)
            }

    @PostConstruct
    private fun initialize() {
        createTable()
    }


    override fun add(torrent: Torrent) {
        batcher(1).add(torrent).flush()
    }

    override fun batcher(size:Int): Batcher<Torrent> {
        return SimpleJdbcBatcher(
                size.toLong(), "INSERT INTO torrent (id, category_id, hash, name, size, created) VALUES (?, ?, ?, ?, ?, ?)", jdbcTemplate, setter)
    }

    override fun contains(id: Long): Boolean {
        return jdbcTemplate.queryForObject("SELECT count(id) FROM torrent WHERE id = ?", Int::class.java, id) > 0
    }

    override fun findById(id: Long): Torrent? {
        return jdbcTemplate.queryForObject("SELECT id, category_id, hash, name, size, created FROM torrent WHERE id = ?", rowMapper, id)
    }

    override fun findByName(name: String, categoryId: Long): List<Torrent> {
        return jdbcTemplate.query("SELECT id, category_id, hash, name, size, created FROM torrent WHERE name like ? and category_id = ?", rowMapper, "%$name%", categoryId)
    }

    override fun findByName(name: String): List<Torrent> {
        return jdbcTemplate.query("SELECT id, category_id, hash, name, size, created FROM torrent WHERE name like ?", rowMapper, "%$name%")
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