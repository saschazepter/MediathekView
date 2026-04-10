package mediathek.tool.notification

import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.lang.foreign.*
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.nio.charset.StandardCharsets

class MacNotificationCenter : INotificationCenter {
    override fun displayNotification(msg: NotificationMessage) {
        UserNotifications.show(msg.title, msg.message)
    }

    @Throws(IOException::class)
    override fun close() {
    }

    private object UserNotifications {
        private const val BLOCK_HAS_SIGNATURE = 1 shl 30
        private const val UN_AUTHORIZATION_OPTION_SOUND = 1L shl 1
        private const val UN_AUTHORIZATION_OPTION_ALERT = 1L shl 2
        private const val UN_NOTIFICATION_PRESENTATION_OPTION_SOUND = 1L shl 1
        private const val UN_NOTIFICATION_PRESENTATION_OPTION_ALERT = 1L shl 2
        private const val UN_NOTIFICATION_PRESENTATION_OPTION_LIST = 1L shl 3
        private const val UN_NOTIFICATION_PRESENTATION_OPTION_BANNER = 1L shl 4
        private const val POINTER_SIZE = 8L
        private const val MEDIATHEK_VIEW_DELEGATE_CLASS = "MediathekViewUserNotificationDelegate"

        private val logger = LogManager.getLogger()
        private val arena = Arena.global()
        private val linker = Linker.nativeLinker()
        private val methodHandles = MethodHandles.lookup()
        private val lookup = SymbolLookup.libraryLookup("/usr/lib/libobjc.dylib", arena)
            .or(SymbolLookup.libraryLookup("/System/Library/Frameworks/Foundation.framework/Foundation", arena))
            .or(SymbolLookup.libraryLookup("/System/Library/Frameworks/UserNotifications.framework/UserNotifications", arena))
            .or(linker.defaultLookup())
        private val msgSendPointer: MemorySegment = lookup.findOrThrow("objc_msgSend")
        private val getClass: MethodHandle = downcallPointer("objc_getClass", ValueLayout.ADDRESS)
        private val registerSelector: MethodHandle = downcallPointer("sel_registerName", ValueLayout.ADDRESS)
        private val allocateClassPair: MethodHandle = downcallPointer(
            "objc_allocateClassPair",
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG
        )
        private val registerClassPair: MethodHandle = downcallVoid("objc_registerClassPair", ValueLayout.ADDRESS)
        private val addMethod: MethodHandle = downcall(
            lookup.findOrThrow("class_addMethod"),
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS
            )
        )
        private val globalBlockClass: MemorySegment = lookup.findOrThrow("_NSConcreteGlobalBlock")
        private val authorizationBlock = ObjcBlock(
            upcall(
                "authorizationCallback",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS),
                MemorySegment::class.java,
                java.lang.Boolean.TYPE,
                MemorySegment::class.java
            ),
            "v@?B@"
        )
        private val foregroundNotificationDelegate = createForegroundNotificationDelegate()

        private val pendingNotifications = mutableListOf<Pair<String, String>>()
        private var authorizationRequestInFlight = false
        private var authorizationGranted = false
        private var deprecatedFallbackLogged = false

        @Suppress("unused")
        @JvmStatic
        private fun authorizationCallback(_block: MemorySegment, granted: Boolean, _error: MemorySegment) {
            if (authorizationCompleted(granted)) {
                deliverPending()
            } else {
                clearPending()
            }
        }

        @Suppress("unused")
        @JvmStatic
        private fun willPresentCallback(
            _self: MemorySegment,
            _command: MemorySegment,
            _center: MemorySegment,
            _notification: MemorySegment,
            completionHandler: MemorySegment
        ) {
            val invoke = completionHandler.get(ValueLayout.ADDRESS, POINTER_SIZE + 8)
            val presentationOptions = UN_NOTIFICATION_PRESENTATION_OPTION_SOUND or
                    UN_NOTIFICATION_PRESENTATION_OPTION_ALERT or
                    UN_NOTIFICATION_PRESENTATION_OPTION_LIST or
                    UN_NOTIFICATION_PRESENTATION_OPTION_BANNER
            val invokeCompletion = downcall(
                invoke,
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
            )

            invokeCompletion.invokeExact(completionHandler, presentationOptions)
        }

        @Synchronized
        fun show(title: String, body: String) {
            if (!isRunningFromAppBundle()) {
                logDeprecatedFallback()
                deliverDeprecated(title, body)
                return
            }

            pendingNotifications += title to body

            if (authorizationGranted) {
                deliverPending()
            } else if (!authorizationRequestInFlight) {
                authorizationRequestInFlight = true
                requestAuthorization()
            }
        }

        private fun isRunningFromAppBundle(): Boolean {
            val mainBundle = msgPtr(cls("NSBundle"), "mainBundle")
            val bundlePath = nsStringToString(msgPtr(mainBundle, "bundlePath")) ?: return false

            return bundlePath.endsWith(".app") || bundlePath.contains(".app/")
        }

        private fun logDeprecatedFallback() {
            if (!deprecatedFallbackLogged) {
                deprecatedFallbackLogged = true
                logger.warn(
                    "macOS UserNotifications are only available when MediathekView is launched from an application bundle; using deprecated NSUserNotification fallback"
                )
            }
        }

        private fun requestAuthorization() {
            val center = msgPtr(cls("UNUserNotificationCenter"), "currentNotificationCenter")
            msgVoid(center, "setDelegate:", foregroundNotificationDelegate)
            msgVoid(
                center,
                "requestAuthorizationWithOptions:completionHandler:",
                UN_AUTHORIZATION_OPTION_ALERT or UN_AUTHORIZATION_OPTION_SOUND,
                authorizationBlock.pointer
            )
        }

        @Synchronized
        private fun authorizationCompleted(granted: Boolean): Boolean {
            authorizationRequestInFlight = false
            authorizationGranted = granted
            return granted
        }

        @Synchronized
        private fun deliverPending() {
            val notifications = pendingNotifications.toList()
            pendingNotifications.clear()

            notifications.forEach { (title, body) ->
                deliverModern(title, body)
            }
        }

        @Synchronized
        private fun clearPending() {
            pendingNotifications.clear()
        }

        private fun deliverModern(title: String, body: String) {
            val pool = msgPtr(msgPtr(cls("NSAutoreleasePool"), "alloc"), "init")

            try {
                val content = msgPtr(msgPtr(cls("UNMutableNotificationContent"), "alloc"), "init")
                msgVoid(content, "setTitle:", nsString(title))
                msgVoid(content, "setBody:", nsString(body))
                msgVoid(content, "setSound:", msgPtr(cls("UNNotificationSound"), "defaultSound"))

                val request = msgPtr(
                    cls("UNNotificationRequest"),
                    "requestWithIdentifier:content:trigger:",
                    nsString("mediathekview-${System.nanoTime()}"),
                    content,
                    MemorySegment.NULL
                )

                val center = msgPtr(cls("UNUserNotificationCenter"), "currentNotificationCenter")
                msgVoid(center, "setDelegate:", foregroundNotificationDelegate)
                msgVoid(center, "addNotificationRequest:withCompletionHandler:", request, MemorySegment.NULL)
            } finally {
                msgVoid(pool, "drain")
            }
        }

        private fun deliverDeprecated(title: String, body: String) {
            val pool = msgPtr(msgPtr(cls("NSAutoreleasePool"), "alloc"), "init")

            try {
                val notification = msgPtr(msgPtr(cls("NSUserNotification"), "alloc"), "init")
                msgVoid(notification, "setTitle:", nsString(title))
                msgVoid(notification, "setInformativeText:", nsString(body))
                msgVoid(notification, "setHasReplyButton:", false)

                val center = msgPtr(cls("NSUserNotificationCenter"), "defaultUserNotificationCenter")
                msgVoid(center, "deliverNotification:", notification)
            } finally {
                msgVoid(pool, "drain")
            }
        }

        private fun createForegroundNotificationDelegate(): MemorySegment {
            val delegateClass = allocateClassPair.invokeExact(
                cls("NSObject"),
                arena.allocateFrom(MEDIATHEK_VIEW_DELEGATE_CLASS, StandardCharsets.UTF_8),
                0L
            ) as MemorySegment
            val willPresentStub = upcall(
                "willPresentCallback",
                FunctionDescriptor.ofVoid(
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS
                ),
                MemorySegment::class.java,
                MemorySegment::class.java,
                MemorySegment::class.java,
                MemorySegment::class.java,
                MemorySegment::class.java
            )
            val added = addMethod.invokeExact(
                delegateClass,
                selector("userNotificationCenter:willPresentNotification:withCompletionHandler:"),
                willPresentStub,
                arena.allocateFrom("v@:@@@?", StandardCharsets.UTF_8)
            ) as Boolean
            check(added) { "Failed to register macOS notification presentation delegate method" }
            registerClassPair.invokeExact(delegateClass)

            return msgPtr(msgPtr(delegateClass, "alloc"), "init")
        }

        private fun cls(name: String): MemorySegment {
            return getClass.invokeExact(arena.allocateFrom(name, StandardCharsets.UTF_8)) as MemorySegment
        }

        private fun selector(name: String): MemorySegment {
            return registerSelector.invokeExact(arena.allocateFrom(name, StandardCharsets.UTF_8)) as MemorySegment
        }

        private fun nsString(value: String): MemorySegment {
            return msgPtr(cls("NSString"), "stringWithUTF8String:", arena.allocateFrom(value, StandardCharsets.UTF_8))
        }

        private fun nsStringToString(value: MemorySegment): String? {
            if (value == MemorySegment.NULL) {
                return null
            }

            val utf8String = msgPtr(value, "UTF8String")
            if (utf8String == MemorySegment.NULL) {
                return null
            }

            return utf8String.reinterpret(Long.MAX_VALUE).getString(0, StandardCharsets.UTF_8)
        }

        private fun msgPtr(receiver: MemorySegment, selector: String): MemorySegment {
            return downcall(
                msgSendPointer,
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            ).invokeExact(receiver, selector(selector)) as MemorySegment
        }

        private fun msgPtr(receiver: MemorySegment, selector: String, arg: MemorySegment): MemorySegment {
            return downcall(
                msgSendPointer,
                FunctionDescriptor.of(
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS
                )
            ).invokeExact(receiver, selector(selector), arg) as MemorySegment
        }

        private fun msgPtr(
            receiver: MemorySegment,
            selector: String,
            arg1: MemorySegment,
            arg2: MemorySegment,
            arg3: MemorySegment
        ): MemorySegment {
            return downcall(
                msgSendPointer,
                FunctionDescriptor.of(
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS
                )
            ).invokeExact(receiver, selector(selector), arg1, arg2, arg3) as MemorySegment
        }

        private fun msgVoid(receiver: MemorySegment, selector: String) {
            downcallVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
                .invokeExact(receiver, selector(selector))
        }

        private fun msgVoid(receiver: MemorySegment, selector: String, arg: MemorySegment) {
            downcallVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
                .invokeExact(receiver, selector(selector), arg)
        }

        private fun msgVoid(receiver: MemorySegment, selector: String, arg1: Long, arg2: MemorySegment) {
            downcallVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
                .invokeExact(receiver, selector(selector), arg1, arg2)
        }

        private fun msgVoid(receiver: MemorySegment, selector: String, arg1: MemorySegment, arg2: MemorySegment) {
            downcallVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
                .invokeExact(receiver, selector(selector), arg1, arg2)
        }

        private fun msgVoid(receiver: MemorySegment, selector: String, arg: Boolean) {
            downcallVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_BOOLEAN)
                .invokeExact(receiver, selector(selector), arg)
        }

        private fun downcallPointer(symbol: String, vararg args: ValueLayout): MethodHandle {
            return downcall(
                lookup.findOrThrow(symbol),
                FunctionDescriptor.of(ValueLayout.ADDRESS, *args)
            )
        }

        private fun downcallVoid(symbol: String, vararg args: ValueLayout): MethodHandle {
            return downcall(lookup.findOrThrow(symbol), FunctionDescriptor.ofVoid(*args))
        }

        private fun downcallVoid(vararg args: ValueLayout): MethodHandle {
            return downcall(msgSendPointer, FunctionDescriptor.ofVoid(*args))
        }

        private fun downcall(symbol: MemorySegment, descriptor: FunctionDescriptor): MethodHandle {
            return linker.downcallHandle(symbol, descriptor)
        }

        private fun upcall(methodName: String, descriptor: FunctionDescriptor, vararg parameterTypes: Class<*>): MemorySegment {
            val methodHandle = methodHandles.findStatic(
                UserNotifications::class.java,
                methodName,
                MethodType.methodType(Void.TYPE, parameterTypes.toList())
            )

            return linker.upcallStub(methodHandle, descriptor, arena)
        }

        private class ObjcBlock(invoke: MemorySegment, signature: String) {
            @Suppress("unused")
            private val signatureMemory: MemorySegment = arena.allocateFrom(signature, StandardCharsets.US_ASCII)
            private val descriptor: MemorySegment = arena.allocate(POINTER_SIZE * 3, POINTER_SIZE)
            private val block: MemorySegment

            val pointer: MemorySegment
                get() = block

            init {
                descriptor.set(ValueLayout.JAVA_LONG, 0, 0)
                descriptor.set(ValueLayout.JAVA_LONG, POINTER_SIZE, 0)
                descriptor.set(ValueLayout.ADDRESS, POINTER_SIZE * 2, signatureMemory)

                block = arena.allocate(POINTER_SIZE * 3 + 8, POINTER_SIZE)
                block.set(ValueLayout.ADDRESS, 0, globalBlockClass)
                block.set(ValueLayout.JAVA_INT, POINTER_SIZE, BLOCK_HAS_SIGNATURE)
                block.set(ValueLayout.JAVA_INT, POINTER_SIZE + 4, 0)
                block.set(ValueLayout.ADDRESS, POINTER_SIZE + 8, invoke)
                block.set(ValueLayout.ADDRESS, POINTER_SIZE * 2 + 8, descriptor)
            }
        }
    }
}
