package mediathek.windows

import mediathek.tool.threads.IndicatorThread
import org.apache.logging.log4j.LogManager
import java.awt.Taskbar
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.util.concurrent.TimeUnit
import javax.swing.JFrame

internal class TaskbarIndicatorThread(parent: MediathekGuiWindows) : IndicatorThread() {
    private val taskbar: Taskbar
    private val parent: JFrame
    private val setThreadExecutionState: MethodHandle?

    private fun disableStandby() {
        val res = setThreadExecutionState?.invoke(WinFlags.ES_CONTINUOUS or WinFlags.ES_SYSTEM_REQUIRED) ?: 0
        if (res as Int == 0) {
            logger.error("disableStandby() failed!")
        }
    }

    private fun enableStandby() {
        val res = setThreadExecutionState?.invoke(WinFlags.ES_CONTINUOUS) ?: 0
        if (res as Int == 0) {
            logger.error("enableStandby() failed!")
        }
    }

    override fun run() {
        try {
            while (!isInterrupted) {
                val percentage = calculateOverallPercentage().toInt()
                taskbar.setWindowProgressValue(parent, percentage)
                taskbar.setWindowProgressState(parent, Taskbar.State.NORMAL)
                disableStandby()
                TimeUnit.MILLISECONDS.sleep(500)
            }
        } catch (_: InterruptedException) {
        } finally {
            //when we are finished, stop progress
            taskbar.setWindowProgressState(parent, Taskbar.State.OFF)
            enableStandby()
        }
    }

    companion object {
        private val logger = LogManager.getLogger()
    }

    init {
        name = "TaskbarIndicatorThread"
        taskbar = Taskbar.getTaskbar()
        this.parent = parent

        val linker = Linker.nativeLinker()
        val setThreadExecutionStateMemSeg = linker.defaultLookup().find("SetThreadExecutionState").orElseThrow()
        setThreadExecutionState = linker.downcallHandle(
            setThreadExecutionStateMemSeg,
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
        )
    }
}