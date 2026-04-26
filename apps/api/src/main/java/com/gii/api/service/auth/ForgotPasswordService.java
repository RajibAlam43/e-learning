package com.gii.api.service.auth;

import com.gii.api.service.SqsProducerService;
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

    public void execute(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        // 🔒 Important: do NOT reveal if email exists
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();

        // 1. Generate token
        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                //.token(token)
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        tokenRepository.save(resetToken);

        // 2. Send email (implement later)
        sqsProducerService.sendMessage("", "", null);
    }
}
