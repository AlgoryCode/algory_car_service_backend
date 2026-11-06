package com.algorycarservice.auth.service;

import com.algorycarservice.auth.model.RefreshToken;
import com.algorycarservice.auth.repository.RefreshTokenRepository;
import com.algorycarservice.common.config.AppProperties;
import com.algorycarservice.common.exception.ApiException;
import com.algorycarservice.common.exception.ErrorCode;
import com.algorycarservice.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Issues and rotates refresh tokens. Each token works once; presenting an already used token is treated as
 * theft and revokes every session of that user.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final AppProperties properties;

    @Transactional
    public IssuedRefreshToken startSession(User user) {
        return issue(user, UUID.randomUUID().toString());
    }

    /** Consumes the token and issues its successor. Reuse detection commits even though the caller gets an error. */
    @Transactional(noRollbackFor = ApiException.class)
    public Rotation rotate(String token) {
        Instant now = Instant.now();
        RefreshToken stored = repository.findByTokenHash(hash(token))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (stored.getRevokedAt() != null) {
            repository.revokeAllForUser(stored.getUser().getId(), now);
        }
        if (!stored.isUsable(now) || !stored.getUser().isActive()) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        stored.setRevokedAt(now);
        return new Rotation(stored.getUser(), issue(stored.getUser(), stored.getSessionId()));
    }

    @Transactional
    public void revoke(String token) {
        repository.findByTokenHash(hash(token)).ifPresent(stored -> stored.setRevokedAt(Instant.now()));
    }

    @Transactional
    public void revokeAll(Long userId) {
        repository.revokeAllForUser(userId, Instant.now());
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void purgeExpired() {
        repository.deleteExpired(Instant.now());
    }

    private IssuedRefreshToken issue(User user, String sessionId) {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hash(token))
                .sessionId(sessionId)
                .expiresAt(Instant.now().plus(properties.jwt().refreshTokenTtl()))
                .build());
        return new IssuedRefreshToken(token, sessionId);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required by every Java runtime", ex);
        }
    }

    public record IssuedRefreshToken(String value, String sessionId) {
    }

    public record Rotation(User user, IssuedRefreshToken next) {
    }
}
