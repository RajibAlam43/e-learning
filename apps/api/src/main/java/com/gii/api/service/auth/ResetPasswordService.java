package com.gii.api.service.auth;

import com.gii.common.entity.user.PasswordResetToken;
import com.gii.common.entity.user.User;
import com.gii.common.repository.user.PasswordResetTokenRepository;
import com.gii.common.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class ResetPasswordService {

    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public void execute(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        // 1. Check expiry
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Token expired");
        }

        // 2. Check already used
        if (resetToken.getUsedAt() != null) {
            throw new RuntimeException("Token already used");
        }

        User user = resetToken.getUser();

        // 3. Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 4. Mark token as used
        resetToken.setUsedAt(Instant.now());
        tokenRepository.save(resetToken);
    }
}
