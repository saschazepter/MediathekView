package ca.odell.glazedlists

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock

internal class LockModernizationTest {
    @Test
    fun defaultLockIsReentrant() {
        val list = BasicEventList<String>()
        val writeLock = list.readWriteLock.writeLock()

        writeLock.lock()
        try {
            writeLock.lock()
            try {
                list.add("value")
            } finally {
                writeLock.unlock()
            }
        } finally {
            writeLock.unlock()
        }

        assertEquals(listOf("value"), list)
        assertInstanceOf(ReentrantReadWriteLock::class.java, list.readWriteLock)
    }

    @Test
    fun debugLockDetectsReadToWriteUpgradeThroughJdkLockMethods() {
        val lock = DebugList<String>().getReadWriteLock()
        lock.readLock().lockInterruptibly()
        try {
            assertThrows(IllegalStateException::class.java) {
                lock.writeLock().tryLock(1, TimeUnit.MILLISECONDS)
            }
        } finally {
            lock.readLock().unlock()
        }
    }
}
