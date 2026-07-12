package ca.odell.glazedlists

import ca.odell.glazedlists.event.ListEvent

open class TransactionList<E> @JvmOverloads constructor(
    source: EventList<E>,
    rollbackEnabled: Boolean = true,
) : TransformedList<E, E>(source) {
    private var rollbackSupport: UndoRedoSupport<E>? =
        if (rollbackEnabled) UndoRedoSupport.install(source) else null
    private val txContextStack = mutableListOf<Context>()

    init {
        rollbackSupport?.addUndoSupportListener { edit ->
            txContextStack.lastOrNull()?.add(edit)
        }
        source.addListEventListener(this)
    }

    fun beginEvent() {
        beginEvent(true)
    }

    fun beginEvent(buffered: Boolean) {
        if (buffered) updates.beginEvent(true)
        txContextStack += Context(buffered)
    }

    fun commitEvent() {
        check(txContextStack.isNotEmpty()) { "No ListEvent exists to commit" }
        txContextStack.removeAt(txContextStack.lastIndex).commit()
    }

    fun rollbackEvent() {
        check(rollbackSupport != null) { "This TransactionList does not support rollback" }
        check(txContextStack.isNotEmpty()) { "No ListEvent exists to roll back" }
        txContextStack.removeAt(txContextStack.lastIndex).rollback()
    }

    fun <R> withTransaction(buffered: Boolean = true, block: TransactionList<E>.() -> R): R {
        beginEvent(buffered)
        return try {
            block().also { commitEvent() }
        } catch (failure: Throwable) {
            try {
                rollbackEvent()
            } catch (rollbackFailure: Throwable) {
                failure.addSuppressed(rollbackFailure)
            }
            throw failure
        }
    }

    override fun isWritable(): Boolean = true

    override fun dispose() {
        rollbackSupport?.uninstall()
        rollbackSupport = null
        txContextStack.clear()
        super.dispose()
    }

    override fun listChanged(listChanges: ListEvent<E>) {
        updates.forwardEvent(listChanges)
    }

    private inner class Context(private val eventStarted: Boolean) {
        private var rollbackEdit: UndoRedoSupport<E>.CompositeEdit? = rollbackSupport?.CompositeEdit()

        fun add(edit: UndoRedoSupport.Edit) {
            rollbackEdit?.add(edit)
        }

        fun commit() {
            rollbackEdit = null
            if (eventStarted) updates.commitEvent()
        }

        fun rollback() {
            rollbackEdit?.let { edit ->
                updates.beginEvent(true)
                try {
                    edit.undo()
                } finally {
                    updates.commitEvent()
                }
                rollbackEdit = null
            }

            if (eventStarted) updates.discardEvent()
        }
    }
}
