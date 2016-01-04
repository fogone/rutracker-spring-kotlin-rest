package ru.nobirds.rutracker.repository.jdbc

import org.springframework.jdbc.core.JdbcTemplate
import ru.nobirds.rutracker.repository.VersionRepository
import javax.annotation.PostConstruct

class JdbcVersionRepository(val jdbcTemplate: JdbcTemplate) : VersionRepository {
    @PostConstruct
    private fun initialize() {
        createTable()
    }

    override fun getCurrentVersion(): Long? {
        return jdbcTemplate.queryForObject("SELECT MAX(version) FROM base_version", Long::class.java)
    }

    override fun updateCurrentVersion(version: Long) {
        jdbcTemplate.update("INSERT INTO base_version (version) VALUES (?)", version)
    }

    override fun clear() {
        dropTable()
        createTable()
    }

    private fun createTable() {
        jdbcTemplate.update("CREATE TABLE IF NOT EXISTS base_version (version BIGINT PRIMARY KEY)")
    }

    private fun dropTable() {
        jdbcTemplate.update("DROP TABLE base_version")
    }

}