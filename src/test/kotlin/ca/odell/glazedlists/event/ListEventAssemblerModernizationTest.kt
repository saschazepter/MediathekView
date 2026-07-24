package ca.odell.glazedlists.event

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.EventList
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal class ListEventAssemblerModernizationTest {
    @Test
    fun listEventConstructionDoesNotUseReflection() {
        val classResource = "/${ListEventAssembler::class.java.name.replace('.', '/')}.class"
        val bytecode =
            requireNotNull(ListEventAssembler::class.java.getResourceAsStream(classResource))
                .use { String(it.readBytes(), Charsets.ISO_8859_1) }

        assertFalse(bytecode.contains("java/lang/Class"))
        assertFalse(bytecode.contains("java/lang/reflect/Constructor"))
    }

    @Test
    fun classAbiRetainsJavaBaselineShapeAndSignatures() {
        val assemblerClass = ListEventAssembler::class.java
        val declaredFields = assemblerClass.declaredFields.associateBy { it.name }
        val declaredMethods = assemblerClass.declaredMethods.associateBy { signatureOf(it) }

        assertTrue(Modifier.isPublic(assemblerClass.modifiers))
        assertTrue(Modifier.isFinal(assemblerClass.modifiers))
        assertFalse(Modifier.isAbstract(assemblerClass.modifiers))
        assertEquals(listOf("E"), assemblerClass.typeParameters.map { it.name })
        assertEquals(1, assemblerClass.declaredConstructors.size)
        assertEquals(
            setOf(
                "Companion",
                "sourceList",
                "eventThread",
                "eventLevel",
                "allowNestedEvents",
                "reorderMap",
                "blockSequence",
                "useListBlocksLinear",
                "listDeltas",
                "publisher",
                "listEvent",
                "eventFormat",
                "eventIsBeingPublished",
            ),
            declaredFields.keys,
        )
        assertEquals(13, declaredFields.size)
        assertEquals(
            setOf(
                "isEventInProgress()Z",
                "beginEvent()V",
                "beginEvent(Z)V",
                "elementInserted(ILjava/lang/Object;)V",
                "elementUpdated(ILjava/lang/Object;Ljava/lang/Object;)V",
                "elementDeleted(ILjava/lang/Object;)V",
                "elementsInserted(II)V",
                "elementsUpdated(II)V",
                "elementsDeleted(II)V",
                "addChange(IIILjava/lang/Object;Ljava/lang/Object;)V",
                "reorder([I)V",
                "forwardEvent(Lca/odell/glazedlists/event/ListEvent;)V",
                "commitEvent()V",
                "discardEvent()V",
                "isEventEmpty()Z",
                "addListEventListener(Lca/odell/glazedlists/event/ListEventListener;)V",
                "removeListEventListener(Lca/odell/glazedlists/event/ListEventListener;)V",
                "getListEventListeners()Ljava/util/List;",
                "getUseListBlocksLinear()Z",
                "getListDeltas()Lca/odell/glazedlists/impl/event/Tree4Deltas;",
                "getListBlocksLinear()Lca/odell/glazedlists/impl/event/BlockSequence;",
                "getReorderMap()[I",
                "cleanup()V",
                "access${DOLLAR}cleanup(Lca/odell/glazedlists/event/ListEventAssembler;)V",
                "access${DOLLAR}setEventIsBeingPublished${DOLLAR}p(Lca/odell/glazedlists/event/ListEventAssembler;Z)V",
            ),
            declaredMethods.keys,
        )
        assertEquals(25, declaredMethods.size)

        val constructor = assemblerClass.getDeclaredConstructor(EventList::class.java, ListEventPublisher::class.java)
        assertTrue(Modifier.isPublic(constructor.modifiers))
        assertEquals(
            "(Lca/odell/glazedlists/EventList;Lca/odell/glazedlists/event/ListEventPublisher;)V",
            descriptorOf(constructor),
        )
        assertEquals(
            listOf("ca.odell.glazedlists.EventList<E>", "ca.odell.glazedlists.event.ListEventPublisher"),
            constructor.genericParameterTypes.map { it.typeName },
        )

        assertField(
            declaredFields.getValue("sourceList"),
            expectedDescriptor = "Lca/odell/glazedlists/EventList;",
            expectedGenericType = "ca.odell.glazedlists.EventList<E>",
            public = false,
            protected = true,
        )
        assertField(
            declaredFields.getValue("eventThread"),
            expectedDescriptor = "Ljava/lang/Thread;",
            private = true,
        )
        assertField(
            declaredFields.getValue("eventLevel"),
            expectedDescriptor = "I",
            public = false,
            protected = true,
        )
        assertField(
            declaredFields.getValue("allowNestedEvents"),
            expectedDescriptor = "Z",
            public = false,
            protected = true,
        )
        assertField(
            declaredFields.getValue("reorderMap"),
            expectedDescriptor = "[I",
            public = false,
            protected = true,
        )
        assertField(
            declaredFields.getValue("blockSequence"),
            expectedDescriptor = "Lca/odell/glazedlists/impl/event/BlockSequence;",
            expectedGenericType = "ca.odell.glazedlists.impl.event.BlockSequence<E>",
            private = true,
            final = true,
        )
        assertField(
            declaredFields.getValue("useListBlocksLinear"),
            expectedDescriptor = "Z",
            private = true,
        )
        assertField(
            declaredFields.getValue("listDeltas"),
            expectedDescriptor = "Lca/odell/glazedlists/impl/event/Tree4Deltas;",
            expectedGenericType = "ca.odell.glazedlists.impl.event.Tree4Deltas<E>",
            private = true,
            final = true,
        )
        assertField(
            declaredFields.getValue("publisher"),
            expectedDescriptor = "Lca/odell/glazedlists/event/SequenceDependenciesEventPublisher;",
            private = true,
            final = true,
        )
        assertField(
            declaredFields.getValue("listEvent"),
            expectedDescriptor = "Lca/odell/glazedlists/event/ListEvent;",
            expectedGenericType = "ca.odell.glazedlists.event.ListEvent<E>",
            private = true,
            final = true,
        )
        assertField(
            declaredFields.getValue("eventFormat"),
            expectedDescriptor = "Lca/odell/glazedlists/event/ListEventAssembler${DOLLAR}ListEventFormat;",
            expectedGenericType = "ca.odell.glazedlists.event.ListEventAssembler<E>${DOLLAR}ListEventFormat",
            private = true,
            final = true,
        )
        assertField(
            declaredFields.getValue("eventIsBeingPublished"),
            expectedDescriptor = "Z",
            private = true,
        )

        assertMethod(
            ListEventAssembler.Companion::class.java.getDeclaredMethod("createListEventPublisher"),
            expectedDescriptor = "()Lca/odell/glazedlists/event/ListEventPublisher;",
            expectedGenericReturnType = "ca.odell.glazedlists.event.ListEventPublisher",
            public = true,
            final = true,
        )
        assertMethod(
            declaredMethods.getValue("isEventInProgress()Z"),
            expectedDescriptor = "()Z",
            expectedGenericReturnType = "boolean",
            public = true,
            final = true,
        )
        assertMethod(
            declaredMethods.getValue("beginEvent()V"),
            expectedDescriptor = "()V",
            expectedGenericReturnType = "void",
            public = true,
            final = true,
        )
        assertMethod(
            declaredMethods.getValue("beginEvent(Z)V"),
            expectedDescriptor = "(Z)V",
            expectedGenericReturnType = "void",
            public = true,
            final = true,
            synchronized = true,
        )
        assertMethod(
            declaredMethods.getValue("elementInserted(ILjava/lang/Object;)V"),
            expectedDescriptor = "(ILjava/lang/Object;)V",
            expectedGenericReturnType = "void",
            expectedGenericParameters = listOf("int", "E"),
            public = true,
            final = true,
        )
        assertMethod(
            declaredMethods.getValue("elementUpdated(ILjava/lang/Object;Ljava/lang/Object;)V"),
            expectedDescriptor = "(ILjava/lang/Object;Ljava/lang/Object;)V",
            expectedGenericReturnType = "void",
            expectedGenericParameters = listOf("int", "E", "E"),
            public = true,
            final = true,
        )
        assertMethod(
            declaredMethods.getValue("elementDeleted(ILjava/lang/Object;)V"),
            expectedDescriptor = "(ILjava/lang/Object;)V",
            expectedGenericReturnType = "void",
            expectedGenericParameters = listOf("int", "E"),
            public = true,
            final = true,
        )
        assertMethod(
            declaredMethods.getValue("elementsInserted(II)V"),
            expectedDescriptor = "(II)V",
            expectedGenericReturnType = "void",
            expectedGenericParameters = listOf("int", "int"),
            public = true,
            final = true,
        )
        assertMethod(
            declaredMethods.getValue("elementsUpdated(II)V"),
            expectedDescriptor = "(II)V",
            expectedGenericReturnType = "void",
            expectedGenericParameters = listOf("int", "int"),
            public = true,
            final = true,
        )
        assertMethod(
            declaredMethods.getValue("elementsDeleted(II)V"),
            expectedDescriptor = "(II)V",
            expectedGenericReturnType = "void",
            expectedGenericParameters = listOf("int", "int"),
            public = true,
            final = true,
        )
        assertMethod(
            declaredMethods.getValue("addChange(IIILjava/lang/Object;Ljava/lang/Object;)V"),
            expectedDescriptor = "(IIILjava/lang/Object;Ljava/lang/Object;)V",
            expectedGenericReturnType = "void",
            expectedGenericParameters = listOf("int", "int", "int", "E", "E"),
            private = true,
            final = true,
        )
        assertMethod(
            declaredMethods.getValue("reorder([I)V"),
            expectedDescriptor = "([I)V",
            expectedGenericReturnType = "void",
            expectedGenericParameters = listOf("int[]"),
            public = true,
            final = true,
        )
        assertMethod(
            declaredMethods.getValue("forwardEvent(Lca/odell/glazedlists/event/ListEvent;)V"),
            expectedDescriptor = "(Lca/odell/glazedlists/event/ListEvent;)V",
            expectedGenericReturnType = "void",
            expectedGenericParameters = listOf("ca.odell.glazedlists.event.ListEvent<?>"),
            public = true,
            final = true,
        )
        assertMethod(
            declaredMethods.getValue("commitEvent()V"),
            expectedDescriptor = "()V",
            expectedGenericReturnType = "void",
            public = true,
            final = true,
            synchronized = true,
        )
        assertMethod(
            declaredMethods.getValue("discardEvent()V"),
            expectedDescriptor = "()V",
            expectedGenericReturnType = "void",
            public = true,
            final = true,
            synchronized = true,
        )
        assertMethod(
            declaredMethods.getValue("isEventEmpty()Z"),
            expectedDescriptor = "()Z",
            expectedGenericReturnType = "boolean",
            public = true,
            final = true,
        )
        assertMethod(
            declaredMethods.getValue("addListEventListener(Lca/odell/glazedlists/event/ListEventListener;)V"),
            expectedDescriptor = "(Lca/odell/glazedlists/event/ListEventListener;)V",
            expectedGenericReturnType = "void",
            expectedGenericParameters = listOf("ca.odell.glazedlists.event.ListEventListener<? super E>"),
            public = true,
            final = true,
            synchronized = true,
        )
        assertMethod(
            declaredMethods.getValue("removeListEventListener(Lca/odell/glazedlists/event/ListEventListener;)V"),
            expectedDescriptor = "(Lca/odell/glazedlists/event/ListEventListener;)V",
            expectedGenericReturnType = "void",
            expectedGenericParameters = listOf("ca.odell.glazedlists.event.ListEventListener<? super E>"),
            public = true,
            final = true,
            synchronized = true,
        )
        assertMethod(
            declaredMethods.getValue("getListEventListeners()Ljava/util/List;"),
            expectedDescriptor = "()Ljava/util/List;",
            expectedGenericReturnType = "java.util.List<ca.odell.glazedlists.event.ListEventListener<E>>",
            public = true,
            final = true,
        )
        assertMethod(
            declaredMethods.getValue("getUseListBlocksLinear()Z"),
            expectedDescriptor = "()Z",
            expectedGenericReturnType = "boolean",
            public = true,
            final = true,
            synthetic = true,
        )
        assertMethod(
            declaredMethods.getValue("getListDeltas()Lca/odell/glazedlists/impl/event/Tree4Deltas;"),
            expectedDescriptor = "()Lca/odell/glazedlists/impl/event/Tree4Deltas;",
            expectedGenericReturnType = "ca.odell.glazedlists.impl.event.Tree4Deltas",
            public = true,
            final = true,
            synthetic = true,
        )
        assertMethod(
            declaredMethods.getValue("getListBlocksLinear()Lca/odell/glazedlists/impl/event/BlockSequence;"),
            expectedDescriptor = "()Lca/odell/glazedlists/impl/event/BlockSequence;",
            expectedGenericReturnType = "ca.odell.glazedlists.impl.event.BlockSequence",
            public = true,
            final = true,
            synthetic = true,
        )
        assertMethod(
            declaredMethods.getValue("getReorderMap()[I"),
            expectedDescriptor = "()[I",
            expectedGenericReturnType = "int[]",
            public = true,
            final = true,
            synthetic = true,
        )
        assertMethod(
            declaredMethods.getValue("cleanup()V"),
            expectedDescriptor = "()V",
            expectedGenericReturnType = "void",
            private = true,
            final = true,
        )

        assertField(
            declaredFields.getValue("Companion"),
            expectedDescriptor = "Lca/odell/glazedlists/event/ListEventAssembler${DOLLAR}Companion;",
            public = true,
            static = true,
            final = true,
        )
        assertMethod(
            declaredMethods.getValue("access${DOLLAR}cleanup(Lca/odell/glazedlists/event/ListEventAssembler;)V"),
            expectedDescriptor = "(Lca/odell/glazedlists/event/ListEventAssembler;)V",
            expectedGenericReturnType = "void",
            expectedGenericParameters = listOf("ca.odell.glazedlists.event.ListEventAssembler"),
            public = true,
            static = true,
            final = true,
            synthetic = true,
        )
        assertMethod(
            declaredMethods.getValue("access${DOLLAR}setEventIsBeingPublished${DOLLAR}p(Lca/odell/glazedlists/event/ListEventAssembler;Z)V"),
            expectedDescriptor = "(Lca/odell/glazedlists/event/ListEventAssembler;Z)V",
            expectedGenericReturnType = "void",
            expectedGenericParameters = listOf("ca.odell.glazedlists.event.ListEventAssembler", "boolean"),
            public = true,
            static = true,
            final = true,
            synthetic = true,
        )
    }

    @Test
    fun constructorRetainsSourceAndPublisherIdentity() {
        val source = BasicEventList<String>()
        val publisher = ListEventAssembler.createListEventPublisher()
        val secondPublisher = ListEventAssembler.createListEventPublisher()
        val assembler = ListEventAssembler(source, publisher)

        assertTrue(publisher is SequenceDependenciesEventPublisher)
        assertTrue(secondPublisher is SequenceDependenciesEventPublisher)
        assertNotSame(publisher, secondPublisher)
        assertSame(source, fieldValue<EventList<*>>(assembler, "sourceList"))
        assertSame(publisher, fieldValue(assembler, "publisher"))
        assertSame(source, fieldValue<ListEvent<*>>(assembler, "listEvent").sourceList)

    }

    @Test
    fun constructorRejectsForeignPublisher() {
        val source = BasicEventList<String>()

        val foreignPublisherFailure = assertThrows(ClassCastException::class.java) {
            ListEventAssembler(source, ForeignListEventPublisher())
        }
        assertTrue(foreignPublisherFailure.message!!.contains("SequenceDependenciesEventPublisher"))
    }

    @Test
    fun kotlinCompilerArtifactsRemainSyntheticOrEffectivelyPrivate() {
        val assemblerClass = ListEventAssembler::class.java
        val facadeClass = Class.forName("ca.odell.glazedlists.event.ListEventAssemblerKt")
        assertTrue(Modifier.isPublic(facadeClass.modifiers))
        assertTrue(Modifier.isFinal(facadeClass.modifiers))

        val facadeMethods = facadeClass.declaredMethods.associateBy { it.name + descriptorOf(it) }
        assertEquals(
            setOf(
                "isEventInProgress(Lca/odell/glazedlists/event/ListEventAssembler;)Z",
                "isEventEmpty(Lca/odell/glazedlists/event/ListEventAssembler;)Z",
                "getUseListBlocksLinear(Lca/odell/glazedlists/event/ListEventAssembler;)Z",
                "getListDeltas(Lca/odell/glazedlists/event/ListEventAssembler;)Lca/odell/glazedlists/impl/event/Tree4Deltas;",
                "getListBlocksLinear(Lca/odell/glazedlists/event/ListEventAssembler;)Lca/odell/glazedlists/impl/event/BlockSequence;",
                "getReorderMap(Lca/odell/glazedlists/event/ListEventAssembler;)[I",
            ),
            facadeMethods.keys,
        )
        facadeMethods.values.forEach { method ->
            assertTrue(Modifier.isPublic(method.modifiers), method.name)
            assertTrue(Modifier.isStatic(method.modifiers), method.name)
            assertTrue(Modifier.isFinal(method.modifiers), method.name)
            assertTrue(method.isSynthetic, method.name)
        }

        val companionClass = assemblerClass.declaredClasses.single { it.simpleName == "Companion" }
        assertTrue(Modifier.isPublic(companionClass.modifiers))
        assertTrue(Modifier.isStatic(companionClass.modifiers))
        assertTrue(Modifier.isFinal(companionClass.modifiers))
        val companionFactory = companionClass.getDeclaredMethod("createListEventPublisher")
        assertTrue(Modifier.isPublic(companionFactory.modifiers))
        assertTrue(Modifier.isFinal(companionFactory.modifiers))
        assertFalse(Modifier.isStatic(companionFactory.modifiers))
        val companionConstructors = companionClass.declaredConstructors
        assertEquals(2, companionConstructors.size)
        assertTrue(companionConstructors.any { Modifier.isPrivate(it.modifiers) && !it.isSynthetic })
        assertTrue(companionConstructors.any { Modifier.isPublic(it.modifiers) && it.isSynthetic })

        val eventFormatClass = assemblerClass.declaredClasses.single { it.simpleName == "ListEventFormat" }
        assertTrue(Modifier.isPrivate(eventFormatClass.modifiers))
        assertTrue(Modifier.isFinal(eventFormatClass.modifiers))
        val eventFormatConstructor = eventFormatClass.declaredConstructors.single()
        assertTrue(Modifier.isPublic(eventFormatConstructor.modifiers))
        assertFalse(eventFormatConstructor.isSynthetic)
        assertEquals(
            listOf(assemblerClass),
            eventFormatConstructor.parameterTypes.toList(),
        )
    }

    @Test
    fun equalButDistinctListenersRemainDistinctAndDuplicateRemovalUsesIdentity() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)
        val trace = mutableListOf<String>()
        val first = EqualButDistinctListener("same", "first", trace)
        val second = EqualButDistinctListener("same", "second", trace)
        val equalButUnregistered = EqualButDistinctListener("same", "ghost", trace)

        assembler.addListEventListener(first)
        assembler.addListEventListener(first)
        assembler.addListEventListener(second)

        assertListenerSnapshot(assembler, first, first, second)
        publishInsert(assembler, Any())
        assertEquals(listOf("first", "first", "second"), trace)

        assembler.removeListEventListener(equalButUnregistered)
        assertListenerSnapshot(assembler, first, first, second)

        trace.clear()
        assembler.removeListEventListener(first)
        publishInsert(assembler, Any())
        assertEquals(listOf("first", "second"), trace)
        assertListenerSnapshot(assembler, first, second)
    }

    @Test
    fun listenersAddedDuringDispatchObserveOnlySubsequentEvents() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)
        val trace = mutableListOf<String>()
        val lateListener = ListEventListener<Any> { trace += "late" }
        var registered = false
        val initialListener =
            ListEventListener<Any> {
                trace += "initial"
                if (!registered) {
                    assembler.addListEventListener(lateListener)
                    registered = true
                }
            }

        assembler.addListEventListener(initialListener)

        publishInsert(assembler, Any())
        publishInsert(assembler, Any())

        assertEquals(listOf("initial", "initial", "late"), trace)
    }

    @Test
    fun listenersRemovedDuringDispatchStillReceiveTheCurrentEventAndSelfRemovalOnlyAffectsFutureEvents() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)
        val trace = mutableListOf<String>()
        val lateListener = ListEventListener<Any> { trace += "late" }
        lateinit var selfRemovingListener: ListEventListener<Any>
        selfRemovingListener =
            ListEventListener {
                trace += "self"
                assembler.removeListEventListener(selfRemovingListener)
            }
        val initialListener =
            ListEventListener<Any> {
                trace += "initial"
                assembler.removeListEventListener(lateListener)
            }

        assembler.addListEventListener(initialListener)
        assembler.addListEventListener(selfRemovingListener)
        assembler.addListEventListener(lateListener)

        publishInsert(assembler, Any())
        publishInsert(assembler, Any())

        assertEquals(listOf("initial", "self", "late", "initial"), trace)
        assertListenerSnapshot(assembler, initialListener)
    }

    @Test
    fun beginEventWithoutNestingRejectsASecondBeginWithTheCurrentThreadName() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)

        assembler.beginEvent()
        val failure = assertThrows(ConcurrentModificationException::class.java) {
            assembler.beginEvent()
        }

        assertEquals(
            "Cannot begin a new event while another event is in progress by thread, ${Thread.currentThread().name}",
            failure.message,
        )
        assertTrue(assembler.isEventInProgress)

        assembler.discardEvent()
        assertFalse(assembler.isEventInProgress)
        assertDoesNotThrow {
            assembler.beginEvent()
            assembler.discardEvent()
        }
    }

    @Test
    fun outerAllowNestedEventKeepsContradictionModeEnabledEvenWhenInnerScopeUsesFalse() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)
        var notifications = 0
        assembler.addListEventListener { notifications++ }

        assembler.beginEvent(true)
        assembler.beginEvent(false)
        val inserted = Any()
        assembler.elementInserted(0, inserted)
        assembler.elementDeleted(0, inserted)
        assembler.commitEvent()
        assertTrue(assembler.isEventInProgress)
        assertEquals(0, notifications)
        assembler.commitEvent()

        assertEquals(0, notifications)
        assertFalse(assembler.isEventInProgress)
        assertTrue(assembler.isEventEmpty)
    }

    @Test
    fun commitWithoutBeginRetainsTheLegacyFailureMessage() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)

        val failure = assertThrows(IllegalStateException::class.java) {
            assembler.commitEvent()
        }

        assertEquals("Cannot commit without an event in progress", failure.message)
    }

    @Test
    fun discardWithoutBeginRetainsTheLegacyFailureMessageAndLeavesAssemblerReusable() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)
        val changes = mutableListOf<Change>()
        assembler.addListEventListener { event -> changes += snapshotChanges(event) }

        val failure = assertThrows(IllegalStateException::class.java) {
            assembler.discardEvent()
        }

        assertEquals("Cannot discard without an event in progress", failure.message)
        assertFalse(assembler.isEventInProgress)
        assertTrue(assembler.isEventEmpty)

        publishInsert(assembler, "reused")

        assertEquals(
            listOf(Change(ListEvent.INSERT, 0, ListEvent.UNKNOWN_VALUE, "reused", false, null)),
            changes,
        )
    }

    @Test
    fun changesRecordedWithoutBeginDoNotThrowUntilCommitAndAreDroppedByTheNextNormalCycle() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)
        var notifications = 0
        assembler.addListEventListener { notifications++ }

        assembler.elementInserted(0, "orphaned")

        assertFalse(assembler.isEventInProgress)
        assertFalse(assembler.isEventEmpty)

        val failure = assertThrows(IllegalStateException::class.java) {
            assembler.commitEvent()
        }
        assertEquals("Cannot commit without an event in progress", failure.message)
        assertFalse(assembler.isEventInProgress)
        assertFalse(assembler.isEventEmpty)

        assembler.beginEvent()
        assertTrue(assembler.isEventInProgress)
        assertTrue(assembler.isEventEmpty)
        assembler.commitEvent()

        assertEquals(0, notifications)
        assertTrue(assembler.isEventEmpty)
        assertFalse(assembler.isEventInProgress)
    }

    @Test
    fun emptyCommitDoesNotPublishAndResetsTheAssemblerForReuse() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)
        val changes = mutableListOf<Change>()
        assembler.addListEventListener { event ->
            while (event.next()) {
                changes += Change(event.type, event.index, event.oldValue, event.newValue, event.isReordering, null)
            }
        }

        assembler.beginEvent()
        assembler.commitEvent()
        assertTrue(changes.isEmpty())

        publishInsert(assembler, "later")
        assertEquals(listOf(Change(ListEvent.INSERT, 0, ListEvent.UNKNOWN_VALUE, "later", false, null)), changes)
    }

    @Test
    fun nestedCommitsPublishOnlyAtTheOutermostBoundary() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)
        val batches = mutableListOf<List<Change>>()
        assembler.addListEventListener { event ->
            val batch = mutableListOf<Change>()
            while (event.next()) {
                batch += Change(event.type, event.index, event.oldValue, event.newValue, event.isReordering, null)
            }
            batches += batch
        }

        val first = Any()
        val second = Any()
        assembler.beginEvent(true)
        assembler.elementInserted(0, first)
        assembler.beginEvent(false)
        assembler.elementInserted(1, second)
        assembler.commitEvent()

        assertTrue(assembler.isEventInProgress)
        assertTrue(batches.isEmpty())

        assembler.commitEvent()

        assertEquals(1, batches.size)
        assertEquals(listOf(0, 1), batches.single().map(Change::index))
        assertEquals(listOf(first, second), batches.single().map(Change::newValue))
    }

    @Test
    fun nestedDiscardRetainsRecordedChangesUntilTheOutermostCommitPublishesAndCleansUp() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)
        val outerValue = Any()
        val innerValue = Any()
        val reuseValue = Any()
        val batches = mutableListOf<List<Change>>()
        assembler.addListEventListener { event -> batches += snapshotChanges(event) }

        assembler.beginEvent(true)
        assembler.elementInserted(0, outerValue)
        assembler.beginEvent(true)
        assembler.elementInserted(1, innerValue)
        assembler.discardEvent()

        assertTrue(assembler.isEventInProgress)
        assertFalse(assembler.isEventEmpty)
        assertTrue(batches.isEmpty())

        assembler.commitEvent()

        assertEquals(1, batches.size)
        assertEquals(listOf(0, 1), batches.single().map(Change::index))
        assertSame(outerValue, batches.single()[0].newValue)
        assertSame(innerValue, batches.single()[1].newValue)
        assertFalse(assembler.isEventInProgress)
        assertTrue(assembler.isEventEmpty)

        publishInsert(assembler, reuseValue)

        assertEquals(2, batches.size)
        assertEquals(listOf(0), batches[1].map(Change::index))
        assertSame(reuseValue, batches[1].single().newValue)
    }

    @Test
    fun reorderOnlyEventsRetainTheOriginalMapAliasAndDeleteThenInsertBlocks() {
        val source = BasicEventList<Any>()
        repeat(3) { source += Any() }
        val assembler = ListEventAssembler(source, source.publisher)
        val reorderMap = intArrayOf(2, 1, 0)
        val blocks = mutableListOf<BlockChange>()
        assembler.addListEventListener { event ->
            assertTrue(event.isReordering)
            assertSame(reorderMap, event.reorderMap)
            while (event.nextBlock()) {
                blocks += BlockChange(event.type, event.blockStartIndex, event.blockEndIndex)
            }
        }

        assembler.beginEvent()
        assembler.reorder(reorderMap)
        assembler.commitEvent()

        assertEquals(
            listOf(
                BlockChange(ListEvent.DELETE, 0, 2),
                BlockChange(ListEvent.INSERT, 0, 2),
            ),
            blocks,
        )
    }

    @Test
    fun reorderRejectsMixedAndRepeatedReorderOperations() {
        val source = BasicEventList<Any>()
        repeat(3) { source += Any() }
        val assembler = ListEventAssembler(source, source.publisher)

        assembler.beginEvent()
        assembler.elementInserted(0, Any())
        val mixedFailure = assertThrows(IllegalStateException::class.java) {
            assembler.reorder(intArrayOf(2, 1, 0))
        }
        assertEquals("Cannot combine reorder with other change events", mixedFailure.message)
        assembler.discardEvent()

        assembler.beginEvent()
        assembler.reorder(intArrayOf(2, 1, 0))
        val repeatedFailure = assertThrows(IllegalStateException::class.java) {
            assembler.reorder(intArrayOf(2, 1, 0))
        }
        assertEquals("Cannot combine reorder with other change events", repeatedFailure.message)
        assembler.discardEvent()
    }

    @Test
    fun zeroLengthReorderIsATotalNoOp() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)
        var notifications = 0
        assembler.addListEventListener { notifications++ }

        assembler.beginEvent()
        assembler.reorder(IntArray(0))
        assertTrue(assembler.isEventEmpty)
        assembler.commitEvent()

        assertEquals(0, notifications)
        assertTrue(assembler.isEventEmpty)

        publishInsert(assembler, Any())
        assertEquals(1, notifications)
    }

    @Test
    fun reorderUsesMapLengthEvenWhenItDoesNotMatchTheSourceSize() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)
        val blocks = mutableListOf<BlockChange>()
        assembler.addListEventListener { event ->
            while (event.nextBlock()) {
                blocks += BlockChange(event.type, event.blockStartIndex, event.blockEndIndex)
            }
        }

        assembler.beginEvent()
        assembler.reorder(intArrayOf(1, 0))
        assembler.commitEvent()

        assertEquals(
            listOf(
                BlockChange(ListEvent.DELETE, 0, 1),
                BlockChange(ListEvent.INSERT, 0, 1),
            ),
            blocks,
        )
    }

    @Test
    fun bulkChangesRetainTheirTypesIndicesAndUnknownValues() {
        assertRange(ListEvent.INSERT, 2..4) { assembler -> assembler.elementsInserted(2, 4) }
        assertRange(ListEvent.UPDATE, 3..5) { assembler -> assembler.elementsUpdated(3, 5) }
        assertRange(ListEvent.DELETE, 1..3) { assembler -> assembler.elementsDeleted(1, 3) }
    }

    @Test
    fun linearEventsRetainSuppliedSingleElementValueIdentities() {
        val oldValue = Any()
        val insertedValue = Any()
        val updatedValue = Any()
        val deletedValue = Any()

        val insert = collectChanges { assembler -> assembler.elementInserted(0, insertedValue) }.single()
        val update = collectChanges { assembler -> assembler.elementUpdated(0, oldValue, updatedValue) }.single()
        val delete = collectChanges { assembler -> assembler.elementDeleted(0, deletedValue) }.single()

        assertSame(ListEvent.UNKNOWN_VALUE, insert.oldValue)
        assertSame(insertedValue, insert.newValue)
        assertSame(oldValue, update.oldValue)
        assertSame(updatedValue, update.newValue)
        assertSame(deletedValue, delete.oldValue)
        assertSame(ListEvent.UNKNOWN_VALUE, delete.newValue)
    }

    @Test
    fun treeFallbackRetainsPerElementOldValuesForOverlappingUpdates() {
        val oldValue = Any()

        val changes = collectChanges(2) { assembler ->
            assembler.elementUpdated(0, oldValue, Any())
            assembler.elementsUpdated(0, 1)
        }

        assertEquals(listOf(0, 1), changes.map(Change::index))
        assertEquals(listOf(ListEvent.UPDATE, ListEvent.UPDATE), changes.map(Change::type))
        assertSame(oldValue, changes[0].oldValue)
        assertSame(ListEvent.UNKNOWN_VALUE, changes[1].oldValue)
    }

    @Test
    fun treeFallbackRetainsOriginalOldValueAndLatestNewValueForRepeatedUpdate() {
        val originalValue = Any()
        val intermediateValue = Any()
        val latestValue = Any()

        val update = collectChanges(1) { assembler ->
            assembler.elementUpdated(0, originalValue, intermediateValue)
            assembler.elementUpdated(0, intermediateValue, latestValue)
        }.single()

        assertEquals(ListEvent.UPDATE, update.type)
        assertSame(originalValue, update.oldValue)
        assertSame(latestValue, update.newValue)
    }

    @Test
    fun treeFallbackRetainsPerElementOldValuesForOverlappingDeletes() {
        val oldValue = Any()

        val changes = collectChanges(2) { assembler ->
            assembler.elementUpdated(0, oldValue, Any())
            assembler.elementsDeleted(0, 1)
        }

        assertEquals(listOf(0, 0), changes.map(Change::index))
        assertEquals(listOf(ListEvent.DELETE, ListEvent.DELETE), changes.map(Change::type))
        assertSame(oldValue, changes[0].oldValue)
        assertSame(ListEvent.UNKNOWN_VALUE, changes[1].oldValue)
    }

    @Test
    fun treeFallbackExposesInsertedValuesAsNewValues() {
        val firstValue = Any()
        val secondValue = Any()

        val changes = collectChanges(2) { assembler ->
            assembler.elementInserted(1, firstValue)
            assembler.elementInserted(0, secondValue)
        }

        assertEquals(listOf(0, 2), changes.map(Change::index))
        assertEquals(listOf(ListEvent.INSERT, ListEvent.INSERT), changes.map(Change::type))
        changes.forEach { change -> assertSame(ListEvent.UNKNOWN_VALUE, change.oldValue) }
        assertSame(secondValue, changes[0].newValue)
        assertSame(firstValue, changes[1].newValue)
    }

    @Test
    fun insertThenUpdateNormalizesToInsertWithTheLatestValueRegardlessOfContradictionFlag() {
        for (allowContradictingEvents in listOf(false, true)) {
            val insertedValue = Any()
            val updatedValue = Any()

            val changes = collectChanges(allowContradictingEvents = allowContradictingEvents) { assembler ->
                assembler.elementInserted(0, insertedValue)
                assembler.elementUpdated(0, insertedValue, updatedValue)
            }

            val insert = changes.single()
            assertEquals(ListEvent.INSERT, insert.type)
            assertSame(ListEvent.UNKNOWN_VALUE, insert.oldValue)
            assertSame(updatedValue, insert.newValue)
        }
    }

    @Test
    fun insertThenDeleteFailsWithoutContradictionsAndCancelsCompletelyWithThem() {
        val insertedValue = Any()
        val strictFailure = assertThrows(IllegalStateException::class.java) {
            collectChanges(allowContradictingEvents = false) { assembler ->
                assembler.elementInserted(0, insertedValue)
                assembler.elementDeleted(0, insertedValue)
            }
        }
        assertEquals(
            "Remove 0 undoes prior insert at the same index! Consider enabling contradicting events.",
            strictFailure.message,
        )

        val contradictoryChanges = collectChanges(allowContradictingEvents = true) { assembler ->
            assembler.elementInserted(0, insertedValue)
            assembler.elementDeleted(0, insertedValue)
        }
        assertTrue(contradictoryChanges.isEmpty())
    }

    @Test
    fun deleteThenInsertRemainsADeleteFollowedByAnInsertForBothContradictionModes() {
        for (allowContradictingEvents in listOf(false, true)) {
            val oldValue = Any()
            val newValue = Any()

            val changes = collectChanges(1, allowContradictingEvents) { assembler ->
                assembler.elementDeleted(0, oldValue)
                assembler.elementInserted(0, newValue)
            }

            assertEquals(listOf(ListEvent.DELETE, ListEvent.INSERT), changes.map(Change::type))
            assertEquals(listOf(0, 0), changes.map(Change::index))
            assertSame(oldValue, changes[0].oldValue)
            assertSame(ListEvent.UNKNOWN_VALUE, changes[0].newValue)
            assertSame(ListEvent.UNKNOWN_VALUE, changes[1].oldValue)
            assertSame(newValue, changes[1].newValue)
        }
    }

    @Test
    fun deleteThenUpdateRemainsADeleteFollowedByAnUpdateForBothContradictionModes() {
        for (allowContradictingEvents in listOf(false, true)) {
            val oldValue = Any()
            val newValue = Any()

            val changes = collectChanges(1, allowContradictingEvents) { assembler ->
                assembler.elementDeleted(0, oldValue)
                assembler.elementUpdated(0, oldValue, newValue)
            }

            assertEquals(listOf(ListEvent.DELETE, ListEvent.UPDATE), changes.map(Change::type))
            assertEquals(listOf(0, 0), changes.map(Change::index))
            assertSame(oldValue, changes[0].oldValue)
            assertSame(ListEvent.UNKNOWN_VALUE, changes[0].newValue)
            assertSame(oldValue, changes[1].oldValue)
            assertSame(newValue, changes[1].newValue)
        }
    }

    @Test
    fun reorderThenUpdateRetainsTheReorderingFlagAndItsCurrentWeirdDeleteInsertShape() {
        val reorderMap = intArrayOf(2, 1, 0)
        val changes = collectChanges(sourceSize = 3) { assembler ->
            assembler.reorder(reorderMap)
            assembler.elementUpdated(1, "old", "new")
        }

        assertEquals(
            listOf(
                Change(ListEvent.INSERT, 0, ListEvent.UNKNOWN_VALUE, ListEvent.UNKNOWN_VALUE, true, reorderMap),
                Change(ListEvent.INSERT, 1, ListEvent.UNKNOWN_VALUE, "new", true, reorderMap),
                Change(ListEvent.INSERT, 2, ListEvent.UNKNOWN_VALUE, ListEvent.UNKNOWN_VALUE, true, reorderMap),
                Change(ListEvent.DELETE, 3, ListEvent.UNKNOWN_VALUE, ListEvent.UNKNOWN_VALUE, true, reorderMap),
                Change(ListEvent.DELETE, 3, ListEvent.UNKNOWN_VALUE, ListEvent.UNKNOWN_VALUE, true, reorderMap),
                Change(ListEvent.DELETE, 3, ListEvent.UNKNOWN_VALUE, ListEvent.UNKNOWN_VALUE, true, reorderMap),
            ),
            changes,
        )
    }

    @Test
    fun linearEventReportsRemainingBlocks() {
        assertRemainingBlocks(forceTreeFallback = false)
    }

    @Test
    fun treeEventReportsRemainingBlocks() {
        assertRemainingBlocks(forceTreeFallback = true)
    }

    @Test
    fun treeFallbackPreservesPayloadIdentityAcrossCallsForCoalescing() {
        val oldValue = Any()
        val newValue = Any()
        val source = BasicEventList<Any>()
        repeat(3) { source += Any() }
        val assembler = ListEventAssembler(source, source.publisher)
        assembler.addListEventListener { event ->
            assertEquals(2, event.blocksRemaining)
            val indices = mutableListOf<Int>()
            while (event.next()) {
                indices += event.index
                assertSame(oldValue, event.oldValue)
                assertSame(newValue, event.newValue)
            }
            assertEquals(listOf(0, 1, 2), indices)
        }

        assembler.beginEvent()
        assembler.elementUpdated(2, oldValue, newValue)
        assembler.elementUpdated(0, oldValue, newValue)
        assembler.elementUpdated(1, oldValue, newValue)
        assembler.commitEvent()
    }

    @Test
    fun treeEventCopiesIterateIndependentlyWithTheSameValues() {
        val firstValue = Any()
        val secondValue = Any()
        val source = BasicEventList<Any>()
        repeat(2) { source += Any() }
        val assembler = ListEventAssembler(source, source.publisher)
        assembler.addListEventListener { event ->
            val copy = event.copy()

            assertTrue(event.next())
            assertTrue(copy.next())
            assertEquals(event.index, copy.index)
            assertSame(event.oldValue, copy.oldValue)
            assertSame(event.newValue, copy.newValue)

            assertTrue(event.next())
            assertEquals(0, copy.index)
            assertTrue(copy.next())
            assertEquals(event.index, copy.index)
            assertSame(secondValue, event.newValue)
            assertSame(secondValue, copy.newValue)
        }

        assembler.beginEvent()
        assembler.elementInserted(1, secondValue)
        assembler.elementInserted(0, firstValue)
        assembler.commitEvent()
    }

    @Test
    fun forwardEventAcceptsARealEventFromAnotherSourceAndResetsOnlyTheOriginalCursor() {
        val firstValue = Any()
        val secondValue = Any()
        val producer = pendingEvent {
            it.elementInserted(0, firstValue)
            it.elementInserted(1, secondValue)
        }
        val copy = producer.event.copy()
        assertTrue(copy.next())
        assertEquals(0, copy.index)

        val consumerSource = BasicEventList<Any>()
        val consumer = ListEventAssembler(consumerSource, consumerSource.publisher)
        val forwarded = mutableListOf<Change>()
        consumer.addListEventListener { event ->
            while (event.next()) {
                forwarded += Change(event.type, event.index, event.oldValue, event.newValue, event.isReordering, null)
            }
        }

        consumer.forwardEvent(producer.event)

        assertEquals(
            listOf(
                Change(ListEvent.INSERT, 0, ListEvent.UNKNOWN_VALUE, firstValue, false, null),
                Change(ListEvent.INSERT, 1, ListEvent.UNKNOWN_VALUE, secondValue, false, null),
            ),
            forwarded,
        )
        assertTrue(producer.event.next())
        assertEquals(0, producer.event.index)
        assertEquals(0, copy.index)
        assertTrue(copy.next())
        assertEquals(1, copy.index)

        producer.assembler.discardEvent()
    }

    @Test
    fun forwardedReorderRetainsTheOriginalMapAliasAndDoesNotResetTheSourceCursor() {
        val reorderMap = intArrayOf(2, 1, 0)
        val producer = pendingEvent(sourceSize = 3) {
            it.reorder(reorderMap)
        }
        assertTrue(producer.event.nextBlock())
        assertEquals(0, producer.event.index)

        val consumerSource = BasicEventList<Any>()
        val consumer = ListEventAssembler(consumerSource, consumerSource.publisher)
        val forwardedBlocks = mutableListOf<BlockChange>()
        consumer.addListEventListener { event ->
            assertTrue(event.isReordering)
            assertSame(reorderMap, event.reorderMap)
            while (event.nextBlock()) {
                forwardedBlocks += BlockChange(event.type, event.blockStartIndex, event.blockEndIndex)
            }
        }

        consumer.forwardEvent(producer.event)

        assertEquals(0, producer.event.index)
        assertTrue(producer.event.nextBlock())
        assertEquals(0, producer.event.index)
        assertEquals(
            listOf(
                BlockChange(ListEvent.DELETE, 0, 2),
                BlockChange(ListEvent.INSERT, 0, 2),
            ),
            forwardedBlocks,
        )

        producer.assembler.discardEvent()
    }

    @Test
    fun partiallyConsumedForwardEventCopiesOnlyTheRemainingChangesAndThenResetsTheSourceCursor() {
        val firstValue = Any()
        val secondValue = Any()
        val thirdValue = Any()
        val producer = pendingEvent {
            it.elementInserted(0, firstValue)
            it.elementInserted(1, secondValue)
            it.elementInserted(2, thirdValue)
        }
        assertTrue(producer.event.next())
        assertEquals(0, producer.event.index)
        assertSame(firstValue, producer.event.newValue)

        val consumerSource = BasicEventList<Any>()
        val consumer = ListEventAssembler(consumerSource, consumerSource.publisher)
        val forwarded = mutableListOf<Change>()
        consumer.addListEventListener { event -> forwarded += snapshotChanges(event) }

        consumer.forwardEvent(producer.event)

        assertEquals(listOf(1, 2), forwarded.map(Change::index))
        assertSame(secondValue, forwarded[0].newValue)
        assertSame(thirdValue, forwarded[1].newValue)
        assertTrue(producer.event.next())
        assertEquals(0, producer.event.index)
        assertSame(firstValue, producer.event.newValue)

        producer.assembler.discardEvent()
    }

    @Test
    fun forwardEventInsideAllowedNestingPublishesOnlyWhenTheOuterEventCommits() {
        val producer = pendingEvent(sourceSize = 1) {
            it.elementUpdated(0, "old", "new")
        }
        val consumerSource = BasicEventList<Any>()
        val consumer = ListEventAssembler(consumerSource, consumerSource.publisher)
        val batches = mutableListOf<List<Change>>()
        consumer.addListEventListener { event ->
            val batch = mutableListOf<Change>()
            while (event.next()) {
                batch += Change(event.type, event.index, event.oldValue, event.newValue, event.isReordering, null)
            }
            batches += batch
        }

        consumer.beginEvent(true)
        consumer.forwardEvent(producer.event)
        assertTrue(consumer.isEventInProgress)
        assertTrue(batches.isEmpty())

        consumer.commitEvent()

        assertEquals(
            listOf(
                listOf(Change(ListEvent.UPDATE, 0, "old", "new", false, null)),
            ),
            batches,
        )

        producer.assembler.discardEvent()
    }

    @Test
    fun reentrantListenerEventMergesIntoTheCurrentPublicationForLaterListenersThenCleansUp() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)
        val outerValue = Any()
        val nestedValue = Any()
        val reuseValue = Any()
        val deliveries = mutableListOf<Pair<String, List<Change>>>()
        var nestedEventSent = false
        val firstListener =
            ListEventListener { event ->
                deliveries += "first" to snapshotChanges(event)
                if (!nestedEventSent) {
                    nestedEventSent = true
                    assembler.beginEvent()
                    assembler.elementInserted(1, nestedValue)
                    assembler.commitEvent()
                }
            }
        val secondListener =
            ListEventListener { event ->
                deliveries += "second" to snapshotChanges(event)
            }
        assembler.addListEventListener(firstListener)
        assembler.addListEventListener(secondListener)

        assembler.beginEvent()
        assembler.elementInserted(0, outerValue)
        assembler.commitEvent()

        assertEquals(2, deliveries.size)
        assertEquals("first", deliveries[0].first)
        assertEquals(listOf(0), deliveries[0].second.map(Change::index))
        assertSame(outerValue, deliveries[0].second.single().newValue)
        assertEquals("second", deliveries[1].first)
        assertEquals(listOf(0, 1), deliveries[1].second.map(Change::index))
        assertSame(outerValue, deliveries[1].second[0].newValue)
        assertSame(nestedValue, deliveries[1].second[1].newValue)
        assertFalse(assembler.isEventInProgress)
        assertTrue(assembler.isEventEmpty)

        deliveries.clear()
        publishInsert(assembler, reuseValue)

        assertEquals(2, deliveries.size)
        assertEquals(listOf(0), deliveries[0].second.map(Change::index))
        assertEquals(listOf(0), deliveries[1].second.map(Change::index))
        assertSame(reuseValue, deliveries[0].second.single().newValue)
        assertSame(reuseValue, deliveries[1].second.single().newValue)
    }

    @Test
    fun reentrantListenerEventsEnterContradictionModeDuringPublicationAndRemainReusable() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)
        val outerValue = Any()
        val transientValue = Any()
        val reuseValue = Any()
        val deliveries = mutableListOf<List<Change>>()
        var nestedEventSent = false
        assembler.addListEventListener { event ->
            deliveries += snapshotChanges(event)
            if (!nestedEventSent) {
                nestedEventSent = true
                assertDoesNotThrow {
                    assembler.beginEvent()
                    assembler.elementInserted(0, transientValue)
                    assembler.elementDeleted(0, transientValue)
                    assembler.commitEvent()
                }
            }
        }
        assembler.addListEventListener { event ->
            deliveries += snapshotChanges(event)
        }

        assembler.beginEvent()
        assembler.elementInserted(0, outerValue)
        assembler.commitEvent()

        assertEquals(2, deliveries.size)
        deliveries.forEach { batch ->
            assertEquals(listOf(0), batch.map(Change::index))
            assertSame(outerValue, batch.single().newValue)
        }
        assertFalse(assembler.isEventInProgress)
        assertTrue(assembler.isEventEmpty)

        deliveries.clear()
        publishInsert(assembler, reuseValue)

        assertEquals(2, deliveries.size)
        deliveries.forEach { batch ->
            assertEquals(listOf(0), batch.map(Change::index))
            assertSame(reuseValue, batch.single().newValue)
        }
    }

    @Test
    fun forwardEventInsideDisallowedNestingFailsWithTheCurrentThreadName() {
        val producer = pendingEvent {
            it.elementInserted(0, "value")
        }
        val consumerSource = BasicEventList<Any>()
        val consumer = ListEventAssembler(consumerSource, consumerSource.publisher)

        consumer.beginEvent()
        val failure = assertThrows(ConcurrentModificationException::class.java) {
            consumer.forwardEvent(producer.event)
        }
        assertEquals(
            "Cannot begin a new event while another event is in progress by thread, ${Thread.currentThread().name}",
            failure.message,
        )
        assertTrue(consumer.isEventInProgress)
        consumer.discardEvent()

        producer.assembler.discardEvent()
    }

    @Test
    fun malformedForwardEventAfterOneValidCopyLeavesPartialStateUntilDiscardThenReusesCleanly() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)
        val firstValue = Any()
        val reuseValue = Any()
        val malformed = PartiallyExplodingListEvent(BasicEventList(), firstValue, IllegalStateException("bad second change"))
        val changes = mutableListOf<Change>()
        assembler.addListEventListener { event -> changes += snapshotChanges(event) }

        val failure = assertThrows(IllegalStateException::class.java) {
            assembler.forwardEvent(malformed)
        }
        assertEquals("bad second change", failure.message)
        assertTrue(assembler.isEventInProgress)
        assertFalse(assembler.isEventEmpty)
        assertTrue(changes.isEmpty())

        assembler.discardEvent()
        assertFalse(assembler.isEventInProgress)
        assertTrue(assembler.isEventEmpty)

        publishInsert(assembler, reuseValue)

        assertEquals(1, changes.size)
        assertSame(reuseValue, changes.single().newValue)
    }

    @Test
    fun malformedForwardEventLeavesNestedStateUntilDiscardThenReusesCleanly() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)
        val malformed = ExplodingListEvent(BasicEventList(), IllegalStateException("bad type"))

        val failure = assertThrows(IllegalStateException::class.java) {
            assembler.forwardEvent(malformed)
        }
        assertEquals("bad type", failure.message)
        assertTrue(assembler.isEventInProgress)
        assertTrue(assembler.isEventEmpty)

        assembler.discardEvent()
        assertFalse(assembler.isEventInProgress)
        assertTrue(assembler.isEventEmpty)

        val changes = mutableListOf<Change>()
        assembler.addListEventListener { event ->
            while (event.next()) {
                changes += Change(event.type, event.index, event.oldValue, event.newValue, event.isReordering, null)
            }
        }
        publishInsert(assembler, "reused")

        assertEquals(
            listOf(Change(ListEvent.INSERT, 0, ListEvent.UNKNOWN_VALUE, "reused", false, null)),
            changes,
        )
    }

    @Test
    fun listenerExceptionStillDeliversLaterListenersThenCleansUpAndReuses() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)
        val trace = mutableListOf<String>()
        var failNext = true
        val firstListener =
            ListEventListener<Any> {
                trace += "first"
                if (failNext) {
                    failNext = false
                    throw IllegalStateException("listener failure")
                }
            }
        val secondListener = ListEventListener<Any> { trace += "second" }
        assembler.addListEventListener(firstListener)
        assembler.addListEventListener(secondListener)

        val failure = assertThrows(IllegalStateException::class.java) {
            publishInsert(assembler, Any())
        }
        assertEquals("listener failure", failure.message)
        assertEquals(listOf("first", "second"), trace)
        assertFalse(assembler.isEventInProgress)
        assertTrue(assembler.isEventEmpty)

        trace.clear()
        publishInsert(assembler, Any())
        assertEquals(listOf("first", "second"), trace)
    }

    @Test
    fun orderingFailureDuringListenerRegistrationLeavesTheSharedPublisherUsable() {
        val publisher = ListEventAssembler.createListEventPublisher()
        val first = BasicEventList<String>(publisher, null)
        val second = BasicEventList<String>(publisher, null)
        val third = BasicEventList<String>(publisher, null)
        val trace = mutableListOf<String>()
        val firstListener = ListEventListener<String> { trace += "first" }
        val secondListener = ListEventListener<String> { trace += "second" }

        first.addListEventListener(firstListener)
        second.addListEventListener(secondListener)
        publisher.setRelatedSubject(firstListener, second)
        publisher.setRelatedSubject(secondListener, first)

        val failure = assertThrows(IllegalStateException::class.java) {
            third.addListEventListener(ListEventListener<String> {})
        }
        assertTrue(failure.message!!.startsWith("Listener cycle detected"))

        first.add("first")
        second.add("second")

        assertEquals(listOf("first", "second"), trace)
    }

    private fun assertRange(
        expectedType: Int,
        expectedIndices: IntRange,
        addChanges: (ListEventAssembler<String>) -> Unit,
    ) {
        val source = BasicEventList<String>()
        val assembler = ListEventAssembler(source, source.publisher)
        val changes = mutableListOf<Change>()
        assembler.addListEventListener { event ->
            while (event.next()) {
                changes += Change(event.type, event.index, event.oldValue, event.newValue, event.isReordering, null)
            }
        }

        assembler.beginEvent()
        addChanges(assembler)
        assembler.commitEvent()

        val expectedEventIndices = if (expectedType == ListEvent.DELETE) {
            List(expectedIndices.count()) { expectedIndices.first }
        } else {
            expectedIndices.toList()
        }
        assertEquals(expectedEventIndices, changes.map(Change::index))
        assertEquals(List(expectedIndices.count()) { expectedType }, changes.map(Change::type))
        changes.forEach { change ->
            assertSame(ListEvent.UNKNOWN_VALUE, change.oldValue)
            assertSame(ListEvent.UNKNOWN_VALUE, change.newValue)
        }
    }

    private fun collectChanges(
        sourceSize: Int = 0,
        allowContradictingEvents: Boolean = false,
        addChanges: (ListEventAssembler<Any>) -> Unit,
    ): List<Change> {
        val source = BasicEventList<Any>()
        repeat(sourceSize) { source += Any() }
        val assembler = ListEventAssembler(source, source.publisher)
        val changes = mutableListOf<Change>()
        assembler.addListEventListener { event ->
            while (event.next()) {
                changes += Change(
                    event.type,
                    event.index,
                    event.oldValue,
                    event.newValue,
                    event.isReordering,
                    if (event.isReordering) event.reorderMap else null,
                )
            }
        }

        assembler.beginEvent(allowContradictingEvents)
        addChanges(assembler)
        assembler.commitEvent()
        return changes
    }

    private fun snapshotChanges(event: ListEvent<Any>): List<Change> {
        val changes = mutableListOf<Change>()
        while (event.next()) {
            changes += Change(
                event.type,
                event.index,
                event.oldValue,
                event.newValue,
                event.isReordering,
                if (event.isReordering) event.reorderMap else null,
            )
        }
        return changes
    }

    private fun assertRemainingBlocks(forceTreeFallback: Boolean) {
        val source = BasicEventList<Any>()
        repeat(3) { source += Any() }
        val assembler = ListEventAssembler(source, source.publisher)
        val counts = mutableListOf<Int>()
        assembler.addListEventListener { event ->
            counts += event.blocksRemaining
            assertTrue(event.nextBlock())
            counts += event.blocksRemaining
            assertTrue(event.nextBlock())
            counts += event.blocksRemaining
            assertFalse(event.nextBlock())
        }

        assembler.beginEvent()
        if (forceTreeFallback) {
            assembler.elementUpdated(2, Any(), Any())
            assembler.elementUpdated(0, Any(), Any())
        } else {
            assembler.elementUpdated(0, Any(), Any())
            assembler.elementUpdated(2, Any(), Any())
        }
        assembler.commitEvent()

        assertEquals(listOf(2, 1, 0), counts)
    }

    private fun publishInsert(assembler: ListEventAssembler<Any>, value: Any) {
        assembler.beginEvent()
        assembler.elementInserted(0, value)
        assembler.commitEvent()
    }

    @Suppress("UNCHECKED_CAST")
    private fun pendingEvent(
        sourceSize: Int = 0,
        addChanges: (ListEventAssembler<Any>) -> Unit,
    ): PendingEvent {
        val source = BasicEventList<Any>()
        repeat(sourceSize) { source += Any() }
        val assembler = ListEventAssembler(source, source.publisher)
        assembler.beginEvent()
        addChanges(assembler)
        val listEventField = ListEventAssembler::class.java.getDeclaredField("listEvent")
        listEventField.isAccessible = true
        val event = listEventField.get(assembler) as ListEvent<Any>
        event.reset()
        return PendingEvent(source, assembler, event)
    }

    private fun assertListenerSnapshot(assembler: ListEventAssembler<Any>, vararg expected: ListEventListener<Any>) {
        val listeners = assembler.getListEventListeners()
        assertEquals(expected.size, listeners.size)
        expected.forEachIndexed { index, listener ->
            assertSame(listener, listeners[index])
        }
    }

    private fun assertField(
        field: Field,
        expectedDescriptor: String,
        expectedGenericType: String? = null,
        public: Boolean = false,
        protected: Boolean = false,
        private: Boolean = false,
        static: Boolean = false,
        final: Boolean = false,
    ) {
        assertEquals(expectedDescriptor, descriptorOf(field), field.name)
        expectedGenericType?.let { assertEquals(it, field.genericType.typeName, field.name) }
        assertEquals(public, Modifier.isPublic(field.modifiers), "${field.name} public")
        assertEquals(protected, Modifier.isProtected(field.modifiers), "${field.name} protected")
        assertEquals(private, Modifier.isPrivate(field.modifiers), "${field.name} private")
        assertEquals(static, Modifier.isStatic(field.modifiers), "${field.name} static")
        assertEquals(final, Modifier.isFinal(field.modifiers), "${field.name} final")
    }

    private fun assertMethod(
        method: Method,
        expectedDescriptor: String,
        expectedGenericReturnType: String,
        expectedGenericParameters: List<String> = method.genericParameterTypes.map { it.typeName },
        public: Boolean = false,
        protected: Boolean = false,
        private: Boolean = false,
        static: Boolean = false,
        final: Boolean = false,
        synchronized: Boolean = false,
        synthetic: Boolean = false,
    ) {
        assertEquals(expectedDescriptor, descriptorOf(method), method.name)
        assertEquals(expectedGenericReturnType, method.genericReturnType.typeName, method.name)
        assertEquals(expectedGenericParameters, method.genericParameterTypes.map { it.typeName }, method.name)
        assertEquals(public, Modifier.isPublic(method.modifiers), "${method.name} public")
        assertEquals(protected, Modifier.isProtected(method.modifiers), "${method.name} protected")
        assertEquals(private, Modifier.isPrivate(method.modifiers), "${method.name} private")
        assertEquals(static, Modifier.isStatic(method.modifiers), "${method.name} static")
        assertEquals(final, Modifier.isFinal(method.modifiers), "${method.name} final")
        assertEquals(synchronized, Modifier.isSynchronized(method.modifiers), "${method.name} synchronized")
        assertEquals(synthetic, method.isSynthetic, "${method.name} synthetic")
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> fieldValue(target: Any, fieldName: String): T {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(target) as T
    }

    private fun descriptorOf(constructor: Constructor<*>): String =
        buildString {
            append('(')
            constructor.parameterTypes.forEach { append(descriptorOf(it)) }
            append(")V")
        }

    private fun descriptorOf(method: Method): String =
        buildString {
            append('(')
            method.parameterTypes.forEach { append(descriptorOf(it)) }
            append(')')
            append(descriptorOf(method.returnType))
        }

    private fun descriptorOf(field: Field): String = descriptorOf(field.type)

    private fun signatureOf(method: Method): String = method.name + descriptorOf(method)

    private fun descriptorOf(type: Class<*>): String =
        when {
            type.isPrimitive -> primitiveDescriptors.getValue(type)
            type.isArray -> type.name.replace('.', '/')
            else -> "L${type.name.replace('.', '/')};"
        }

    private class Change(
        val type: Int,
        val index: Int,
        val oldValue: Any?,
        val newValue: Any?,
        val reordering: Boolean,
        val reorderMap: IntArray?,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Change) return false
            return type == other.type &&
                index == other.index &&
                oldValue === other.oldValue &&
                newValue === other.newValue &&
                reordering == other.reordering &&
                reorderMap === other.reorderMap
        }

        override fun hashCode(): Int {
            var result = type
            result = 31 * result + index
            result = 31 * result + System.identityHashCode(oldValue)
            result = 31 * result + System.identityHashCode(newValue)
            result = 31 * result + reordering.hashCode()
            result = 31 * result + System.identityHashCode(reorderMap)
            return result
        }

        override fun toString(): String =
            "Change(type=$type, index=$index, oldValue=$oldValue, newValue=$newValue, reordering=$reordering, reorderMap=${reorderMap?.contentToString()})"
    }

    private data class BlockChange(val type: Int, val start: Int, val end: Int)

    private data class PendingEvent(
        val source: BasicEventList<Any>,
        val assembler: ListEventAssembler<Any>,
        val event: ListEvent<Any>,
    )

    private class ForeignListEventPublisher : ListEventPublisher {
        override fun setRelatedListener(subject: Any, relatedListener: Any) = Unit

        override fun clearRelatedListener(subject: Any, relatedListener: Any) = Unit

        override fun setRelatedSubject(listener: Any, relatedSubject: Any) = Unit

        override fun clearRelatedSubject(listener: Any) = Unit
    }

    private class EqualButDistinctListener(
        private val equalityKey: String,
        private val id: String,
        private val trace: MutableList<String>,
    ) : ListEventListener<Any> {
        override fun listChanged(listChanges: ListEvent<Any>) {
            trace += id
        }

        override fun equals(other: Any?): Boolean =
            other is EqualButDistinctListener && other.equalityKey == equalityKey

        override fun hashCode(): Int = equalityKey.hashCode()
    }

    private class ExplodingListEvent(
        sourceList: EventList<Any>,
        private val failure: RuntimeException,
    ) : ListEvent<Any>(sourceList) {
        private var cursor = -1

        override fun copy(): ListEvent<Any> = this

        override fun reset() {
            cursor = -1
        }

        override fun next(): Boolean {
            if (cursor == -1) {
                cursor = 0
                return true
            }
            return false
        }

        override fun hasNext(): Boolean = cursor == -1

        override fun nextBlock(): Boolean = next()

        override fun isReordering(): Boolean = false

        override fun getReorderMap(): IntArray = throw IllegalStateException()

        override fun getIndex(): Int = 0

        override fun getBlockStartIndex(): Int = 0

        override fun getBlockEndIndex(): Int = 0

        override fun getType(): Int = throw failure

        override fun getOldValue(): Any = "old"

        override fun getNewValue(): Any = "new"

        override fun getBlocksRemaining(): Int = if (cursor == -1) 1 else 0

        override fun toString(): String = "ExplodingListEvent"
    }

    private class PartiallyExplodingListEvent(
        sourceList: EventList<Any>,
        private val firstValue: Any,
        private val failure: RuntimeException,
    ) : ListEvent<Any>(sourceList) {
        private var cursor = -1

        override fun copy(): ListEvent<Any> = this

        override fun reset() {
            cursor = -1
        }

        override fun next(): Boolean {
            if (cursor < 1) {
                cursor++
                return true
            }
            return false
        }

        override fun hasNext(): Boolean = cursor < 1

        override fun nextBlock(): Boolean = next()

        override fun isReordering(): Boolean = false

        override fun getReorderMap(): IntArray = throw IllegalStateException()

        override fun getIndex(): Int = if (cursor == 0) 0 else throw failure

        override fun getBlockStartIndex(): Int = if (cursor == 0) 0 else throw failure

        override fun getBlockEndIndex(): Int = if (cursor == 0) 0 else throw failure

        override fun getType(): Int = if (cursor == 0) INSERT else throw failure

        override fun getOldValue(): Any = if (cursor == 0) UNKNOWN_VALUE else throw failure

        override fun getNewValue(): Any = if (cursor == 0) firstValue else throw failure

        override fun getBlocksRemaining(): Int = 1 - cursor

        override fun toString(): String = "PartiallyExplodingListEvent"
    }

    private companion object {
        private const val DOLLAR = '$'

        private val primitiveDescriptors = mapOf(
            Boolean::class.javaPrimitiveType!! to "Z",
            Byte::class.javaPrimitiveType!! to "B",
            Char::class.javaPrimitiveType!! to "C",
            Short::class.javaPrimitiveType!! to "S",
            Int::class.javaPrimitiveType!! to "I",
            Long::class.javaPrimitiveType!! to "J",
            Float::class.javaPrimitiveType!! to "F",
            Double::class.javaPrimitiveType!! to "D",
            Void.TYPE to "V",
        )
    }
}
