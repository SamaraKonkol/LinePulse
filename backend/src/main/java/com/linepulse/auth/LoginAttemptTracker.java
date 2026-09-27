package com.linepulse.auth;

import com.linepulse.common.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class LoginAttemptTracker {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Map<String, Deque<Instant>> FAILURES = new ConcurrentHashMap<>();

    private LoginAttemptTracker() {
    }

    static void checkAllowed(String registration) {
        Deque<Instant> attempts = FAILURES.get(registration);
        if (attempts == null) return;
        synchronized (attempts) {
            evictExpired(attempts, Instant.now());
            if (attempts.size() >= MAX_FAILURES) {
                throw new TooManyRequestsException("Muitas tentativas de login. Tente novamente em alguns minutos.");
            }
            if (attempts.isEmpty()) FAILURES.remove(registration, attempts);
        }
    }

    static void recordFailure(String registration) {
        Deque<Instant> attempts = FAILURES.computeIfAbsent(registration, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            Instant now = Instant.now();
            evictExpired(attempts, now);
            attempts.addLast(now);
        }
    }

    static void recordSuccess(String registration) {
        FAILURES.remove(registration);
    }

    static void clearForTests(String registration) {
        FAILURES.remove(registration);
    }

    private static void evictExpired(Deque<Instant> attempts, Instant now) {
        Instant cutoff = now.minus(WINDOW);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
            attempts.removeFirst();
        }
    }
}
