package ru.nobirds.rutracker.repository

interface VersionRepository {

    fun getCurrentVersion():Long?

    fun updateCurrentVersion(version:Long)

    fun clear()

}

