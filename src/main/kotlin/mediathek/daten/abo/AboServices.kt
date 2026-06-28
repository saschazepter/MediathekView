package mediathek.daten.abo

import kotlinx.coroutines.*
import mediathek.config.Daten
import mediathek.controller.history.AboHistoryController
import mediathek.daten.ListeAbo
import org.apache.logging.log4j.LogManager
import java.util.concurrent.ExecutionException

class AboServices(daten: Daten) {
    private val historyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var completedAboHistory: AboHistoryController? = null
    private var historyJob: Deferred<Unit>? = null

    val list: ListeAbo = ListeAbo(daten)

    val historyController: AboHistoryController
        get() = completedAboHistory!!

    fun launchHistoryDataLoading() {
        logger.trace("launching async history data loading")
        val loadingJob = historyScope.async {
            completedAboHistory = AboHistoryController()
        }
        loadingJob.invokeOnCompletion { throwable ->
            if (throwable != null) {
                logger.error("launchAboHistoryController", throwable)
            }
        }
        historyJob = loadingJob
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    fun waitForHistoryDataLoadingToComplete() {
        val runningHistoryLoad = historyJob ?: return

        try {
            runBlocking {
                runningHistoryLoad.await()
            }
        } catch (exception: InterruptedException) {
            throw exception
        } catch (exception: Throwable) {
            throw ExecutionException(exception)
        } finally {
            if (historyJob === runningHistoryLoad) {
                historyJob = null
            }
        }
    }

    private companion object {
        private val logger = LogManager.getLogger(AboServices::class.java)
    }
}
