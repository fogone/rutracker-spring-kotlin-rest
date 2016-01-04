package ru.nobirds.rutracker

import org.springframework.boot.SpringApplication
import ru.nobirds.rutracker.configuration.MainConfiguration

fun main(args: Array<String>) {
    SpringApplication
            .run(MainConfiguration::class.java, *args)
}

