package ru.nobirds.rutracker.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import ru.nobirds.rutracker.repository.CategoryRepository
import ru.nobirds.rutracker.repository.TorrentRepository
import ru.nobirds.rutracker.repository.VersionRepository
import ru.nobirds.rutracker.repository.jdbc.JdbcCategoryRepository
import ru.nobirds.rutracker.repository.jdbc.JdbcTorrentRepository
import ru.nobirds.rutracker.repository.jdbc.JdbcVersionRepository

@Configuration
@Profile("jdbc")
open class JdbcRepositoriesConfiguration {

    @Bean
    open fun versionRepository(jdbcTemplate: JdbcTemplate): VersionRepository {
        return JdbcVersionRepository(jdbcTemplate)
    }

    @Bean
    open fun categoryRepository(jdbcTemplate: JdbcTemplate): CategoryRepository {
        return JdbcCategoryRepository(jdbcTemplate)
    }

    @Bean
    open fun torrentRepository(jdbcTemplate: JdbcTemplate): TorrentRepository {
        return JdbcTorrentRepository(jdbcTemplate)
    }

}
