package ru.nobirds.rutracker

data class Category(val id:Long, val name:String, val parent:Long = 0)

data class Torrent(val id:Long,val categoryId:Long, val hash:String, val name:String, val size:Long, val created:String)
