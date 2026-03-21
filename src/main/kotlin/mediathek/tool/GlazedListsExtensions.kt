package mediathek.tool

import ca.odell.glazedlists.EventList

inline fun <T> EventList<T>.withWriteLock(action: EventList<T>.() -> Unit) {
    val lock = readWriteLock.writeLock()
    lock.lock()
    try {
        action()
    } finally {
        lock.unlock()
    }
}
