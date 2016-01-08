package ru.nobirds.rutracker.repository

import ru.nobirds.rutracker.Category
import ru.nobirds.rutracker.utils.Batcher

interface CategoryRepository {

    fun contains(id:Long):Boolean

    fun findById(id:Long): Category?

    fun count():Int

    fun clear()

    fun batcher(size:Int):Batcher<Category>

}

