package ru.nobirds.rutracker.rest

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import ru.nobirds.rutracker.Torrent
import ru.nobirds.rutracker.repository.TorrentRepository

@RequestMapping("/api/torrents")
class TorrentsController(val torrentRepository: TorrentRepository) {

    @ResponseBody
    @RequestMapping(method = arrayOf(RequestMethod.GET))
    fun find(@RequestParam name:String, @RequestParam(required = false) categoryId:Long?):List<Torrent> {
        return if(categoryId != null)
            torrentRepository.findByName(name, categoryId)
        else
            torrentRepository.findByName(name)
    }

}