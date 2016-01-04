package ru.nobirds.rutracker.rest

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import ru.nobirds.rutracker.Category
import ru.nobirds.rutracker.repository.CategoryRepository

@RequestMapping("/api/categories")
class CategoriesController(val categoryRepository: CategoryRepository) {

    @ResponseBody
    @RequestMapping(method = arrayOf(RequestMethod.GET))
    fun find(@RequestParam id:Long):Category {
        val category = categoryRepository.findById(id)

        if(category != null)
            return category
        else
            throw EntityNotFoundException("Category with id $id not found.")
    }

}