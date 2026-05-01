package com.gii.api.service.auth;

import com.gii.api.model.request.auth.ForgotPasswordRequest;
import com.gii.api.service.util.IdentifierNormalizationUtil;
import com.gii.common.entity.user.User;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.enums.VerificationPurpose;
import com.gii.common.repository.user.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ForgotPasswordService {

  private final UserRepository userRepository;
  private final VerificationCodeService verificationCodeService;

  public void execute(ForgotPasswordRequest request) {
    String identifier =
        IdentifierNormalizationUtil.normalizeIdentifier(request.channel(), request.identifier());

    Optional<User> userOpt = findUserByChannel(request.channel(), identifier);
    if (userOpt.isEmpty()) {
      // Keep response identical to prevent account enumeration.
      return;
    }

    User user = userOpt.get();
    verificationCodeService.generateAndSend(
        user.getId(), VerificationPurpose.PASSWORD_RESET, request.channel(), identifier);
  }

  private Optional<User> findUserByChannel(VerificationChannel channel, String identifier) {
    return switch (channel) {
      case EMAIL -> userRepository.findByEmail(identifier.toLowerCase());
      case PHONE -> userRepository.findByPhone(identifier);
    };
  }
}
