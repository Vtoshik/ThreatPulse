package com.threatpulse.analyzer;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses a fake clock whose time moves forward when the fake sleeper "sleeps",
 * so the tests run instantly but behave like real waiting.
 */
public class RequestThrottleTest {
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private long nowNanos = 0;
    private final List<Long> sleeps = new ArrayList<>();

    private RequestThrottle throttle(int requestsPerMinute) {
        return new RequestThrottle(requestsPerMinute, () -> nowNanos, millis -> {
            sleeps.add(millis);
            nowNanos += millis * NANOS_PER_MILLI;
        });
    }

    private void passTime(long millis) {
        nowNanos += millis * NANOS_PER_MILLI;
    }

    @Test
    void acquire_shouldNotWait_forTheFirstCall() {
        RequestThrottle throttle = throttle(12);

        throttle.acquire();

        assertThat(sleeps).isEmpty();
    }

    @Test
    void acquire_shouldSpaceCallsEvenly_whenCalledInABurst() {
        // 12 per minute means one call every 5 seconds
        RequestThrottle throttle = throttle(12);

        throttle.acquire();
        throttle.acquire();
        throttle.acquire();

        assertThat(sleeps).containsExactly(5000L, 5000L);
    }

    @Test
    void acquire_shouldOnlyWaitForTheRestOfTheInterval() {
        RequestThrottle throttle = throttle(12);

        throttle.acquire();
        passTime(3000);
        throttle.acquire();

        // 3 seconds of the 5 second interval already passed
        assertThat(sleeps).containsExactly(2000L);
    }

    @Test
    void acquire_shouldNotWait_whenTheIntervalHasPassed() {
        RequestThrottle throttle = throttle(12);

        throttle.acquire();
        passTime(10_000);
        throttle.acquire();

        assertThat(sleeps).isEmpty();
    }

    @Test
    void acquire_shouldNotCountIdleTimeAsSavedCapacity() {
        // After a long pause the next calls must still be spaced, not sent as one burst
        RequestThrottle throttle = throttle(12);

        throttle.acquire();
        passTime(60_000);
        throttle.acquire();
        throttle.acquire();

        assertThat(sleeps).containsExactly(5000L);
    }

    @Test
    void acquire_shouldNeverWait_whenThrottleIsOff() {
        RequestThrottle throttle = throttle(0);

        throttle.acquire();
        throttle.acquire();
        throttle.acquire();

        assertThat(sleeps).isEmpty();
    }

    @Test
    void acquire_shouldKeepTheInterruptFlag_whenInterruptedWhileWaiting() {
        RequestThrottle throttle = new RequestThrottle(12, () -> nowNanos, millis -> {
            throw new InterruptedException();
        });

        throttle.acquire();
        throttle.acquire();

        assertThat(Thread.interrupted()).isTrue();
    }
}
