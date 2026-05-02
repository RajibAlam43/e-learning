package com.gii.api.service.me;

import com.gii.api.model.request.me.UpdateProfileRequest;
import com.gii.api.model.response.me.InstructorProfileResponse;
import com.gii.api.model.response.me.MeResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.user.InstructorProfile;
import com.gii.common.entity.user.User;
import com.gii.common.entity.user.UserProfile;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.repository.certificate.CertificateRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.live.LiveClassAttendanceRepository;
import com.gii.common.repository.user.InstructorProfileRepository;
import com.gii.common.repository.user.UserProfileRepository;
import com.gii.common.repository.user.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {

  private final CurrentUserService currentUserService;
  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final InstructorProfileRepository instructorProfileRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final CertificateRepository certificateRepository;
  private final LiveClassAttendanceRepository liveClassAttendanceRepository;

  @Transactional(readOnly = true)
  public MeResponse get(Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);
    InstructorProfile instructorProfile =
        instructorProfileRepository.findById(user.getId()).orElse(null);
    return toResponse(user, profile, instructorProfile);
  }

  public MeResponse update(UpdateProfileRequest request, Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);

    applyUserUpdates(user, request);
    userRepository.save(user);

    profile = upsertUserProfile(user, profile, request);

    // Instructor profile is updated only when instructor-related payload is present.
    boolean wantsInstructorUpdate = hasInstructorPayload(request);
    InstructorProfile instructorProfile =
        instructorProfileRepository.findById(user.getId()).orElse(null);
    if (wantsInstructorUpdate) {
      instructorProfile = upsertInstructorProfile(user, instructorProfile, request);
    }

    return toResponse(user, profile, instructorProfile);
  }

  private void applyUserUpdates(User user, UpdateProfileRequest request) {
    if (request.fullName() != null && !request.fullName().isBlank()) {
      user.setFullName(request.fullName().trim());
    }

    if (request.email() != null) {
      String normalizedEmail = normalizeEmail(request.email());
      if (!normalizedEmail.equals(user.getEmail())
          && userRepository.existsByEmail(normalizedEmail)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
      }
      if (!normalizedEmail.equals(user.getEmail())) {
        // Email change requires re-verification.
        user.setEmail(normalizedEmail);
        user.setEmailVerifiedAt(null);
      }
    }

    if (request.phone() != null) {
      String normalizedPhone = normalizePhone(request.phone());
      if (!normalizedPhone.equals(user.getPhone())
          && userRepository.existsByPhone(normalizedPhone)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already in use");
      }
      if (!normalizedPhone.equals(user.getPhone())) {
        // Phone change requires re-verification.
        user.setPhone(normalizedPhone);
        user.setPhoneVerifiedAt(null);
      }
    }

    if (request.phoneCountryCode() != null) {
      user.setPhoneCountryCode(normalizeCountryCode(request.phoneCountryCode()));
    }
  }

  private UserProfile upsertUserProfile(
      User user, UserProfile current, UpdateProfileRequest request) {
    UserProfile profile = current != null ? current : UserProfile.builder().user(user).build();

    if (request.avatarUrl() != null) {
      profile.setAvatarUrl(blankToNull(request.avatarUrl()));
    }
    if (request.locale() != null && !request.locale().isBlank()) {
      profile.setLocale(request.locale().trim());
    }
    if (request.timezone() != null) {
      profile.setTimezone(blankToNull(request.timezone()));
    }
    if (request.bio() != null) {
      profile.setBio(blankToNull(request.bio()));
    }

    if (profile.getLocale() == null || profile.getLocale().isBlank()) {
      profile.setLocale("bn-BD");
    }

    return userProfileRepository.save(profile);
  }

  private InstructorProfile upsertInstructorProfile(
      User user, InstructorProfile current, UpdateProfileRequest request) {
    InstructorProfile profile =
        current != null
            ? current
            : InstructorProfile.builder()
                .user(user)
                .displayName(user.getFullName())
                .isPublic(true)
                .build();

    if (request.displayName() != null && !request.displayName().isBlank()) {
      profile.setDisplayName(request.displayName().trim());
    }
    if (request.headline() != null) {
      profile.setHeadline(blankToNull(request.headline()));
    }
    if (request.institution() != null) {
      profile.setInstitution(blankToNull(request.institution()));
    }
    if (request.expertiseArea() != null) {
      profile.setExpertiseArea(blankToNull(request.expertiseArea()));
    }
    if (request.about() != null) {
      profile.setAbout(blankToNull(request.about()));
    }
    if (request.photoUrl() != null) {
      profile.setPhotoUrl(blankToNull(request.photoUrl()));
    }
    if (request.isPublic() != null) {
      profile.setIsPublic(request.isPublic());
    }
    if (request.credentialsText() != null) {
      profile.setCredentialsText(blankToNull(request.credentialsText()));
    }
    if (request.specialties() != null) {
      profile.setSpecialties(request.specialties());
    }
    if (request.yearsExperience() != null) {
      profile.setYearsExperience(request.yearsExperience());
    }

    return instructorProfileRepository.save(profile);
  }

  private MeResponse toResponse(
      User user, UserProfile profile, InstructorProfile instructorProfile) {
    long totalEnrolled =
        enrollmentRepository.countByUserIdAndStatus(user.getId(), EnrollmentStatus.ACTIVE);
    long completed =
        enrollmentRepository.countByUserIdAndStatusAndCompletedAtIsNotNull(
            user.getId(), EnrollmentStatus.ACTIVE);
    long earnedCertificates = certificateRepository.countByUserIdAndRevokedAtIsNull(user.getId());
    long attended = liveClassAttendanceRepository.countByUserId(user.getId());

    return MeResponse.builder()
        .userId(user.getId())
        .studentCode(user.getStudentCode())
        .fullName(user.getFullName())
        .email(user.getEmail())
        .phone(user.getPhone())
        .phoneCountryCode(user.getPhoneCountryCode())
        .status(user.getStatus().name())
        .emailVerified(user.getEmailVerifiedAt() != null)
        .phoneVerified(user.getPhoneVerifiedAt() != null)
        .emailVerifiedAt(user.getEmailVerifiedAt())
        .phoneVerifiedAt(user.getPhoneVerifiedAt())
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .roles(user.getRoleNames())
        .permissions(List.of())
        .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
        .locale(profile != null ? profile.getLocale() : "bn-BD")
        .timezone(profile != null ? profile.getTimezone() : null)
        .bio(profile != null ? profile.getBio() : null)
        .extraJson(profile != null ? profile.getExtraJson() : Map.of())
        .instructorProfile(toInstructorProfileResponse(instructorProfile))
        .totalEnrolledCourses((int) totalEnrolled)
        .completedCourses((int) completed)
        .earnedCertificates((int) earnedCertificates)
        .totalLiveClassesAttended((int) attended)
        .build();
  }

  private InstructorProfileResponse toInstructorProfileResponse(
      InstructorProfile instructorProfile) {
    if (instructorProfile == null) {
      return null;
    }
    return InstructorProfileResponse.builder()
        .displayName(instructorProfile.getDisplayName())
        .headline(instructorProfile.getHeadline())
        .institution(instructorProfile.getInstitution())
        .expertiseArea(instructorProfile.getExpertiseArea())
        .about(instructorProfile.getAbout())
        .photoUrl(instructorProfile.getPhotoUrl())
        .isPublic(instructorProfile.getIsPublic())
        .credentialsText(instructorProfile.getCredentialsText())
        .specialties(instructorProfile.getSpecialties())
        .yearsExperience(instructorProfile.getYearsExperience())
        .createdAt(instructorProfile.getCreatedAt())
        .updatedAt(instructorProfile.getUpdatedAt())
        .build();
  }

  private boolean hasInstructorPayload(UpdateProfileRequest request) {
    return request.displayName() != null
        || request.headline() != null
        || request.institution() != null
        || request.expertiseArea() != null
        || request.about() != null
        || request.photoUrl() != null
        || request.isPublic() != null
        || request.credentialsText() != null
        || request.specialties() != null
        || request.yearsExperience() != null;
  }

  private String normalizeEmail(String email) {
    String normalized = email.trim().toLowerCase(Locale.ROOT);
    if (normalized.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be blank");
    }
    return normalized;
  }

  private String normalizePhone(String phone) {
    String normalized = phone.trim().replaceAll("[^0-9+]", "");
    if (normalized.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone cannot be blank");
    }
    return normalized;
  }

  private String normalizeCountryCode(String code) {
    if (code == null) {
      return null;
    }
    String digits = code.replaceAll("[^0-9]", "");
    if (digits.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone country code is invalid");
    }
    String normalized = "+" + digits;
    if (normalized.length() > 5) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone country code is too long");
    }
    return normalized;
  }

  private String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isBlank() ? null : trimmed;
  }
}
