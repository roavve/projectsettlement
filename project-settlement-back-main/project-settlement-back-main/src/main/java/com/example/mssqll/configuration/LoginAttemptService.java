package com.example.mssqll.configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 10;
    private static final long LOCK_DURATION_MINUTES = 15;
    private static final long BASE_DELAY_MS = 300;
    private static final long MAX_DELAY_MS = 2000;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    @Data
    private static class Attempt {
        private int failedAttempts;
        private Instant lockedUntil;
    }

    public boolean isLocked(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt == null || attempt.getLockedUntil() == null) {
            return false;
        }

        if (attempt.getLockedUntil().isAfter(Instant.now())) {
            return true;
        }

        attempt.setLockedUntil(null);
        attempt.setFailedAttempts(0);
        return false;
    }

    public void loginFailed(String key) {
        Attempt attempt = attempts.computeIfAbsent(key, k -> new Attempt());
        int failed = attempt.getFailedAttempts() + 1;
        attempt.setFailedAttempts(failed);

        if (failed >= MAX_FAILED_ATTEMPTS) {
            attempt.setLockedUntil(Instant.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES));
            attempt.setFailedAttempts(0);
            log.warn("Account [{}] locked until {}", key, attempt.getLockedUntil());
        } else {
            log.warn("Failed login attempt {} for [{}]", failed, key);
        }
    }

    public void loginSucceeded(String key) {
        attempts.remove(key);
    }

    public long getDelayMs(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt == null) return 0L;

        long delay = (long) attempt.getFailedAttempts() * BASE_DELAY_MS;
        return Math.min(delay, MAX_DELAY_MS);
    }
}

