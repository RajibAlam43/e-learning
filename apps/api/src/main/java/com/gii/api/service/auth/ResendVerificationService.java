package com.gii.api.service.auth;

import com.gii.api.service.SqsProducerService;
import com.gii.common.entity.user.EmailVerificationToken;
import com.gii.common.entity.user.User;
import com.gii.common.repository.user.EmailVerificationTokenRepository;
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
public class ResendVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final SqsProducerService sqsProducerService;

    public void execute(String email) {

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();

//        if (user.getEmailVerified()) {
//            return;
//        }

        String token = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                //.token(token)
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .build();

        tokenRepository.save(verificationToken);

        // TODO
        sqsProducerService.sendMessage("", "", null);
    }
}
