package ru.nobirds.rutracker

import ru.nobirds.rutracker.configuration.ImportProperties
import ru.nobirds.rutracker.repository.CategoryRepository
import ru.nobirds.rutracker.repository.TorrentRepository
import ru.nobirds.rutracker.repository.VersionRepository
import ru.nobirds.rutracker.utils.CsvParser
import ru.nobirds.rutracker.utils.component6
import ru.nobirds.rutracker.utils.component7
import ru.nobirds.rutracker.utils.logger
import ru.nobirds.rutracker.utils.timed
import ru.nobirds.rutracker.utils.use
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.PostConstruct

class ImportService(
        val importProperties: ImportProperties,
        val categoryRepository: CategoryRepository,
        val torrentRepository: TorrentRepository,
        val versionRepository: VersionRepository) {

    private val logger = logger()

    private val versionRegex = "\\d+".toRegex()

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

    private fun updateEntities(directory: Path):Int = withExecutor { executor ->
        val clearTime = timed {
            versionRepository.clear()
            categoryRepository.clear()
            torrentRepository.clear()
        }

        logger.info("Old data clean in {} secs", clearTime/1000f)

        val categoriesImportTime = timed {
            importCategories(executor, directory)
        }

        logger.info("Categories imported. Total {} in {} secs", categoryRepository.count(), categoriesImportTime/1000f)

        val torrentsImportTime = timed {
            importTorrents(executor, directory)
        }

        val torrentsCount = torrentRepository.count()

        logger.info("Torrents imported. Total {} in {} secs", torrentsCount, torrentsImportTime/1000f)

        torrentsCount
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

    private fun importCategories(executor: ExecutorService, directory:Path) {
        val topCategories = importTopCategories(directory)

        executor
                .invokeAll(topCategories.map { importTopCategoryWorker(directory, it) })
                .map { it.get() }
    }

    private fun importTopCategoryWorker(directory: Path, topCategory: CategoryAndFile):Callable<Unit> = Callable {
        categoryRepository.batcher(importProperties.categoryBatchSize).use {
            parser(directory, topCategory.file).use {
                it
                        .map { createCategory(it, topCategory.category.id) }
                        .forEach { add(it) }
            }
        }
    }

    private fun importTopCategories(directory: Path):List<CategoryAndFile> = parser(directory, index).use {
        it.map {
            val category = createCategory(it)
            categoryRepository.add(category)
            CategoryAndFile(category, it[2])
        }.toList()
    }

    private fun createCategory(tuple: List<String>, parentId: Long = 0): Category {
        val (id, name) = tuple
        return Category(id.toLong(), name, parentId)
    }

    private fun importTorrents(executor: ExecutorService, directory:Path) {
        val contentFiles = findContentFiles(directory)

        executor
                .invokeAll(contentFiles.map { importTorrentsWorker(directory, it) })
                .map { it.get() }
    }

    private fun importTorrentsWorker(directory: Path, file:String):Callable<Unit> = Callable {
        torrentRepository.batcher(importProperties.torrentBatchSize).use {
            parser(directory, file).use {
                for (tuple in it) {
                    add(createTorrent(tuple))
                }
            }
        }
    }

    private fun createTorrent(it: List<String>): Torrent {
        val (categoryId, categoryName, id, hash, name, size, created) = it
        return Torrent(id.toLong(), categoryId.toLong(), hash, name, size.toLong(), created)
    }

    private fun findContentFiles(directory:Path):List<String> =
            parser(directory, index).use { it.map { it[2] }.toList() }

    private fun parser(path: Path, name: String): CsvParser {
        return CsvParser(Files.newBufferedReader(path.resolve(name)))
    }

    internal data class CategoryAndFile(val category: Category, val file:String)

    @PostConstruct
    fun handleApplicationStartedEvent() {
        checkUpdates()
    }

}

