package com.gii.api.service.auth;

import com.gii.common.entity.user.EmailVerificationToken;
import com.gii.common.entity.user.User;
import com.gii.common.repository.user.EmailVerificationTokenRepository;
import com.gii.common.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class VerifyService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    public void execute(String token) {

        EmailVerificationToken verificationToken = tokenRepository.findByTokenHash(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Token expired");
        }

        if (verificationToken.getUsedAt() != null) {
            throw new RuntimeException("Token already used");
        }

        User user = verificationToken.getUser();

        user.setEmailVerifiedAt(Instant.now());
        userRepository.save(user);

        verificationToken.setUsedAt(Instant.now());
        tokenRepository.save(verificationToken);
    }
}
