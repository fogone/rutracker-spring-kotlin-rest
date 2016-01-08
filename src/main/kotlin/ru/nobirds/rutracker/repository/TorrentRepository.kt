package ru.nobirds.rutracker.repository

import ru.nobirds.rutracker.Torrent
import ru.nobirds.rutracker.utils.Batcher

interface TorrentRepository {

    fun contains(id:Long):Boolean

    fun findById(id:Long): Torrent?

    fun findByName(name:String):List<Torrent>

    fun findByName(name:String, categoryId:Long):List<Torrent>

    fun clear()

    fun count(): Int

    fun batcher(size:Int):Batcher<Torrent>

}

