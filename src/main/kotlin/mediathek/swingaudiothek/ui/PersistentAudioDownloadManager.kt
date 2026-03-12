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

package mediathek.swingaudiothek.ui

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mediathek.config.StandardLocations
import mediathek.swingaudiothek.model.AudioEntry
import okhttp3.*
import org.apache.logging.log4j.LogManager
import java.io.InputStream
import java.net.SocketException
import java.net.URI
import java.nio.file.*
import java.util.*

class PersistentAudioDownloadManager(
    private val httpClient: OkHttpClient,
    private val onDownloadCompleted: (AudioDownloadTaskSnapshot) -> Unit
) {
    private val logger = LogManager.getLogger(PersistentAudioDownloadManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tasks = linkedMapOf<String, ManagedTask>()
    private val listeners = mutableListOf<(List<AudioDownloadTaskSnapshot>) -> Unit>()
    private val storePath = StandardLocations.getSettingsDirectory().resolve("audiothek-downloads.json")
    private var lastPersistNanos = 0L
    private var lastNotifyNanos = 0L

    init {
        loadPersistedTasks()
    }

    fun addListener(listener: (List<AudioDownloadTaskSnapshot>) -> Unit) {
        listeners += listener
        listener(tasks.values.map { it.snapshot })
    }

    fun snapshot(taskId: String): AudioDownloadTaskSnapshot? {
        return tasks[taskId]?.snapshot
    }

    fun enqueue(entry: AudioEntry, targetFile: Path) {
        val task = ManagedTask(
            snapshot = AudioDownloadTaskSnapshot(
                id = UUID.randomUUID().toString(),
                audioName = entry.title.ifBlank { "(ohne Titel)" },
                channel = entry.channel,
                theme = entry.theme,
                audioUrl = entry.audioUrl?.toString().orEmpty(),
                targetFile = targetFile.toString(),
                tempFile = buildTempFilePath(targetFile).toString(),
                state = AudioDownloadTaskState.PAUSED
            )
        )
        tasks[task.snapshot.id] = task
        persistAndNotify(force = true)
        resume(task.snapshot.id)
    }

    fun resume(taskId: String) {
        val task = tasks[taskId] ?: return
        if (task.snapshot.audioUrl.isBlank() || task.job?.isActive == true) {
            return
        }

        task.stopMode = StopMode.NONE
        task.snapshot = task.snapshot.copy(
            state = AudioDownloadTaskState.DOWNLOADING,
            errorMessage = null
        )
        persistAndNotify(force = true)

        task.job = scope.launch {
            try {
                runDownload(task)
            } catch (_: CancellationException) {
                when (task.stopMode) {
                    StopMode.PAUSE, StopMode.SHUTDOWN -> updateTask(
                        task,
                        task.snapshot.copy(state = AudioDownloadTaskState.PAUSED)
                    )
                    StopMode.CANCEL -> {
                        deleteIfExists(Path.of(task.snapshot.tempFile))
                        updateTask(task, task.snapshot.copy(state = AudioDownloadTaskState.CANCELLED))
                    }
                    StopMode.NONE -> updateTask(task, task.snapshot.copy(state = AudioDownloadTaskState.PAUSED))
                }
            } catch (ex: Exception) {
                if (isExpectedStop(task, ex)) {
                    handleExpectedStop(task)
                } else {
                    logger.warn("Audio download failed for {}", task.snapshot.audioUrl, ex)
                    updateTask(
                        task,
                        task.snapshot.copy(
                            state = AudioDownloadTaskState.FAILED,
                            errorMessage = ex.message ?: ex::class.java.simpleName
                        )
                    )
                }
            } finally {
                task.activeCall = null
                task.job = null
                task.stopMode = StopMode.NONE
            }
        }
    }

    fun pause(taskId: String) {
        val task = tasks[taskId] ?: return
        task.stopMode = StopMode.PAUSE
        task.activeCall?.cancel()
        task.job?.cancel()
        if (task.job == null) {
            updateTask(task, task.snapshot.copy(state = AudioDownloadTaskState.PAUSED))
        }
    }

    fun cancel(taskId: String) {
        val task = tasks[taskId] ?: return
        task.stopMode = StopMode.CANCEL
        task.activeCall?.cancel()
        task.job?.cancel()
        if (task.job == null) {
            deleteIfExists(Path.of(task.snapshot.tempFile))
            updateTask(
                task,
                task.snapshot.copy(
                    state = AudioDownloadTaskState.CANCELLED,
                    downloadedBytes = 0L,
                    totalBytes = null,
                    errorMessage = null
                )
            )
        }
    }

    fun remove(taskId: String) {
        val task = tasks[taskId] ?: return
        if (task.job?.isActive == true) {
            return
        }
        deleteIfExists(Path.of(task.snapshot.tempFile))
        tasks.remove(taskId)
        persistAndNotify(force = true)
    }

    suspend fun shutdown() {
        for (task in tasks.values) {
            if (task.job?.isActive == true) {
                task.stopMode = StopMode.SHUTDOWN
                task.activeCall?.cancel()
                task.job?.cancel()
            } else if (task.snapshot.state == AudioDownloadTaskState.DOWNLOADING) {
                task.snapshot = task.snapshot.copy(state = AudioDownloadTaskState.PAUSED)
            }
        }
        persistAndNotify(force = true)
        for (task in tasks.values) {
            task.job?.cancelAndJoin()
        }
    }

    private suspend fun runDownload(task: ManagedTask) {
        val snapshot = task.snapshot
        val audioUrl = URI.create(snapshot.audioUrl)
        val targetFile = Path.of(snapshot.targetFile)
        val tempFile = Path.of(snapshot.tempFile)
        Files.createDirectories(targetFile.parent ?: Path.of("."))

        val existingBytes = sizeIfExists(tempFile)?.coerceAtLeast(0L) ?: 0L
        if (existingBytes != snapshot.downloadedBytes) {
            updateTask(task, snapshot.copy(downloadedBytes = existingBytes))
        }

        executeDownload(task, audioUrl.toString(), tempFile, targetFile)
    }

    private suspend fun executeDownload(task: ManagedTask, url: String, tempFile: Path, targetFile: Path) {
        val downloadedBytes = task.snapshot.downloadedBytes
        val request = Request.Builder()
            .url(url)
            .get()
            .apply {
                if (downloadedBytes > 0L) {
                    header("Range", "bytes=$downloadedBytes-")
                }
            }
            .build()

        val call = httpClient.newCall(request)
        task.activeCall = call
        call.execute().use { response ->
            when {
                response.code == 416 && downloadedBytes > 0L -> {
                    deleteIfExists(tempFile)
                    updateTask(task, task.snapshot.copy(downloadedBytes = 0L, totalBytes = null))
                    executeDownload(task, url, tempFile, targetFile)
                }
                response.isSuccessful && response.body != null -> {
                    if (response.code == 200 && downloadedBytes > 0L) {
                        deleteIfExists(tempFile)
                        updateTask(task, task.snapshot.copy(downloadedBytes = 0L, totalBytes = determineTotalBytes(response, 0L)))
                        executeDownload(task, url, tempFile, targetFile)
                        return
                    }
                    streamResponseToFile(task, response, response.body!!, tempFile)
                    moveDownloadedFile(tempFile, targetFile)
                    updateTask(
                        task,
                        task.snapshot.copy(
                            state = AudioDownloadTaskState.COMPLETED,
                            downloadedBytes = task.snapshot.totalBytes ?: task.snapshot.downloadedBytes,
                            errorMessage = null
                        )
                    )
                    onDownloadCompleted(task.snapshot)
                }
                else -> error("Download fehlgeschlagen: HTTP ${response.code}")
            }
        }
    }

    private fun streamResponseToFile(task: ManagedTask, response: Response, body: ResponseBody, tempFile: Path) {
        val existingBytes = task.snapshot.downloadedBytes
        val totalBytes = determineTotalBytes(response, existingBytes)
        updateTask(task, task.snapshot.copy(totalBytes = totalBytes))

        val options = if (existingBytes > 0L) {
            arrayOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)
        } else {
            arrayOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
        }

        body.byteStream().use { input ->
            Files.newOutputStream(tempFile, *options).buffered().use { output ->
                copyStream(task, input, output, existingBytes, totalBytes)
            }
        }
    }

    private fun copyStream(
        task: ManagedTask,
        input: InputStream,
        output: java.io.OutputStream,
        startBytes: Long,
        totalBytes: Long?
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var downloadedBytes = startBytes
        while (true) {
            val read = input.read(buffer)
            if (read < 0) {
                break
            }
            output.write(buffer, 0, read)
            downloadedBytes += read
            updateTask(
                task,
                task.snapshot.copy(
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes
                ),
                persist = shouldPersistProgress()
            )
        }
    }

    private fun determineTotalBytes(response: Response, downloadedBytes: Long): Long? {
        response.header("Content-Range")
            ?.substringAfterLast('/')
            ?.toLongOrNull()
            ?.let { return it }
        val contentLength = response.body?.contentLength()?.takeIf { it >= 0L } ?: return null
        return if (response.code == 206) {
            downloadedBytes + contentLength
        } else {
            contentLength
        }
    }

    private fun updateTask(task: ManagedTask, snapshot: AudioDownloadTaskSnapshot, persist: Boolean = true) {
        task.snapshot = snapshot
        persistAndNotify(force = persist)
    }

    private fun isExpectedStop(task: ManagedTask, exception: Exception): Boolean {
        if (task.stopMode == StopMode.NONE) {
            return false
        }
        return exception is SocketException || exception is java.io.InterruptedIOException
    }

    private fun handleExpectedStop(task: ManagedTask) {
        when (task.stopMode) {
            StopMode.PAUSE, StopMode.SHUTDOWN -> updateTask(
                task,
                task.snapshot.copy(state = AudioDownloadTaskState.PAUSED)
            )
            StopMode.CANCEL -> {
                deleteIfExists(Path.of(task.snapshot.tempFile))
                updateTask(
                    task,
                    task.snapshot.copy(
                        state = AudioDownloadTaskState.CANCELLED,
                        downloadedBytes = 0L,
                        totalBytes = null,
                        errorMessage = null
                    )
                )
            }
            StopMode.NONE -> Unit
        }
    }

    private fun persistAndNotify(force: Boolean) {
        if (force || shouldPersistProgress()) {
            persist()
        }
        if (force || shouldNotifyProgress()) {
            val snapshots = tasks.values.map { it.snapshot }
            listeners.forEach { it(snapshots) }
        }
    }

    private fun shouldPersistProgress(): Boolean {
        val now = System.nanoTime()
        if (now - lastPersistNanos >= 1_000_000_000L) {
            lastPersistNanos = now
            return true
        }
        return false
    }

    private fun shouldNotifyProgress(): Boolean {
        val now = System.nanoTime()
        if (now - lastNotifyNanos >= 250_000_000L) {
            lastNotifyNanos = now
            return true
        }
        return false
    }

    private fun persist() {
        runCatching {
            Files.createDirectories(storePath.parent)
            Files.writeString(storePath, JSON.encodeToString(tasks.values.map { it.snapshot }))
        }.onFailure {
            logger.warn("Failed to persist audiothek downloads", it)
        }
    }

    private fun loadPersistedTasks() {
        if (!Files.exists(storePath)) {
            return
        }

        val snapshots = runCatching {
            JSON.decodeFromString<List<AudioDownloadTaskSnapshot>>(Files.readString(storePath))
        }.getOrElse {
            logger.warn("Failed to read persisted audiothek downloads", it)
            emptyList()
        }

        snapshots
            .asSequence()
            .filterNot { it.state == AudioDownloadTaskState.COMPLETED }
            .forEach { snapshot ->
            val restoredState = when (snapshot.state) {
                AudioDownloadTaskState.DOWNLOADING -> AudioDownloadTaskState.PAUSED
                else -> snapshot.state
            }
            tasks[snapshot.id] = ManagedTask(snapshot.copy(state = restoredState))
            }
        persistAndNotify(force = true)
    }

    private fun buildTempFilePath(targetFile: Path): Path {
        return targetFile.resolveSibling(targetFile.fileName.toString() + ".audiothek.part")
    }

    private fun deleteIfExists(path: Path) {
        runCatching { Files.deleteIfExists(path) }
            .onFailure { logger.debug("Failed to delete {}", path, it) }
    }

    private fun moveDownloadedFile(tempFile: Path, targetFile: Path) {
        try {
            Files.move(
                tempFile,
                targetFile,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private data class ManagedTask(
        var snapshot: AudioDownloadTaskSnapshot,
        var job: Job? = null,
        var activeCall: Call? = null,
        var stopMode: StopMode = StopMode.NONE
    )

    private enum class StopMode {
        NONE,
        PAUSE,
        CANCEL,
        SHUTDOWN
    }

    companion object {
        private val JSON = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }
}

@Serializable
data class AudioDownloadTaskSnapshot(
    val id: String,
    val audioName: String,
    val channel: String,
    val theme: String,
    val audioUrl: String,
    val targetFile: String,
    val tempFile: String,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val state: AudioDownloadTaskState,
    val errorMessage: String? = null
)

@Serializable
enum class AudioDownloadTaskState {
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    CANCELLED,
    FAILED
}

private fun sizeIfExists(path: Path): Long? {
    return if (Files.exists(path)) Files.size(path) else null
}
