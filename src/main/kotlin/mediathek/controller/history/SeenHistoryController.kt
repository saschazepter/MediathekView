/*
 * Copyright (c) 2026 derreisende77.
 * This code was developed as part of the MediathekView project https://github.com/mediathekview/MediathekView
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package mediathek.controller.history

import com.google.common.hash.BloomFilter
import com.google.common.hash.Funnels
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mediathek.config.application.ApplicationConfiguration
import mediathek.gui.messages.history.SeenHistoryChangedEvent
import mediathek.sqlite.SeenHistoryCorruptionHandler
import mediathek.tool.MessageBus
import mediathek.tool.sql.SqlDatabaseConfig
import org.apache.logging.log4j.LogManager
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.sql.SQLException
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Database based seen history controller.
 *
 * Public methods remain blocking for compatibility, while all JDBC access is confined
 * to a process-wide coroutine dispatcher with parallelism 1.
 */
class SeenHistoryController : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private val store = sharedStore()

    fun removeAll() {
        val removed = runStoreCatching("removeAll", false) {
            removeAllEntries()
            true
        }
        if (removed) {
            SeenHistoryCache.clear()
            sendChangeMessage()
        }
    }

    internal fun markSeen(entry: SeenHistoryEntry): Boolean {
        val inserted = runStoreCatching("markSeen single", false) {
            insertSeenEntry(entry)
        }
        if (inserted) {
            SeenHistoryCache.add(entry.source, entry.url)
            sendChangeMessage()
        }
        return inserted
    }

    internal fun markSeen(candidates: List<SeenHistoryEntry>): Boolean {
        val success = runStoreCatching("markSeen", false) {
            if (candidates.isNotEmpty()) {
                insertSeenEntries(candidates)
            }
            true
        }
        if (success) {
            candidates
                .groupBy(SeenHistoryEntry::source, SeenHistoryEntry::url)
                .forEach { (source, urls) -> SeenHistoryCache.add(source, urls) }
            sendChangeMessage()
        }
        return success
    }

    internal fun markUnseen(source: SeenHistorySource, url: String): Boolean {
        val success = runStoreCatching("markUnseen", false) {
            removeSeenUrl(source, url)
            true
        }
        if (success) {
            SeenHistoryCache.remove(source, url)
            sendChangeMessage()
        }
        return success
    }

    internal fun markUnseen(source: SeenHistorySource, urls: Collection<String>): Boolean {
        val success = runStoreCatching("markUnseen", false) {
            if (urls.isNotEmpty()) {
                removeSeenUrls(source, urls)
            }
            true
        }
        if (success) {
            SeenHistoryCache.remove(source, urls)
            sendChangeMessage()
        }
        return success
    }

    /**
     * Load source-specific URLs from database and store them in the process-wide memory cache.
     */
    fun prepareMemoryCache(source: SeenHistorySource = SeenHistorySource.FILM) {
        if (SeenHistoryCache.isPrepared(source)) {
            return
        }

        val urls = runStoreCatching("prepareMemoryCache", null as Set<String>?) {
            loadUrls(source)
        } ?: return

        SeenHistoryCache.load(source, urls)
        logger.trace("cache size: {}", SeenHistoryCache.size(source))
    }

    fun isMemoryCachePrepared(source: SeenHistorySource = SeenHistorySource.FILM): Boolean =
        SeenHistoryCache.isPrepared(source)

    fun performMaintenance() {
        logger.trace("Start maintenance")

        val applicationConfiguration = ApplicationConfiguration.getInstance()
        val lastRunDate = applicationConfiguration.seenHistoryMaintenanceLastRun
        val now = LocalDate.now()
        val shouldRunHeavyMaintenance = lastRunDate == null ||
            ChronoUnit.DAYS.between(lastRunDate, now) >= MAX_DAYS

        val success = runStoreCatching("Failed to execute maintenance script", false) {
            performMaintenance(shouldRunHeavyMaintenance)
            true
        }
        if (success) {
            SeenHistoryCache.clear()
            if (shouldRunHeavyMaintenance) {
                applicationConfiguration.seenHistoryMaintenanceLastRun = now
            }
        }

        logger.trace("Finished maintenance")
    }

    fun performDatabaseCompact() {
        runStoreCatching("performDatabaseCompact", Unit) {
            compactDatabase()
        }
    }

    internal fun hasBeenSeen(source: SeenHistorySource, url: String): Boolean {
        if (SeenHistoryCache.isPrepared(source)) {
            return hasBeenSeenFromPreparedCache(source, url) {
                runStoreCatching("hasBeenSeen", false) {
                    containsUrl(source, url)
                }
            }
        }

        return runStoreCatching("hasBeenSeen", false) {
            containsUrl(source, url)
        }
    }

    override fun close() {
        closed.set(true)
    }

    private fun <T> runStore(block: suspend SeenHistoryStore.() -> T): T = runBlocking {
        withContext(databaseDispatcher) {
            check(!closed.get()) { "SeenHistoryController is already closed." }
            store.block()
        }
    }

    private fun <T> runStoreCatching(
        errorMessage: String,
        fallback: T,
        block: suspend SeenHistoryStore.() -> T
    ): T {
        return try {
            runStore(block)
        } catch (ex: SQLException) {
            logger.error(errorMessage, ex)
            fallback
        }
    }

    private fun sendChangeMessage() {
        MessageBus.messageBus.publishAsync(SeenHistoryChangedEvent())
    }


    companion object {
        private val logger = LogManager.getLogger()
        private const val MAX_DAYS: Long = 30
        private val databaseDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
        private val sharedStoreLock = Any()
        @Volatile
        private var sharedStore: SeenHistoryStore? = null

        private fun sharedStore(): SeenHistoryStore {
            sharedStore?.let { return it }

            val createdStore = createStore()

            synchronized(sharedStoreLock) {
                sharedStore?.let { existingStore ->
                    runCatching { createdStore.close() }
                        .onFailure { logger.warn("Failed to close redundant seen history store", it) }
                    return existingStore
                }

                sharedStore = createdStore
                return createdStore
            }
        }

        private fun createStore(): SeenHistoryStore {
            return try {
                openStore(SqlDatabaseConfig.historyDbPath)
            } catch (ex: SeenHistoryCorruptionHandler.CorruptSeenHistoryDatabaseException) {
                SeenHistoryCorruptionHandler.resolveCorruption(ex.dbPath) { databasePath ->
                    openStore(databasePath)
                }
            }
        }

        private fun openStore(databasePath: Path): SeenHistoryStore {
            return SeenHistoryCorruptionHandler.openStoreOrThrowCorrupt(databasePath) { resolvedPath ->
                SeenHistoryStore(SqlDatabaseConfig.createDataSource(resolvedPath), resolvedPath)
            }
        }

        fun prepareSharedMemoryCache(source: SeenHistorySource) {
            SeenHistoryController().use { controller ->
                controller.prepareMemoryCache(source)
            }
        }

        fun hasBeenSeenFromSharedCache(source: SeenHistorySource, url: String): Boolean {
            if (!SeenHistoryCache.isPrepared(source)) {
                prepareSharedMemoryCache(source)
            }
            return hasBeenSeenFromPreparedCache(source, url) {
                hasBeenSeenInSharedStore(source, url)
            }
        }

        fun closeSharedStore() {
            runBlocking {
                withContext(databaseDispatcher) {
                    val storeToClose = synchronized(sharedStoreLock) {
                        sharedStore.also { sharedStore = null }
                    }
                    runCatching {
                        storeToClose?.close()
                    }.onFailure {
                        logger.warn("Failed to close shared seen history store", it)
                    }
                }
            }
        }

        private fun hasBeenSeenInSharedStore(source: SeenHistorySource, url: String): Boolean {
            return try {
                runBlocking {
                    withContext(databaseDispatcher) {
                        sharedStore().containsUrl(source, url)
                    }
                }
            } catch (ex: SQLException) {
                logger.error("hasBeenSeen", ex)
                false
            }
        }

        private fun hasBeenSeenFromPreparedCache(
            source: SeenHistorySource,
            url: String,
            exactLookup: () -> Boolean,
        ): Boolean {
            if (url.isBlank()) {
                return false
            }
            if (SeenHistoryCache.containsVerified(source, url)) {
                return true
            }
            if (!SeenHistoryCache.mightContain(source, url)) {
                return false
            }

            val seen = exactLookup()
            SeenHistoryCache.recordLookup(source, url, seen)
            return seen
        }

    }
}

internal data class SeenHistoryEntry(
    val source: SeenHistorySource,
    val theme: String,
    val title: String,
    val url: String
)

internal object SeenHistoryCache {
    private val lock = Any()
    private val caches = SeenHistorySource.entries.associateWith { SourceCache() }

    fun isPrepared(source: SeenHistorySource): Boolean =
        cacheFor(source).bloomFilter != null

    fun size(source: SeenHistorySource): Long =
        cacheFor(source).bloomFilter?.approximateElementCount() ?: 0L

    fun containsVerified(source: SeenHistorySource, url: String): Boolean =
        url.isNotBlank() && cacheFor(source).verifiedSeenUrls.contains(url)

    fun mightContain(source: SeenHistorySource, url: String): Boolean =
        url.isNotBlank() && cacheFor(source).bloomFilter?.mightContain(url) == true

    fun load(source: SeenHistorySource, urls: Set<String>) {
        synchronized(lock) {
            if (isPrepared(source)) {
                return
            }
            val cache = cacheFor(source)
            cache.verifiedSeenUrls.clear()
            cache.bloomFilter = createBloomFilter(urls)
        }
    }

    fun add(source: SeenHistorySource, url: String) {
        if (url.isBlank()) {
            return
        }
        add(source, listOf(url))
    }

    fun add(source: SeenHistorySource, urls: Collection<String>) {
        if (!isPrepared(source) || urls.isEmpty()) {
            return
        }
        synchronized(lock) {
            val cache = cacheFor(source)
            val bloomFilter = cache.bloomFilter ?: return
            urls.asSequence()
                .filter(String::isNotBlank)
                .forEach { url ->
                    bloomFilter.put(url)
                    cache.verifiedSeenUrls.add(url)
                }
        }
    }

    fun remove(source: SeenHistorySource, url: String) {
        if (url.isBlank()) {
            return
        }
        remove(source, listOf(url))
    }

    fun remove(source: SeenHistorySource, urls: Collection<String>) {
        if (urls.isEmpty()) {
            return
        }
        synchronized(lock) {
            val cache = cacheFor(source)
            urls.asSequence().filter(String::isNotBlank).forEach(cache.verifiedSeenUrls::remove)
        }
    }

    fun recordLookup(source: SeenHistorySource, url: String, seen: Boolean) {
        if (url.isBlank()) {
            return
        }
        val verifiedSeenUrls = cacheFor(source).verifiedSeenUrls
        if (seen) {
            verifiedSeenUrls.add(url)
        } else {
            verifiedSeenUrls.remove(url)
        }
    }

    fun clear() {
        synchronized(lock) {
            caches.values.forEach { cache ->
                cache.bloomFilter = null
                cache.verifiedSeenUrls.clear()
            }
        }
    }

    private fun cacheFor(source: SeenHistorySource): SourceCache =
        caches.getValue(source)

    private fun createBloomFilter(urls: Set<String>): BloomFilter<CharSequence> {
        val bloomFilter = BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8),
            urls.size.toLong().coerceAtLeast(1),
            FALSE_POSITIVE_PROBABILITY,
        )
        urls.asSequence().filter(String::isNotBlank).forEach(bloomFilter::put)
        return bloomFilter
    }

    private class SourceCache {
        @Volatile
        var bloomFilter: BloomFilter<CharSequence>? = null
        val verifiedSeenUrls: MutableSet<String> = ConcurrentHashMap.newKeySet()
    }

    private const val FALSE_POSITIVE_PROBABILITY = 0.000001
}
