package ru.nobirds.rutracker.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.nobirds.rutracker.ImportService
import ru.nobirds.rutracker.repository.CategoryRepository
import ru.nobirds.rutracker.repository.TorrentRepository
import ru.nobirds.rutracker.repository.VersionRepository

@ConfigurationProperties("import")
data class ImportProperties(var directory:String = "", var index:String = "",
                            var threads:Int = Runtime.getRuntime().availableProcessors(),
                            var categoryBatchSize:Int = 100,
                            var torrentBatchSize:Int = 5000,
                            var ignoreStoredVersions:Boolean = false)

@Configuration
@EnableConfigurationProperties(ImportProperties::class)
open class ImportConfiguration {

    @Bean
    open fun importService(properties: ImportProperties,
                           categoryRepository: CategoryRepository,
                           torrentRepository: TorrentRepository,
                           versionRepository: VersionRepository): ImportService {
        return ImportService(properties, categoryRepository, torrentRepository, versionRepository)
    }

}