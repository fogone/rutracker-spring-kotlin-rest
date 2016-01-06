package ru.nobirds.rutracker

data class Category(val id:Long, val name:String, val parent:Category?)

val RootCategory:Category = Category(0, "root", null)

data class Torrent(val id:Long,val categoryId:Long, val hash:String, val name:String, val size:Long, val created:String)

data class CategoryAndTorrents(val category:Category, val torrents:List<Torrent>)