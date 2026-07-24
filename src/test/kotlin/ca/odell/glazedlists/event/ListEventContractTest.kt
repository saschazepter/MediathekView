package ca.odell.glazedlists.event

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.EventList

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal class ListEventContractTest {
    @Test
    fun constantsAndUnknownValueRetainLegacyIdentity() {
        assertEquals(0, ListEvent.DELETE)
        assertEquals(1, ListEvent.UPDATE)
        assertEquals(2, ListEvent.INSERT)
        assertSame("UNKNOWN VALUE", ListEvent.UNKNOWN_VALUE)
        assertEquals("UNKNOWN VALUE", ListEvent.UNKNOWN_VALUE.toString())
        assertSame(ListEvent.unknownValue<Any>(), ListEvent.UNKNOWN_VALUE)
    }

    @Test
    fun unknownValueRetainsIdentityAcrossGenericViews() {
        val asString: String = ListEvent.unknownValue()
        val asAny: Any = ListEvent.unknownValue()
        val asCharSequence: CharSequence = ListEvent.unknownValue()

        assertSame(ListEvent.UNKNOWN_VALUE, asString as Any)
        assertSame(asAny, ListEvent.UNKNOWN_VALUE)
        assertSame(ListEvent.UNKNOWN_VALUE, asCharSequence as Any)
    }

    @Test
    fun constructorRejectsNullSourceThroughEventObject() {
        val source = BasicEventList<Any>()
        val assembler = ListEventAssembler(source, source.publisher)
        val constructor = Class.forName("ca.odell.glazedlists.event.Tree4DeltasListEvent")
            .getDeclaredConstructor(ListEventAssembler::class.java, EventList::class.java)
        val failure = assertThrows(InvocationTargetException::class.java) {
            constructor.newInstance(assembler, null)
        }

        assertTrue(failure.cause is IllegalArgumentException)
        assertEquals("null source", failure.cause?.message)
    }

    @Test
    fun protectedSourceFieldsRemainMutableAndCanDiverge() {
        val originalSource = BasicEventList<String>()
        val replacementSource = BasicEventList<String>()
        val event = MutableListEvent(originalSource)

        assertSame(originalSource, event.source)
        assertSame(originalSource, event.sourceList)
        assertSame(originalSource, event.sourceListFromGetter())

        event.replaceSourceList(replacementSource)

        assertSame(originalSource, event.source)
        assertSame(replacementSource, event.sourceList)
        assertSame(replacementSource, event.sourceListFromGetter())

        event.replaceEventObjectSource(replacementSource)

        assertSame(replacementSource, event.source)
        assertSame(replacementSource, event.sourceList)
        assertSame(replacementSource, event.sourceListFromGetter())
    }

    @Test
    fun listEventAbiRetainsLegacyShapeExceptForDocumentedKotlinDifferences() {
        val listEventClass = ListEvent::class.java

        assertTrue(Modifier.isPublic(listEventClass.modifiers))
        assertTrue(Modifier.isAbstract(listEventClass.modifiers))
        assertEquals(listOf("E"), listEventClass.typeParameters.map { it.name })

        val constructor = listEventClass.getDeclaredConstructor(EventList::class.java)
        assertFalse(Modifier.isPublic(constructor.modifiers))
        // Kotlin cannot emit Java's package-private constructor for a public abstract class.
        // Protected is the narrowest boundary that still permits subclasses.
        assertTrue(Modifier.isProtected(constructor.modifiers))
        assertFalse(Modifier.isPrivate(constructor.modifiers))
        assertEquals("(Lca/odell/glazedlists/EventList;)V", descriptorOf(constructor))
        assertEquals("ca.odell.glazedlists.EventList<E>", constructor.genericParameterTypes.single().typeName)

        val sourceListField = listEventClass.getDeclaredField("sourceList")
        assertTrue(Modifier.isProtected(sourceListField.modifiers))
        assertFalse(Modifier.isFinal(sourceListField.modifiers))
        assertEquals("Lca/odell/glazedlists/EventList;", descriptorOf(sourceListField))

        assertFieldModifiers(listEventClass.getField("DELETE"))
        assertFieldModifiers(listEventClass.getField("UPDATE"))
        assertFieldModifiers(listEventClass.getField("INSERT"))
        assertFieldModifiers(listEventClass.getField("UNKNOWN_VALUE"))

        val unknownValue = listEventClass.getDeclaredMethod("unknownValue")
        assertTrue(Modifier.isPublic(unknownValue.modifiers))
        assertTrue(Modifier.isStatic(unknownValue.modifiers))
        // Kotlin's @JvmStatic bridge is final; the README documents this unavoidable drift.
        assertTrue(Modifier.isFinal(unknownValue.modifiers))
        assertEquals("()Ljava/lang/Object;", descriptorOf(unknownValue))
        assertEquals("E", unknownValue.genericReturnType.typeName)

        val companion = listEventClass.getDeclaredField("Companion")
        assertTrue(Modifier.isPublic(companion.modifiers))
        assertTrue(Modifier.isStatic(companion.modifiers))
        assertTrue(Modifier.isFinal(companion.modifiers))
        assertEquals(listEventClass.name + '$' + "Companion", companion.type.name)

        listOf(
            "getIndexProperty",
            "getBlockStartIndexProperty",
            "getBlockEndIndexProperty",
            "getTypeProperty",
            "getOldValueProperty",
            "getNewValueProperty",
            "getBlocksRemainingProperty",
            "getReorderMapProperty",
            "isReorderingProperty",
        ).forEach { name -> assertTrue(listEventClass.getDeclaredMethod(name).isSynthetic, name) }

        val abstractMethodDescriptors = mapOf(
            "copy" to "()Lca/odell/glazedlists/event/ListEvent;",
            "reset" to "()V",
            "next" to "()Z",
            "hasNext" to "()Z",
            "nextBlock" to "()Z",
            "isReordering" to "()Z",
            "getReorderMap" to "()[I",
            "getIndex" to "()I",
            "getBlockStartIndex" to "()I",
            "getBlockEndIndex" to "()I",
            "getType" to "()I",
            "getOldValue" to "()Ljava/lang/Object;",
            "getNewValue" to "()Ljava/lang/Object;",
            "getBlocksRemaining" to "()I",
            "toString" to "()Ljava/lang/String;",
        )
        for ((name, descriptor) in abstractMethodDescriptors) {
            val method = listEventClass.getDeclaredMethod(name)
            assertTrue(Modifier.isPublic(method.modifiers), name)
            assertTrue(Modifier.isAbstract(method.modifiers), name)
            assertEquals(descriptor, descriptorOf(method), name)
        }

        val getSourceList = listEventClass.getDeclaredMethod("getSourceList")
        assertTrue(Modifier.isPublic(getSourceList.modifiers))
        assertFalse(Modifier.isAbstract(getSourceList.modifiers))
        assertFalse(Modifier.isFinal(getSourceList.modifiers))
        assertEquals("()Lca/odell/glazedlists/EventList;", descriptorOf(getSourceList))
        assertEquals("ca.odell.glazedlists.EventList<E>", getSourceList.genericReturnType.typeName)

        val kotlinSourceList = Class.forName("ca.odell.glazedlists.event.ListEventKt")
            .getDeclaredMethod("getSourceList", listEventClass)
        assertTrue(kotlinSourceList.isSynthetic)
    }

    @Test
    fun tree4DeltasListEventAbiRetainsPackagePrivateClassAndPublicConstructor() {
        val treeEventClass = Class.forName("ca.odell.glazedlists.event.Tree4DeltasListEvent")

        assertFalse(Modifier.isPublic(treeEventClass.modifiers))
        assertFalse(Modifier.isPrivate(treeEventClass.modifiers))
        assertFalse(Modifier.isProtected(treeEventClass.modifiers))
        assertFalse(Modifier.isAbstract(treeEventClass.modifiers))
        assertFalse(Modifier.isFinal(treeEventClass.modifiers))
        assertEquals(listOf("E"), treeEventClass.typeParameters.map { it.name })
        assertEquals("ca.odell.glazedlists.event.ListEvent<E>", treeEventClass.genericSuperclass.typeName)

        val constructor = treeEventClass.getDeclaredConstructor(ListEventAssembler::class.java, EventList::class.java)
        assertTrue(Modifier.isPublic(constructor.modifiers))
        assertEquals(
            "(Lca/odell/glazedlists/event/ListEventAssembler;Lca/odell/glazedlists/EventList;)V",
            descriptorOf(constructor),
        )
        assertEquals(
            listOf("ca.odell.glazedlists.event.ListEventAssembler<E>", "ca.odell.glazedlists.EventList<E>"),
            constructor.genericParameterTypes.map { it.typeName },
        )

        val copy = treeEventClass.getDeclaredMethod("copy")
        assertEquals("()Lca/odell/glazedlists/event/ListEvent;", descriptorOf(copy))
        assertEquals("ca.odell.glazedlists.event.ListEvent<E>", copy.genericReturnType.typeName)

        val directMethods = mapOf(
            "reset" to "()V",
            "next" to "()Z",
            "hasNext" to "()Z",
            "nextBlock" to "()Z",
            "isReordering" to "()Z",
            "getReorderMap" to "()[I",
            "getIndex" to "()I",
            "getBlockStartIndex" to "()I",
            "getBlockEndIndex" to "()I",
            "getType" to "()I",
            "getOldValue" to "()Ljava/lang/Object;",
            "getNewValue" to "()Ljava/lang/Object;",
            "getBlocksRemaining" to "()I",
            "toString" to "()Ljava/lang/String;",
        )
        for ((name, descriptor) in directMethods) {
            val method = treeEventClass.getDeclaredMethod(name)
            assertTrue(Modifier.isPublic(method.modifiers), name)
            assertEquals(descriptor, descriptorOf(method), name)
        }
    }

    private fun assertFieldModifiers(field: Field) {
        assertTrue(Modifier.isPublic(field.modifiers))
        assertTrue(Modifier.isStatic(field.modifiers))
        assertTrue(Modifier.isFinal(field.modifiers))
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

    private fun descriptorOf(type: Class<*>): String =
        when {
            type.isPrimitive -> primitiveDescriptors.getValue(type)
            type.isArray -> type.name.replace('.', '/')
            else -> "L${type.name.replace('.', '/')};"
        }

    private class MutableListEvent<E>(sourceList: EventList<E>) : ListEvent<E>(sourceList) {
        fun sourceListFromGetter(): EventList<E> = getSourceList()

        fun replaceSourceList(replacement: EventList<E>) {
            sourceList = replacement
        }

        fun replaceEventObjectSource(replacement: EventList<E>) {
            source = replacement
        }

        override fun copy(): ListEvent<E> = this

        override fun reset() = Unit

        override fun next(): Boolean = false

        override fun hasNext(): Boolean = false

        override fun nextBlock(): Boolean = false

        override fun isReordering(): Boolean = false

        override fun getReorderMap(): IntArray = IntArray(0)

        override fun getIndex(): Int = 0

        override fun getBlockStartIndex(): Int = 0

        override fun getBlockEndIndex(): Int = 0

        override fun getType(): Int = UPDATE

        override fun getOldValue(): E? = null

        override fun getNewValue(): E? = null

        override fun getBlocksRemaining(): Int = 0

        override fun toString(): String = "MutableListEvent"
    }

    private companion object {
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
