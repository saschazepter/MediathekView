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

package mediathek.tool.notification

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class NotificationServiceTest {
    @AfterEach
    fun closeNotificationService() {
        NotificationService.close()
    }

    @Test
    fun `reconfiguration waits for notification delivery to finish`() {
        val deliveryStarted = CountDownLatch(1)
        val releaseDelivery = CountDownLatch(1)
        val previousCenterClosed = CountDownLatch(1)
        val reconfigurationStarted = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        val previousCenter = object : INotificationCenter {
            override fun displayNotification(msg: NotificationMessage) {
                deliveryStarted.countDown()
                check(releaseDelivery.await(5, TimeUnit.SECONDS)) { "Notification delivery was not released" }
            }

            override fun close() {
                previousCenterClosed.countDown()
            }
        }

        try {
            NotificationService.configure({ previousCenter }, true)

            val delivery = executor.submit {
                NotificationService.displayNotification(notificationMessage())
            }
            assertTrue(deliveryStarted.await(5, TimeUnit.SECONDS), "Notification delivery did not start")

            val reconfiguration = executor.submit {
                reconfigurationStarted.countDown()
                NotificationService.configure({ NullNotificationCenter() }, true)
            }

            assertTrue(reconfigurationStarted.await(5, TimeUnit.SECONDS), "Reconfiguration did not start")
            assertFalse(
                previousCenterClosed.await(250, TimeUnit.MILLISECONDS),
                "The active notification center was closed during delivery",
            )

            releaseDelivery.countDown()
            delivery.get(5, TimeUnit.SECONDS)
            reconfiguration.get(5, TimeUnit.SECONDS)

            assertTrue(previousCenterClosed.await(5, TimeUnit.SECONDS), "Previous notification center was not closed")
        } finally {
            releaseDelivery.countDown()
            executor.shutdownNow()
        }
    }

    private fun notificationMessage(): NotificationMessage =
        NotificationMessage().apply {
            title = "Title"
            message = "Message"
            type = MessageType.INFO
        }
}
