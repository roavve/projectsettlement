package com.example.mssqll.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class TokenBlacklistService {

    private final ConcurrentMap<String, Date> blacklist = new ConcurrentHashMap<>();

    /**
     * Adds a token to the blacklist with its expiration date.
     *
     * @param token        The JWT token to blacklist.
     * @param expiryDate   The expiration date of the token.
     */
    public void blacklistToken(String token, Date expiryDate) {
        log.info("Adding token to blacklist (by {})", getCurrentUsername());
        log.debug("Token expiry date: {}, Current blacklist size: {} (by {})", expiryDate, blacklist.size(), getCurrentUsername());

        blacklist.put(token, expiryDate);
        log.info("Token successfully blacklisted. New blacklist size: {} (by {})", blacklist.size(), getCurrentUsername());
    }

    /**
     * Checks if a token is blacklisted.
     *
     * @param token The JWT token to check.
     * @return True if the token is blacklisted, false otherwise.
     */
    public boolean isTokenBlacklisted(String token) {
        log.debug("Checking if token is blacklisted (by {})", getCurrentUsername());
        boolean isBlacklisted = blacklist.containsKey(token);

        if (isBlacklisted) {
            log.warn("Token found in blacklist (by {})", getCurrentUsername());
        } else {
            log.debug("Token not found in blacklist (by {})", getCurrentUsername());
        }

        return isBlacklisted;
    }

    /**
     * Scheduled task to remove expired tokens from the blacklist every hour.
     * This prevents the blacklist from growing indefinitely.
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // every hour
    public void removeExpiredTokens() {
        log.info("Starting scheduled task to remove expired tokens from blacklist (by {})", getCurrentUsername());
        int initialSize = blacklist.size();
        log.debug("Current blacklist size: {} (by {})", initialSize, getCurrentUsername());

        Date now = new Date();
        blacklist.entrySet().removeIf(entry -> entry.getValue().before(now));

        int finalSize = blacklist.size();
        int removedCount = initialSize - finalSize;
        log.info("Expired token cleanup completed. Removed {} tokens, Remaining tokens: {} (by {})", removedCount, finalSize, getCurrentUsername());
    }
}