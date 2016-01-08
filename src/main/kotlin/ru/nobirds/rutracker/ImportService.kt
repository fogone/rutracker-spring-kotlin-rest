package ru.nobirds.rutracker

import org.slf4j.Logger
import ru.nobirds.rutracker.configuration.ImportProperties
import ru.nobirds.rutracker.repository.CategoryRepository
import ru.nobirds.rutracker.repository.TorrentRepository
import ru.nobirds.rutracker.repository.VersionRepository
import ru.nobirds.rutracker.utils.CsvParser
import ru.nobirds.rutracker.utils.and
import ru.nobirds.rutracker.utils.component6
import ru.nobirds.rutracker.utils.component7
import ru.nobirds.rutracker.utils.logger
import ru.nobirds.rutracker.utils.timed
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import kotlin.collections.asSequence
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.collections.component4
import kotlin.collections.component5
import kotlin.collections.map
import kotlin.concurrent.getOrSet
import kotlin.sequences.filter
import kotlin.sequences.forEach
import kotlin.sequences.map
import kotlin.sequences.maxBy
import kotlin.sequences.toList
import kotlin.text.matches
import kotlin.text.toLong
import kotlin.text.toRegex

class ImportService(
        val importProperties: ImportProperties,
        val categoryRepository: CategoryRepository,
        val torrentRepository: TorrentRepository,
        val versionRepository: VersionRepository) {

    private val logger:Logger = logger()

    private val versionRegex = "\\d+".toRegex()
    private val dateParser = ThreadLocal<DateFormat>()

    private val directory: Path = Paths.get(importProperties.directory)
    private val index:String = importProperties.index

    fun checkUpdates() {
        require(Files.isDirectory(directory)) { "$directory is not a directory" }

        val directory = findNewestDirectory()

        require(directory != null) { "Version directories not found" }

        val currentVersion = versionRepository.getCurrentVersion()

        val foundVersion = directory!!.fileName.toString().toLong()

        if (currentVersion == null || currentVersion < foundVersion || importProperties.ignoreStoredVersions) {
            logger.info("Found new base version {}, all entities will be reimported", foundVersion)

            val torrentsCount = updateEntities(directory)

            versionRepository.updateCurrentVersion(foundVersion)

            logger.info("Reimport of version {} done. Torrents count: {}", foundVersion, torrentsCount)
        } else {
            logger.info("Torrents base up to date")
        }
    }

    private fun updateEntities(directory: Path):Int {
        val clearTime = timed {
            versionRepository.clear()
            categoryRepository.clear()
            torrentRepository.clear()
        }

        logger.info("Old data clean in {} secs", clearTime/1000f)

        val importTime = timed {
            importCategoriesAndTorrents(directory)
        }

        val categoriesCount = categoryRepository.count()
        val torrentsCount = torrentRepository.count()

        logger.info("Categories and torrents imported. Total {} and {} in {} secs", categoriesCount, torrentsCount, importTime/1000f)

        return torrentsCount
    }

    private inline fun <R> withExecutor(block:(ExecutorService)->R):R {
        val executor = createExecutor()
        try {
            return block(executor)
        } finally {
            executor.shutdown()
        }
    }

    private fun createExecutor(): ExecutorService = Executors.newFixedThreadPool(importProperties.threads)

    private fun findNewestDirectory(): Path? = Files.newDirectoryStream(directory).use {
        it.asSequence()
                .filter { it.fileName.toString().matches(versionRegex) }
                .maxBy { it.fileName.toString().toLong() }
    }

    private fun importCategoriesAndTorrents(directory:Path) = withExecutor { executor ->
        val topCategories = importTopCategories(directory)

        executor
                .invokeAll(topCategories.map { createImportFileWorker(directory, it) })
                .map { it.get() }
    }

    private fun createImportFileWorker(directory: Path, topCategory: CategoryAndFile):Callable<Unit> = Callable {
        val categoryBatcher = categoryRepository.batcher(importProperties.categoryBatchSize)
        val torrentBatcher = torrentRepository.batcher(importProperties.torrentBatchSize)

        (categoryBatcher and torrentBatcher).use {
            parser(directory, topCategory.file).use {
                it
                        .map { createCategoryAndTorrent(topCategory.category, it) }
                        .forEach {
                            categoryBatcher.add(it.category)
                            torrentBatcher.add(it.torrent)
                        }
            }
        }
    }

    private fun importTopCategories(directory: Path):List<CategoryAndFile> = parser(directory, index).use { parser ->
        categoryRepository.batcher(50).use { batcher ->
            parser.map {
                val category = createTopCategory(it)
                batcher.add(category)
                CategoryAndFile(category, it[2])
            }.toList()
        }
    }

    private fun createTopCategory(tuple: List<String>): Category {
        val (id, name) = tuple
        return Category(id.toLong(), name, RootCategory)
    }

    private fun createCategoryAndTorrent(parent: Category, it: List<String>): CategoryAndTorrent {
        val (categoryId, categoryName, id, hash, name, size, created) = it
        return CategoryAndTorrent(
                Category(categoryId.toLong(), categoryName, parent),
                Torrent(id.toLong(), categoryId.toLong(), hash, name, size.toLong(), created.toDate())
        )
    }

    private fun String.toDate(): Date = dateParser.getOrSet { SimpleDateFormat("yyyy-MM-dd HH:mm:ss") }.parse(this)

    private fun parser(path: Path, name: String): CsvParser {
        return CsvParser(Files.newBufferedReader(path.resolve(name)))
    }

    internal data class CategoryAndFile(val category: Category, val file:String)
    internal data class CategoryAndTorrent(val category: Category, val torrent: Torrent)

    @PostConstruct
    fun handleApplicationStartedEvent() {
        checkUpdates()
    }

}

