package com.gii.api.service.auth;

import com.gii.api.service.SqsProducerService;
import com.gii.api.service.security.TokenHashService;
import com.gii.common.entity.user.PasswordResetToken;
import com.gii.common.entity.user.User;
import com.gii.common.repository.user.PasswordResetTokenRepository;
import com.gii.common.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ForgotPasswordService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final SqsProducerService sqsProducerService;
    private final TokenHashService tokenHashService;

    public void execute(String email) {
        // Find user
        Optional<User> userOpt = userRepository.findByEmail(email);

        // Short circuit if user doesn't exist
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();

        // Generate token
        String token = UUID.randomUUID().toString();

        // Save Token
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHashService.hash(token))
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();
        tokenRepository.save(resetToken);

        // TODO: Send email via worker
        sqsProducerService.sendMessage("", "", null);
    }
}
