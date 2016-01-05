package ru.nobirds.rutracker.rest

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import ru.nobirds.rutracker.CategoryAndTorrents
import ru.nobirds.rutracker.Torrent
import ru.nobirds.rutracker.repository.CategoryRepository
import ru.nobirds.rutracker.repository.TorrentRepository

@RequestMapping("/api/torrents")
class TorrentsController(val torrentRepository: TorrentRepository, val categoryRepository: CategoryRepository) {

    @ResponseBody
    @RequestMapping(method = arrayOf(RequestMethod.GET))
    fun find(@RequestParam name:String, @RequestParam(required = false) categoryId:Long?):List<CategoryAndTorrents> {
        if(name.isEmpty())
            return emptyList()

        return if(categoryId != null)
            torrentRepository.findByName(name, categoryId).groupByCategory(categoryRepository)
        else
            torrentRepository.findByName(name).groupByCategory(categoryRepository)
    }

}

fun List<Torrent>.groupByCategory(categoryRepository: CategoryRepository):List<CategoryAndTorrents> = asSequence()
        .groupBy { it.categoryId }
        .map { CategoryAndTorrents(categoryRepository.findById(it.key)!!, it.value.sortedBy { it.name }) }
        .sortedBy { it.category.name }
        .toList()