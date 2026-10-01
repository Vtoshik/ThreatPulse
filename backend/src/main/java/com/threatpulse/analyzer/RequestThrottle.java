package com.threatpulse.analyzer;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Spaces out calls so they never go faster than a given number per minute.
 * <p>
 * Instead of letting a burst through and getting HTTP 429 back, every caller waits for its
 * turn: with 12 requests per minute, two calls start at least 5 seconds apart. The calls are
 * handed out one at a time, so the limit holds even when many threads share one throttle.
 * <p>
 * The clock and the sleep are injected so tests do not have to wait in real time.
 */
public class RequestThrottle {

    /** Sleeping is a separate interface so tests can replace it. */
    @FunctionalInterface
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private final long minIntervalNanos;
    private final LongSupplier nanoClock;
    private final Sleeper sleeper;

    // Both fields below are only read and written inside acquire(), which is synchronized
    private boolean started = false;
    private long nextAllowedNanos;

    /**
     * @param requestsPerMinute allowed calls per minute, zero or less switches the throttle off
     */
    public RequestThrottle(int requestsPerMinute) {
        this(requestsPerMinute, System::nanoTime, Thread::sleep);
    }

    RequestThrottle(int requestsPerMinute, LongSupplier nanoClock, Sleeper sleeper) {
        this.minIntervalNanos = requestsPerMinute > 0
                ? TimeUnit.MINUTES.toNanos(1) / requestsPerMinute
                : 0;
        this.nanoClock = nanoClock;
        this.sleeper = sleeper;
    }

    /**
     * Blocks until the caller is allowed to make the next call.
     * The first call never waits.
     */
    public synchronized void acquire() {
        if (minIntervalNanos == 0) {
            return;
        }

        long now = nanoClock.getAsLong();
        // Subtract instead of comparing: System.nanoTime() values may overflow or be negative
        long waitNanos = started ? Math.max(0, nextAllowedNanos - now) : 0;

        if (waitNanos > 0) {
            try {
                sleeper.sleep(TimeUnit.NANOSECONDS.toMillis(waitNanos));
            } catch (InterruptedException e) {
                // Keep the interrupt for the caller (for example during shutdown) and stop waiting
                Thread.currentThread().interrupt();
                return;
            }
        }

        started = true;
        nextAllowedNanos = now + waitNanos + minIntervalNanos;
    }
}
