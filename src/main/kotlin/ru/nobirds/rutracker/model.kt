package ru.nobirds.rutracker

import java.util.Date

val RootCategory:Category = Category(0, "root", null)

data class Category(val id:Long, val name:String, val parent:Category?)

data class Torrent(val id:Long,val categoryId:Long, val hash:String, val name:String, val size:Long, val created:Date)

data class CategoryAndTorrents(val category:Category, val torrents:List<Torrent>)