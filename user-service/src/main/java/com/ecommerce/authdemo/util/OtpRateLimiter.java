package com.ecommerce.authdemo.util;

import com.ecommerce.authdemo.exception.TooManyRequestsException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight in-memory OTP throttling (single-instance local/dev friendly):
 * <ul>
 *   <li>Resend cooldown: at most one OTP per identifier every {@code RESEND_COOLDOWN}.</li>
 *   <li>Hourly cap: at most {@code MAX_PER_WINDOW} OTP generations per identifier per hour.</li>
 * </ul>
 * Throws {@link TooManyRequestsException} (HTTP 429) when a limit is hit.
 */
@Component
public class OtpRateLimiter {

    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration WINDOW = Duration.ofHours(1);
    private static final int MAX_PER_WINDOW = 5;

    private final Map<String, Deque<Instant>> sendHistory = new ConcurrentHashMap<>();

    /** Call before generating/sending an OTP. Throws when a limit is exceeded. */
    public synchronized void assertCanSend(String identifier) {
        String key = normalize(identifier);
        Instant now = Instant.now();
        Deque<Instant> history = sendHistory.computeIfAbsent(key, k -> new ArrayDeque<>());

        // Drop entries outside the rolling window.
        while (!history.isEmpty() && Duration.between(history.peekFirst(), now).compareTo(WINDOW) > 0) {
            history.pollFirst();
        }

        Instant last = history.peekLast();
        if (last != null && Duration.between(last, now).compareTo(RESEND_COOLDOWN) < 0) {
            long wait = RESEND_COOLDOWN.getSeconds() - Duration.between(last, now).getSeconds();
            throw new TooManyRequestsException(
                    "Please wait " + Math.max(wait, 1) + "s before requesting another OTP.");
        }

        if (history.size() >= MAX_PER_WINDOW) {
            throw new TooManyRequestsException(
                    "Too many OTP requests. Please try again later.");
        }

        history.addLast(now);
    }

    /** Clears throttle state for an identifier (e.g. after successful verification). */
    public synchronized void reset(String identifier) {
        sendHistory.remove(normalize(identifier));
    }

    private String normalize(String identifier) {
        return String.valueOf(identifier).trim().toLowerCase();
    }
}
