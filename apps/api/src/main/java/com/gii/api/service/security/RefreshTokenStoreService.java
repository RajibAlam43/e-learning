package com.gii.api.service.security;

import com.gii.common.entity.user.RefreshToken;
import com.gii.common.entity.user.User;
import com.gii.common.repository.user.RefreshTokenRepository;
import com.gii.common.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenStoreService {

    private final RefreshTokenRepository repository;
    private final UserRepository userRepository;

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    public User validateAndRotate(String rawToken) {

        String hash = hash(rawToken);

        RefreshToken token = repository.findByTokenHash(hash)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (token.getRevokedAt() != null) {
            throw new RuntimeException("Token revoked");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Token expired");
        }

        // rotate (revoke old)
        token.setRevokedAt(Instant.now());
        repository.save(token);

        return token.getUser();
    }

    private String hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed");
        }
    }

    public String createRefreshToken(User user) {
        return createRefreshToken(user.getEmail());
    }

    public String createRefreshToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String rawToken = UUID.randomUUID().toString();
        String hash = hash(rawToken);

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .expiresAt(Instant.now().plus(REFRESH_TOKEN_TTL))
                .build();

        repository.save(token);

        return rawToken;
    }
}
