package ru.nobirds.rutracker.rest

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import ru.nobirds.rutracker.CategoryAndTorrents
import ru.nobirds.rutracker.repository.CategoryRepository
import ru.nobirds.rutracker.repository.TorrentRepository
import kotlin.collections.asSequence
import kotlin.collections.map
import kotlin.collections.sortedBy
import kotlin.collections.toList
import kotlin.sequences.groupBy

@RequestMapping("/api/torrents")
class TorrentsController(val torrentRepository: TorrentRepository, val categoryRepository: CategoryRepository) {

    @ResponseBody
    @RequestMapping(method = arrayOf(RequestMethod.GET))
    fun find(@RequestParam name:String):List<CategoryAndTorrents> = torrentRepository
            .search(name)
            .asSequence()
            .groupBy { it.categoryId }
            .map { CategoryAndTorrents(categoryRepository.findById(it.key)!!, it.value.sortedBy { it.name }) }
            .sortedBy { it.category.name }
            .toList()

}
