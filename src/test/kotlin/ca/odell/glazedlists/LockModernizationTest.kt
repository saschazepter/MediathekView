package ca.odell.glazedlists

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
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
    fun serializationRestoresFreshReentrantLock() {
        val original = BasicEventList<String>().apply { add("value") }
        val serialized = ByteArrayOutputStream().use { bytes ->
            ObjectOutputStream(bytes).use { it.writeObject(original) }
            bytes.toByteArray()
        }

        val restored = ObjectInputStream(ByteArrayInputStream(serialized)).use {
            @Suppress("UNCHECKED_CAST")
            it.readObject() as BasicEventList<String>
        }

        assertEquals(original, restored)
        assertInstanceOf(ReentrantReadWriteLock::class.java, restored.readWriteLock)
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
