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

package mediathek.controller;

import java.util.concurrent.locks.LockSupport;

/**
 * Lightweight byte-based rate limiter using a monotonic clock.
 * Uses a smooth-bursty model similar to Guava's RateLimiter so small scheduler differences
 * between operating systems do not dominate the effective throughput.
 */
public final class ByteRateLimiter {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final long UNLIMITED_THRESHOLD = Long.MAX_VALUE / 2;
    private static final double MAX_BURST_SECONDS = 1.0d;

    private volatile long bytesPerSecond;
    private volatile boolean unlimited;
    private double storedPermits;
    private double maxPermits;
    private double stableNanosPerPermit;
    private long nextAvailableNanos;

    public ByteRateLimiter(long bytesPerSecond) {
        setRate(bytesPerSecond);
    }

    private static long nanosForPermits(double permits, double nanosPerPermit) {
        final double nanos = permits * nanosPerPermit;
        if (!Double.isFinite(nanos) || nanos >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, (long) Math.ceil(nanos));
    }

    private static long safeAdd(long a, long b) {
        if (Long.MAX_VALUE - a < b) {
            return Long.MAX_VALUE;
        }
        return a + b;
    }

    public void setRate(long bytesPerSecond) {
        synchronized (this) {
            final long now = System.nanoTime();
            this.unlimited = bytesPerSecond >= UNLIMITED_THRESHOLD;
            this.bytesPerSecond = bytesPerSecond <= 0 ? 1 : bytesPerSecond;
            if (unlimited) {
                storedPermits = 0d;
                maxPermits = 0d;
                stableNanosPerPermit = 0d;
                nextAvailableNanos = 0L;
            }
            else {
                resync(now);

                final double oldMaxPermits = maxPermits;
                stableNanosPerPermit = NANOS_PER_SECOND / (double) this.bytesPerSecond;
                maxPermits = Math.max(1d, this.bytesPerSecond * MAX_BURST_SECONDS);

                if (oldMaxPermits <= 0d || !Double.isFinite(oldMaxPermits)) {
                    storedPermits = Math.min(storedPermits, maxPermits);
                }
                else {
                    storedPermits = Math.min(maxPermits, storedPermits * maxPermits / oldMaxPermits);
                }
            }
        }
    }

    public void acquire(int permits, long operationStartedNanos, long operationCompletedNanos) {
        if (permits <= 0) {
            return;
        }

        final long waitNanos;
        synchronized (this) {
            if (unlimited) {
                return;
            }
            final long now = Math.max(operationStartedNanos, operationCompletedNanos);
            waitNanos = reserveAndGetWaitLength(permits, now);
        }

        if (waitNanos > 0) {
            LockSupport.parkNanos(waitNanos);
        }
    }

    private long reserveAndGetWaitLength(int permits, long nowNanos) {
        resync(nowNanos);

        final long momentAvailable = nextAvailableNanos;
        final double permitsToSpendFromStorage = Math.min(permits, storedPermits);
        final double freshPermits = permits - permitsToSpendFromStorage;
        final long waitNanos = nanosForPermits(freshPermits, stableNanosPerPermit);

        nextAvailableNanos = safeAdd(nextAvailableNanos, waitNanos);
        storedPermits -= permitsToSpendFromStorage;

        return Math.max(momentAvailable - nowNanos, 0L);
    }

    private void resync(long nowNanos) {
        if (nowNanos <= nextAvailableNanos) {
            return;
        }
        if (stableNanosPerPermit <= 0d) {
            nextAvailableNanos = nowNanos;
            return;
        }

        final double newPermits = (nowNanos - nextAvailableNanos) / stableNanosPerPermit;
        storedPermits = Math.min(maxPermits, storedPermits + newPermits);
        nextAvailableNanos = nowNanos;
    }
}
