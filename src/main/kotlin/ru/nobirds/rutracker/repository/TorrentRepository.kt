package ru.nobirds.rutracker.repository

import ru.nobirds.rutracker.Torrent
import ru.nobirds.rutracker.utils.Batcher

interface TorrentRepository {

    fun search(name:String):List<Torrent>

    fun clear()

    fun count(): Int

    fun batcher(size:Int):Batcher<Torrent>

}

