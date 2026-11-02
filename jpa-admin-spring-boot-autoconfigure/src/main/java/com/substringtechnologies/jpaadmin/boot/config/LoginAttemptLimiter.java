package com.substringtechnologies.jpaadmin.boot.config;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Locks sign-in after repeated failures. A username is locked only for the address that failed, so others can't lock
 * the real admin out; each address also has a larger overall budget that stops spraying across usernames.
 * Counts live in memory, per instance. Behind a proxy, set {@code server.forward-headers-strategy} so the client address is real.
 */
public class LoginAttemptLimiter {
    static final int ADDRESS_MULTIPLIER = 4;
    private static final int PURGE_THRESHOLD = 10_000;

    private final int maxAttempts;
    private final Duration lockDuration;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private static final class Window {
        int failures;
        Instant started;
        Instant lockedUntil;
        Window(Instant started) { this.started = started; }
    }

    public LoginAttemptLimiter(int maxAttempts, Duration lockDuration, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
        this.clock = clock;
    }

    public boolean enabled() { return maxAttempts > 0; }

    private List<String> keys(String username, String address) {
        String user = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return List.of("user:" + address + "|" + user, "address:" + address);
    }

    private int limit(String key) { return key.startsWith("address:") ? maxAttempts * ADDRESS_MULTIPLIER : maxAttempts; }

    /** Time left on the longest active lock, or {@link Duration#ZERO} when sign-in may proceed. */
    public Duration lockedFor(String username, String address) {
        if (!enabled()) return Duration.ZERO;
        Instant now = clock.instant();
        Duration longest = Duration.ZERO;
        for (String key : keys(username, address)) {
            Window window = windows.get(key);
            if (window == null) continue;
            synchronized (window) {
                if (window.lockedUntil != null && window.lockedUntil.isAfter(now)) {
                    Duration left = Duration.between(now, window.lockedUntil);
                    if (left.compareTo(longest) > 0) longest = left;
                }
            }
        }
        return longest;
    }

    public void recordFailure(String username, String address) {
        if (!enabled()) return;
        Instant now = clock.instant();
        if (windows.size() > PURGE_THRESHOLD) purgeExpired(now);
        for (String key : keys(username, address)) {
            Window window = windows.computeIfAbsent(key, k -> new Window(now));
            synchronized (window) {
                if (!window.started.plus(lockDuration).isAfter(now) && (window.lockedUntil == null || !window.lockedUntil.isAfter(now))) {
                    window.failures = 0;
                    window.started = now;
                    window.lockedUntil = null;
                }
                if (++window.failures >= limit(key)) window.lockedUntil = now.plus(lockDuration);
            }
        }
    }

    /** A successful sign-in clears that username's failures; the address budget keeps counting. */
    public void recordSuccess(String username, String address) {
        if (enabled()) windows.remove(keys(username, address).get(0));
    }

    private void purgeExpired(Instant now) {
        windows.entrySet().removeIf(entry -> {
            Window window = entry.getValue();
            synchronized (window) {
                Instant end = window.lockedUntil != null ? window.lockedUntil : window.started.plus(lockDuration);
                return !end.isAfter(now);
            }
        });
    }

    /** Sign-in refused because of earlier failures; answered with 429 and Retry-After. */
    public static class LockedException extends AuthenticationException {
        private final Duration retryAfter;
        public LockedException(Duration retryAfter) {
            super("Too many failed sign-in attempts");
            this.retryAfter = retryAfter;
        }
        public Duration getRetryAfter() { return retryAfter; }
    }

    /** Wraps the admin's authentication provider; the lock is checked before the password, so a locked user learns nothing. */
    public AuthenticationProvider guard(AuthenticationProvider delegate) {
        return new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String username = authentication.getName();
                String address = authentication.getDetails() instanceof WebAuthenticationDetails details && details.getRemoteAddress() != null
                        ? details.getRemoteAddress() : "unknown";
                Duration locked = lockedFor(username, address);
                if (!locked.isZero()) throw new LockedException(locked);
                try {
                    Authentication result = delegate.authenticate(authentication);
                    if (result != null) recordSuccess(username, address);
                    return result;
                } catch (BadCredentialsException | org.springframework.security.core.userdetails.UsernameNotFoundException e) {
                    recordFailure(username, address);
                    throw e;
                }
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return delegate.supports(authentication);
            }
        };
    }
}
