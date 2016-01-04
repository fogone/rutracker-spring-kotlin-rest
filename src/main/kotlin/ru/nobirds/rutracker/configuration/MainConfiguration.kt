package ru.nobirds.rutracker.configuration

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.web.SpringBootServletInitializer
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(JdbcRepositoriesConfiguration::class, ImportConfiguration::class, RestConfiguration::class)
@EnableAutoConfiguration
open class MainConfiguration : SpringBootServletInitializer() {

    override fun configure(builder: SpringApplicationBuilder): SpringApplicationBuilder {
        return builder.sources(MainConfiguration::class.java)
    }

}