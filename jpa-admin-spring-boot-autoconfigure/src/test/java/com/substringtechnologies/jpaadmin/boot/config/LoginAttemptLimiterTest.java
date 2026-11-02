package com.substringtechnologies.jpaadmin.boot.config;

import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptLimiterTest {
    static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(3, Duration.ofMinutes(10), clock);

    private void fail(String user, String address, int times) {
        for (int i = 0; i < times; i++) limiter.recordFailure(user, address);
    }

    @Test
    void locksAfterMaxAttemptsAndUnlocksAfterTheDuration() {
        fail("admin", "1.1.1.1", 2);
        assertEquals(Duration.ZERO, limiter.lockedFor("admin", "1.1.1.1"));
        fail("Admin ", "1.1.1.1", 1);
        assertEquals(Duration.ofMinutes(10), limiter.lockedFor("admin", "1.1.1.1"), "usernames are matched case-insensitively");
        clock.now = clock.now.plus(Duration.ofMinutes(10));
        assertEquals(Duration.ZERO, limiter.lockedFor("admin", "1.1.1.1"));
    }

    @Test
    void failuresOutsideTheWindowAreForgotten() {
        fail("admin", "1.1.1.1", 2);
        clock.now = clock.now.plus(Duration.ofMinutes(11));
        fail("admin", "1.1.1.1", 2);
        assertEquals(Duration.ZERO, limiter.lockedFor("admin", "1.1.1.1"));
    }

    @Test
    void successClearsTheUsernameButNotTheAddressBudget() {
        fail("admin", "1.1.1.1", 2);
        limiter.recordSuccess("admin", "1.1.1.1");
        fail("admin", "1.1.1.1", 2);
        assertEquals(Duration.ZERO, limiter.lockedFor("admin", "1.1.1.1"));
    }

    @Test
    void oneAddressSprayingManyUsernamesIsLocked() {
        int budget = 3 * LoginAttemptLimiter.ADDRESS_MULTIPLIER;
        for (int i = 0; i < budget; i++) fail("user" + i, "6.6.6.6", 1);
        assertFalse(limiter.lockedFor("someone-new", "6.6.6.6").isZero());
        assertTrue(limiter.lockedFor("someone-new", "7.7.7.7").isZero());
    }

    @Test
    void zeroAttemptsDisablesThrottling() {
        var disabled = new LoginAttemptLimiter(0, Duration.ofMinutes(10), clock);
        for (int i = 0; i < 100; i++) disabled.recordFailure("admin", "1.1.1.1");
        assertEquals(Duration.ZERO, disabled.lockedFor("admin", "1.1.1.1"));
    }
}
