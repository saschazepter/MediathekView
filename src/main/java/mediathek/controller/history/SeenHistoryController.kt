package mediathek.controller.history

import mediathek.audiothek.model.AudioEntry
import mediathek.config.Daten
import mediathek.daten.DatenFilm
import mediathek.gui.messages.history.DownloadHistoryChangedEvent
import mediathek.tool.ApplicationConfiguration
import mediathek.tool.MessageBus
import mediathek.tool.sql.SqlDatabaseConfig
import org.apache.logging.log4j.LogManager
import org.sqlite.SQLiteDataSource
import java.nio.file.Files
import java.nio.file.Path
import java.sql.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap


/**
 * Database based seen history controller.
 */
class SeenHistoryController : AutoCloseable {
    private var connection: Connection
    private var insertStatement: PreparedStatement
    private val dataSource: SQLiteDataSource = SqlDatabaseConfig.dataSource
    private var deleteStatement: PreparedStatement
    private var existingUrlProbeClearStatement: PreparedStatement
    private var existingUrlProbeInsertStatement: PreparedStatement
    private var seenStatement: PreparedStatement

    /**
     * Remove all entries from the database.
     */
    fun removeAll() {
        try {
            connection.createStatement().use { stmt -> stmt.executeUpdate("DELETE FROM seen_history") }
            clearPreparedCache()
            sendChangeMessage()
        } catch (ex: SQLException) {
            logger.error("removeAll", ex)
        }
    }

    fun markUnseen(film: DatenFilm) {
        try {
            deleteStatement.setString(1, film.urlNormalQuality)
            deleteStatement.executeUpdate()
            removeFromPreparedCache(film.urlNormalQuality)

            Daten.getInstance().listeBookmarkList.updateSeen(false, film)

            sendChangeMessage()
        } catch (ex: SQLException) {
            logger.error("markUnseen", ex)
        }
    }

    fun markUnseen(list: List<DatenFilm>) {
        try {
            val urls = list
                .asSequence()
                .map { it.urlNormalQuality }
                .filter(String::isNotBlank)
                .distinct()
                .toList()

            if (urls.isNotEmpty()) {
                inTransaction {
                    for (url in urls) {
                        deleteStatement.setString(1, url)
                        deleteStatement.addBatch()
                    }
                    deleteStatement.executeBatch()
                }
                removeFromPreparedCache(urls)
            }

            Daten.getInstance().listeBookmarkList.updateSeen(false, list)

            sendChangeMessage()
        } catch (ex: SQLException) {
            logger.error("markUnseen", ex)
        }
    }

    fun markSeen(film: DatenFilm?) {
        if (film == null) {
            logger.warn("markSeen: no film found")
            return
        }

        val entry = film.toSeenHistoryEntry() ?: return
        if (hasBeenSeenUrl(entry.url)) return
        try {
            writeToDatabase(entry)
            addToPreparedCache(entry.url)
            Daten.getInstance().listeBookmarkList.updateSeen(true, film)

            sendChangeMessage()
        } catch (ex: SQLException) {
            logger.error("markSeen single", ex)
        }
    }

    fun markSeen(list: List<DatenFilm>) {
        try {
            val candidates = list
                .asSequence()
                .mapNotNull { film -> film.toSeenHistoryEntry()?.let { film to it } }
                .distinctBy { (_, entry) -> entry.url }
                .toList()

            if (candidates.isNotEmpty()) {
                val existingUrls = findExistingUrls(candidates.map { (_, entry) -> entry.url })
                val insertedUrls = ArrayList<String>(candidates.size)

                inTransaction {
                    for ((_, entry) in candidates) {
                        if (entry.url in existingUrls) {
                            continue
                        }

                        insertStatement.setString(1, entry.theme)
                        insertStatement.setString(2, entry.title)
                        insertStatement.setString(3, entry.url)
                        insertStatement.addBatch()
                        insertedUrls.add(entry.url)
                    }
                    insertStatement.executeBatch()
                }
                addToPreparedCache(insertedUrls)
            }

            // Update bookmarks with seen information
            Daten.getInstance().listeBookmarkList.updateSeen(true, list)

            //send one change for all...
            sendChangeMessage()
        } catch (ex: SQLException) {
            logger.error("markSeen", ex)
        }
    }

    fun markSeen(entry: AudioEntry?) {
        if (entry == null) {
            logger.warn("markSeen: no audio entry found")
            return
        }

        val historyEntry = entry.toSeenHistoryEntry() ?: return
        if (hasBeenSeenUrl(historyEntry.url)) return
        try {
            writeToDatabase(historyEntry)
            addToPreparedCache(historyEntry.url)
            sendChangeMessage()
        } catch (ex: SQLException) {
            logger.error("markSeen audio", ex)
        }
    }

    fun markUnseen(entry: AudioEntry) {
        val url = entry.audioUrl?.toString().orEmpty()
        if (url.isBlank()) {
            return
        }

        try {
            deleteStatement.setString(1, url)
            deleteStatement.executeUpdate()
            removeFromPreparedCache(url)
            sendChangeMessage()
        } catch (ex: SQLException) {
            logger.error("markUnseen audio", ex)
        }
    }

    /**
     * Load all URLs from database and store in memory.
     */
    fun prepareMemoryCache() {
        synchronized(CACHE_LOCK) {
            if (memCachePrepared) {
                return
            }

            urlCache.clear()
            connection.createStatement().use { st ->
                st.executeQuery("SELECT DISTINCT(url) as url FROM seen_history").use { rs ->
                    while (rs.next()) {
                        val url = rs.getString(1)
                        urlCache.add(url)
                    }
                }
            }

            logger.trace("cache size: {}", urlCache.size)
            memCachePrepared = true
        }
    }

    fun performMaintenance() {
        logger.trace("Start maintenance")

        val config = ApplicationConfiguration.getConfiguration()
        val lastRunStr = config.getString(LASTRUN, null)
        val lastRunDate = if (lastRunStr != null) LocalDate.parse(lastRunStr) else null
        val now = LocalDate.now()
        val shouldRunHeavyMaintenance = lastRunDate == null ||
                ChronoUnit.DAYS.between(lastRunDate, now) >= MAX_DAYS

        try {
            connection.createStatement().use {
                it.executeUpdate("DELETE FROM seen_history WHERE thema = 'Livestream'")
                clearPreparedCache()
                if (shouldRunHeavyMaintenance) {
                    performDatabaseCompact()
                    config.setProperty(LASTRUN, now.toString())
                }
            }
        } catch (e: SQLException) {
            logger.error("Failed to execute maintenance script", e)
        }

        logger.trace("Finished maintenance")
    }

    fun performDatabaseCompact() {
        logger.info("Compacting database")
        connection.createStatement().use {
            it.executeUpdate("REINDEX seen_history")
            it.executeUpdate("VACUUM")
        }
    }

    /**
     * Delete all data in memory cache
     */
    fun emptyMemoryCache() {
        synchronized(CACHE_LOCK) {
            urlCache.clear()
            memCachePrepared = false
        }
    }

    /**
     * Check if film has been seen by using a in-memory cache.
     */
    fun hasBeenSeenFromCache(film: DatenFilm): Boolean {
        if (!memCachePrepared)
            prepareMemoryCache()

        return urlCache.contains(film.urlNormalQuality)
    }

    fun hasBeenSeenFromCache(entry: AudioEntry): Boolean {
        if (!memCachePrepared)
            prepareMemoryCache()

        val url = entry.audioUrl?.toString().orEmpty()
        return url.isNotBlank() && urlCache.contains(url)
    }

    fun hasBeenSeen(film: DatenFilm): Boolean {
        if (memCachePrepared) {
            return urlCache.contains(film.urlNormalQuality)
        }

        return hasBeenSeenUrl(film.urlNormalQuality)
    }

    fun hasBeenSeen(entry: AudioEntry): Boolean {
        val url = entry.audioUrl?.toString().orEmpty()
        if (url.isBlank()) {
            return false
        }

        if (memCachePrepared) {
            return urlCache.contains(url)
        }

        return hasBeenSeenUrl(url)
    }

    private fun hasBeenSeenUrl(url: String): Boolean {
        if (url.isBlank()) {
            return false
        }

        var result: Boolean

        try {
            seenStatement.setString(1, url)
            seenStatement.executeQuery().use {
                it.next()
                val total = it.getInt(1)
                result = total != 0
            }
        } catch (e: SQLException) {
            logger.error("SQL error:", e)
            result = false
        }

        return result
    }

    /**
     * Creates a empty database and all table, indices necessary for use.
     *
     * @throws SQLException Let the caller handle all errors
     */
    @Throws(SQLException::class)
    private fun createEmptyDatabase(dbPath: Path) {
        val dbUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath().toString()
        DriverManager.getConnection(dbUrl, SqlDatabaseConfig.config.toProperties()).use { conn ->
            conn.transactionIsolation = Connection.TRANSACTION_SERIALIZABLE
            conn.createStatement().use { statement ->
                basicSqliteSettings(statement)
                // drop old tables and indices if existent
                statement.executeUpdate(SeenHistoryMigrator.DROP_INDEX_STMT)
                statement.executeUpdate(SeenHistoryMigrator.DROP_TABLE_STMT)
                // create tables and indices
                statement.executeUpdate(SeenHistoryMigrator.CREATE_TABLE_STMT)
                statement.executeUpdate(SeenHistoryMigrator.CREATE_INDEX_STMT)
            }
        }
    }

    private fun basicSqliteSettings(statement: Statement) {
        statement.executeUpdate(SeenHistoryMigrator.PRAGMA_ENCODING_STMT)
        statement.executeUpdate(SeenHistoryMigrator.PRAGMA_PAGE_SIZE)
    }

    /**
     * Write an entry to the database.
     *
     * @param entry the history data to be written.
     * @throws SQLException .
     */
    @Throws(SQLException::class)
    private fun writeToDatabase(entry: SeenHistoryEntry) {
        insertStatement.setString(1, entry.theme)
        insertStatement.setString(2, entry.title)
        insertStatement.setString(3, entry.url)
        // write each entry into database
        insertStatement.executeUpdate()
    }

    private fun addToPreparedCache(url: String) {
        if (!memCachePrepared || url.isBlank()) {
            return
        }
        synchronized(CACHE_LOCK) {
            if (memCachePrepared) {
                urlCache.add(url)
            }
        }
    }

    private fun addToPreparedCache(urls: Collection<String>) {
        if (!memCachePrepared || urls.isEmpty()) {
            return
        }
        synchronized(CACHE_LOCK) {
            if (memCachePrepared) {
                urls.asSequence().filter(String::isNotBlank).forEach(urlCache::add)
            }
        }
    }

    private fun removeFromPreparedCache(url: String) {
        if (!memCachePrepared || url.isBlank()) {
            return
        }
        synchronized(CACHE_LOCK) {
            if (memCachePrepared) {
                urlCache.remove(url)
            }
        }
    }

    private fun removeFromPreparedCache(urls: Collection<String>) {
        if (!memCachePrepared || urls.isEmpty()) {
            return
        }
        synchronized(CACHE_LOCK) {
            if (memCachePrepared) {
                urls.asSequence().filter(String::isNotBlank).forEach(urlCache::remove)
            }
        }
    }

    private fun clearPreparedCache() {
        synchronized(CACHE_LOCK) {
            if (memCachePrepared) {
                urlCache.clear()
                memCachePrepared = false
            }
        }
    }

    @Throws(SQLException::class)
    private fun <T> inTransaction(block: () -> T): T {
        val previousAutoCommit = connection.autoCommit
        connection.autoCommit = false
        try {
            val result = block()
            connection.commit()
            return result
        } catch (ex: SQLException) {
            connection.rollback()
            throw ex
        } finally {
            connection.autoCommit = previousAutoCommit
        }
    }

    @Throws(SQLException::class)
    private fun findExistingUrls(urls: List<String>): Set<String> {
        if (urls.isEmpty()) {
            return emptySet()
        }

        val existingUrls = LinkedHashSet<String>(urls.size)
        val candidates = urls.asSequence().filter(String::isNotBlank).distinct().toList()
        if (candidates.isEmpty()) {
            return emptySet()
        }

        inTransaction {
            existingUrlProbeClearStatement.executeUpdate()

            for (url in candidates) {
                existingUrlProbeInsertStatement.setString(1, url)
                existingUrlProbeInsertStatement.addBatch()
            }
            existingUrlProbeInsertStatement.executeBatch()

            connection.createStatement().use { statement ->
                statement.executeQuery(FIND_EXISTING_URLS_SQL).use { rs ->
                    while (rs.next()) {
                        existingUrls.add(rs.getString(1))
                    }
                }
            }

            existingUrlProbeClearStatement.executeUpdate()
        }
        return existingUrls
    }

    /**
     * Send notification that the number of entries in the history has been changed.
     */
    private fun sendChangeMessage() {
        MessageBus.messageBus.publishAsync(DownloadHistoryChangedEvent())
    }

    override fun close() {
        try {
            insertStatement.close()
            deleteStatement.close()
            existingUrlProbeClearStatement.close()
            existingUrlProbeInsertStatement.close()
            seenStatement.close()
            connection.close()

            // at this stage we have closed everything and we don´t need the shutdown hook to cleanup
            if (shutdownThread != null)
                Runtime.getRuntime().removeShutdownHook(shutdownThread)
        } catch (ex: SQLException) {
            logger.error("close", ex)
        }
    }

    companion object {
        private val logger = LogManager.getLogger()
        private const val INSERT_SQL = "INSERT OR IGNORE INTO seen_history(thema,titel,url) values (?,?,?)"
        private const val DELETE_SQL = "DELETE FROM seen_history WHERE url = ?"
        private const val CREATE_EXISTING_URL_PROBE_TABLE_SQL = "CREATE TEMP TABLE IF NOT EXISTS temp_seen_history_probe(url TEXT PRIMARY KEY)"
        private const val CLEAR_EXISTING_URL_PROBE_SQL = "DELETE FROM temp_seen_history_probe"
        private const val INSERT_EXISTING_URL_PROBE_SQL = "INSERT OR IGNORE INTO temp_seen_history_probe(url) VALUES (?)"
        private const val FIND_EXISTING_URLS_SQL = """
            SELECT sh.url
            FROM seen_history sh
            INNER JOIN temp_seen_history_probe probe ON probe.url = sh.url
        """
        private const val SEEN_SQL = "SELECT COUNT(url) AS total FROM seen_history WHERE url = ?"
        private const val LASTRUN = "database.seen_history.maintenance.lastRun"
        private const val MAX_DAYS: Long = 30
        private val CACHE_LOCK = Any()
        private val urlCache = ConcurrentHashMap.newKeySet<String>()
        @Volatile private var memCachePrepared: Boolean = false
    }

    private fun performSqliteSetup() {
        connection.createStatement().use { statement ->
            basicSqliteSettings(statement)
            val cpus = Runtime.getRuntime().availableProcessors() / 2
            statement.executeUpdate("PRAGMA threads=$cpus")
        }
    }

    @Throws(SQLException::class)
    private fun ensureUniqueUrlIndex() {
        connection.createStatement().use { statement ->
            statement.executeUpdate(SeenHistoryMigrator.DROP_INDEX_STMT)
        }

        try {
            connection.createStatement().use { statement ->
                statement.executeUpdate(SeenHistoryMigrator.CREATE_INDEX_STMT)
            }
        } catch (_: SQLException) {
            logger.info("Removing duplicate seen history entries before creating unique URL index")
            removeDuplicates()
            connection.createStatement().use { statement ->
                statement.executeUpdate(SeenHistoryMigrator.DROP_INDEX_STMT)
                statement.executeUpdate(SeenHistoryMigrator.CREATE_INDEX_STMT)
            }
        }
    }

    private var shutdownThread: SeenHistoryShutdownHook? = null

    /**
     * Close all database connections if they haven´t been closed already.
     * This allows SQLite to perform additional file cleanup like deletion of WAL and shared-memory files.
     */
    private fun installShutdownHook() {
        shutdownThread = SeenHistoryShutdownHook(connection)
        Runtime.getRuntime().addShutdownHook(shutdownThread)
    }

    @Throws(SQLException::class)
    fun removeDuplicates() {
        val prevAutoCommitState = connection.autoCommit
        connection.autoCommit = false
        try {
            connection.createStatement().use { statement ->
                // get all rows with unique urls
                val code = "CREATE TABLE temp_history AS\n" +
                        "SELECT\n" +
                        "    datum,\n" +
                        "    thema,\n" +
                        "    titel,\n" +
                        "    url\n" +
                        "FROM (\n" +
                        "    SELECT\n" +
                        "        datum,\n" +
                        "        thema,\n" +
                        "        titel,\n" +
                        "        url,\n" +
                        "        ROW_NUMBER() OVER (PARTITION BY url ORDER BY datum DESC) as rn\n" +
                        "    FROM\n" +
                        "        seen_history\n" +
                        ") AS ranked_seen_history\n" +
                        "WHERE\n" +
                        "    rn = 1 \n" +
                        "ORDER BY\n" +
                        "    datum;"
                statement.executeUpdate(code)
                statement.executeUpdate("DELETE FROM seen_history")
                statement.executeUpdate("INSERT INTO seen_history(datum,thema,titel,url) SELECT datum,thema,titel,url FROM temp_history")
                statement.executeUpdate("DROP TABLE temp_history")
            }
            connection.commit()
            emptyMemoryCache()
        }
        catch (e: SQLException) {
            connection.rollback()
            throw e
        }
        connection.autoCommit = prevAutoCommitState
    }

    init {
        try {
            if (!Files.exists(SqlDatabaseConfig.historyDbPath)) {
                // create new empty database
                createEmptyDatabase(SqlDatabaseConfig.historyDbPath)
            }

            // open and use database
            connection = dataSource.connection

            performSqliteSetup()
            ensureUniqueUrlIndex()
            connection.createStatement().use { statement ->
                statement.executeUpdate(CREATE_EXISTING_URL_PROBE_TABLE_SQL)
            }

            insertStatement = connection.prepareStatement(INSERT_SQL)
            deleteStatement = connection.prepareStatement(DELETE_SQL)
            existingUrlProbeClearStatement = connection.prepareStatement(CLEAR_EXISTING_URL_PROBE_SQL)
            existingUrlProbeInsertStatement = connection.prepareStatement(INSERT_EXISTING_URL_PROBE_SQL)
            seenStatement = connection.prepareStatement(SEEN_SQL)

            installShutdownHook()
        } catch (ex: SQLException) {
            logger.error("ctor", ex)
            throw IllegalStateException("Could not initialize seen history controller", ex)
        }
    }
}

private data class SeenHistoryEntry(
    val theme: String,
    val title: String,
    val url: String
)

private fun DatenFilm.toSeenHistoryEntry(): SeenHistoryEntry? {
    if (isLivestream) {
        return null
    }

    val url = urlNormalQuality.takeIf(String::isNotBlank) ?: return null
    return SeenHistoryEntry(
        theme = thema,
        title = title,
        url = url
    )
}

private fun AudioEntry.toSeenHistoryEntry(): SeenHistoryEntry? {
    val url = audioUrl?.toString()?.takeIf(String::isNotBlank) ?: return null
    return SeenHistoryEntry(
        theme = theme,
        title = title,
        url = url
    )
}
