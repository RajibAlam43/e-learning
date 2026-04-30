package com.gii.api.service.auth;

import com.gii.api.model.request.auth.SendVerificationRequest;
import com.gii.common.entity.user.User;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.enums.VerificationPurpose;
import com.gii.common.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

import static com.gii.api.service.util.IdentifierNormalizationUtil.normalizeIdentifier;

@Service
@RequiredArgsConstructor
@Transactional
public class SendVerificationService {

    private final UserRepository userRepository;
    private final VerificationCodeService verificationCodeService;

    public void execute(SendVerificationRequest request) {
        String normalizedIdentifier = normalizeIdentifier(request.channel(), request.identifier());
        Optional<User> userOpt = findUserByChannel(request.channel(), normalizedIdentifier);

        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.orElseThrow();

        if (request.purpose() == VerificationPurpose.EMAIL_VERIFICATION && request.channel() != VerificationChannel.EMAIL) {
            throw new RuntimeException("Invalid verification request");
        }
        if (request.purpose() == VerificationPurpose.PHONE_VERIFICATION && request.channel() != VerificationChannel.PHONE) {
            throw new RuntimeException("Invalid verification request");
        }

        if (request.purpose() == VerificationPurpose.EMAIL_VERIFICATION && user.getEmailVerifiedAt() != null) {
            return;
        }
        if (request.purpose() == VerificationPurpose.PHONE_VERIFICATION && user.getPhoneVerifiedAt() != null) {
            return;
        }

        verificationCodeService.generateAndSend(user.getId(), request.purpose(), request.channel(), normalizedIdentifier);
    }

    private Optional<User> findUserByChannel(VerificationChannel channel, String normalizedIdentifier) {
        return switch (channel) {
            case EMAIL -> userRepository.findByEmail(normalizedIdentifier);
            case PHONE -> userRepository.findByPhone(normalizedIdentifier);
        };
    }
}
