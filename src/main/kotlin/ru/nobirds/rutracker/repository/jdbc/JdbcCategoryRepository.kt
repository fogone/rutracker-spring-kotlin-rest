package ru.nobirds.rutracker.repository.jdbc

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter
import org.springframework.jdbc.core.RowMapper
import ru.nobirds.rutracker.Category
import ru.nobirds.rutracker.RootCategory
import ru.nobirds.rutracker.repository.CategoryRepository
import ru.nobirds.rutracker.utils.Batcher
import ru.nobirds.rutracker.utils.UniqueKeyJdbcBatcher
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.annotation.PostConstruct

class JdbcCategoryRepository(val jdbcTemplate: JdbcTemplate) : CategoryRepository {

    private val rowMapper =
            RowMapper { rs: ResultSet, rowNum: Int -> Category(rs.getLong("id"), rs.getString("name"), findById(rs.getLong("parent"))) }

    private val setter =
            ParameterizedPreparedStatementSetter { ps: PreparedStatement, c: Category ->
                ps.setLong(1, c.id)
                ps.setString(2, c.name)
                ps.setLong(3, c.parent?.id ?: 0)
            }

    @PostConstruct
    private fun initialize() {
        createTable()
    }

    override fun batcher(size:Int): Batcher<Category> {
        return UniqueKeyJdbcBatcher(size.toLong(), "INSERT INTO category (id, name, parent) VALUES (?, ?, ?)",
                jdbcTemplate, setter, { it.id }, { contains(it) })
    }

    override fun contains(id: Long): Boolean {
        return jdbcTemplate.queryForObject("SELECT count(id) FROM category WHERE id = ?", Long::class.java, id) > 0
    }

    override fun findById(id: Long): Category? = if (id != 0L)
        jdbcTemplate.queryForObject("SELECT id, name, parent FROM category WHERE id = ?", rowMapper, id) else RootCategory

    override fun all(): List<Category> {
        return jdbcTemplate.query("SELECT id, name, parent FROM category", rowMapper)
    }

    override fun count(): Int {
        return jdbcTemplate.queryForObject("SELECT count(id) FROM category", Int::class.java)
    }

    override fun clear() {
        dropTable()
        createTable()
    }

    private fun dropTable() {
        jdbcTemplate.update("DROP TABLE category")
    }

    private fun createTable() {
        jdbcTemplate.update("CREATE TABLE IF NOT EXISTS category (id BIGINT PRIMARY KEY, name VARCHAR, parent BIGINT)")
    }

}

