package ru.nobirds.rutracker.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.nobirds.rutracker.repository.CategoryRepository
import ru.nobirds.rutracker.repository.TorrentRepository
import ru.nobirds.rutracker.rest.CategoriesController
import ru.nobirds.rutracker.rest.TorrentsController

@Configuration
open class RestConfiguration {

    @Bean
    open fun categoriesController(categoryRepository: CategoryRepository):CategoriesController
            = CategoriesController(categoryRepository)

    @Bean
    open fun torrentsController(torrentRepository: TorrentRepository, categoryRepository: CategoryRepository):TorrentsController
            = TorrentsController(torrentRepository, categoryRepository)

}