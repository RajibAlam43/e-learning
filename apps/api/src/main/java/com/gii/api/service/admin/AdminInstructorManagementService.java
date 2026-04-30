package com.gii.api.service.admin;

import com.gii.api.model.request.admin.AssignInstructorToCourseRequest;
import com.gii.api.model.request.admin.CreateInstructorRequest;
import com.gii.api.model.request.admin.UpdateInstructorRequest;
import com.gii.api.model.response.admin.AdminCourseSummaryResponse;
import com.gii.api.model.response.admin.AdminInstructorDetailResponse;
import com.gii.api.model.response.admin.AdminInstructorSummaryResponse;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.course.CourseInstructorId;
import com.gii.common.entity.user.InstructorProfile;
import com.gii.common.entity.user.Role;
import com.gii.common.entity.user.User;
import com.gii.common.entity.user.UserRole;
import com.gii.common.entity.user.UserRoleId;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.user.InstructorProfileRepository;
import com.gii.common.repository.user.RoleRepository;
import com.gii.common.repository.user.UserRepository;
import com.gii.common.repository.user.UserRoleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminInstructorManagementService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;
  private final InstructorProfileRepository instructorProfileRepository;
  private final CourseRepository courseRepository;
  private final CourseInstructorRepository courseInstructorRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  public List<AdminInstructorSummaryResponse> list() {
    return instructorProfileRepository.findAll().stream().map(this::toSummary).toList();
  }

  public AdminInstructorDetailResponse create(CreateInstructorRequest request) {
    validateUniqueContacts(request.email(), request.phone(), null);

    User user =
        User.builder()
            .fullName(request.fullName().trim())
            .email(trimToNull(request.email()))
            .phone(trimToNull(request.phone()))
            .phoneCountryCode(trimToNull(request.phoneCountryCode()))
            // Generated placeholder hash; instructor should set real password through reset flow.
            .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
            .status(UserStatus.ACTIVE)
            .build();
    User savedUser = userRepository.save(user);
    attachInstructorRole(savedUser);

    InstructorProfile profile =
        InstructorProfile.builder()
            .user(savedUser)
            .displayName(request.displayName().trim())
            .headline(trimToNull(request.headline()))
            .institution(trimToNull(request.institution()))
            .expertiseArea(trimToNull(request.expertiseArea()))
            .about(trimToNull(request.about()))
            .photoUrl(trimToNull(request.photoUrl()))
            .isPublic(request.isPublic() == null || request.isPublic())
            .credentialsText(trimToNull(request.credentialsText()))
            .specialties(request.specialties())
            .yearsExperience(request.yearsExperience())
            .build();
    InstructorProfile savedProfile = instructorProfileRepository.save(profile);
    return toDetail(savedProfile);
  }

  public AdminInstructorDetailResponse update(UUID instructorId, UpdateInstructorRequest request) {
    User user =
        userRepository
            .findById(instructorId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Instructor user not found"));
    validateUniqueContacts(request.email(), request.phone(), instructorId);

    if (request.fullName() != null && !request.fullName().isBlank()) {
      user.setFullName(request.fullName().trim());
    }
    if (request.email() != null) {
      user.setEmail(trimToNull(request.email()));
    }
    if (request.phone() != null) {
      user.setPhone(trimToNull(request.phone()));
    }
    if (request.phoneCountryCode() != null) {
      user.setPhoneCountryCode(trimToNull(request.phoneCountryCode()));
    }

    InstructorProfile profile =
        instructorProfileRepository
            .findById(instructorId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Instructor profile not found"));

    if (request.displayName() != null && !request.displayName().isBlank()) {
      profile.setDisplayName(request.displayName().trim());
    }
    if (request.headline() != null) {
      profile.setHeadline(trimToNull(request.headline()));
    }
    if (request.institution() != null) {
      profile.setInstitution(trimToNull(request.institution()));
    }
    if (request.expertiseArea() != null) {
      profile.setExpertiseArea(trimToNull(request.expertiseArea()));
    }
    if (request.about() != null) {
      profile.setAbout(trimToNull(request.about()));
    }
    if (request.photoUrl() != null) {
      profile.setPhotoUrl(trimToNull(request.photoUrl()));
    }
    if (request.isPublic() != null) {
      profile.setIsPublic(request.isPublic());
    }
    if (request.credentialsText() != null) {
      profile.setCredentialsText(trimToNull(request.credentialsText()));
    }
    if (request.specialties() != null) {
      profile.setSpecialties(request.specialties());
    }
    if (request.yearsExperience() != null) {
      profile.setYearsExperience(request.yearsExperience());
    }

    userRepository.save(user);
    return toDetail(instructorProfileRepository.save(profile));
  }

  public void assign(UUID courseId, AssignInstructorToCourseRequest request) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    User instructor =
        userRepository
            .findById(request.instructorUserId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instructor not found"));
    if (!instructorProfileRepository.existsByUserId(instructor.getId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not an instructor");
    }
    if (courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, instructor.getId())) {
      return;
    }

    InstructorRole role = parseInstructorRole(request.role());
    CourseInstructorId id =
        CourseInstructorId.builder()
            .courseId(courseId)
            .instructorUserId(instructor.getId())
            .build();
    CourseInstructor assignment =
        CourseInstructor.builder().id(id).course(course).instructor(instructor).role(role).build();
    courseInstructorRepository.save(assignment);
  }

  private void attachInstructorRole(User user) {
    Role role =
        roleRepository
            .findByName("ROLE_INSTRUCTOR")
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "ROLE_INSTRUCTOR missing"));
    UserRoleId id = UserRoleId.builder().userId(user.getId()).roleId(role.getId()).build();
    userRoleRepository.save(UserRole.builder().id(id).user(user).role(role).build());
  }

  private AdminInstructorSummaryResponse toSummary(InstructorProfile profile) {
    User user = profile.getUser();
    int assignedCount = courseInstructorRepository.findByInstructorId(user.getId()).size();
    return AdminInstructorSummaryResponse.builder()
        .userId(user.getId())
        .fullName(user.getFullName())
        .email(user.getEmail())
        .displayName(profile.getDisplayName())
        .headline(profile.getHeadline())
        .isPublic(profile.getIsPublic())
        .assignedCoursesCount(assignedCount)
        .createdAt(profile.getCreatedAt())
        .build();
  }

  private AdminInstructorDetailResponse toDetail(InstructorProfile profile) {
    User user = profile.getUser();
    List<AdminCourseSummaryResponse> assignedCourses =
        courseInstructorRepository.findByInstructorId(user.getId()).stream()
            .map(CourseInstructor::getCourse)
            .map(this::toCourseSummary)
            .toList();
    return AdminInstructorDetailResponse.builder()
        .userId(user.getId())
        .fullName(user.getFullName())
        .email(user.getEmail())
        .phone(user.getPhone())
        .phoneCountryCode(user.getPhoneCountryCode())
        .displayName(profile.getDisplayName())
        .headline(profile.getHeadline())
        .institution(profile.getInstitution())
        .expertiseArea(profile.getExpertiseArea())
        .about(profile.getAbout())
        .photoUrl(profile.getPhotoUrl())
        .isPublic(profile.getIsPublic())
        .credentialsText(profile.getCredentialsText())
        .specialties(profile.getSpecialties())
        .yearsExperience(profile.getYearsExperience())
        .createdAt(profile.getCreatedAt())
        .updatedAt(profile.getUpdatedAt())
        .assignedCourses(assignedCourses)
        .build();
  }

  private AdminCourseSummaryResponse toCourseSummary(Course course) {
    return AdminCourseSummaryResponse.builder()
        .courseId(course.getId())
        .title(course.getTitle())
        .slug(course.getSlug())
        .status(course.getStatus() == null ? PublishStatus.DRAFT : course.getStatus())
        .priceBdt(course.getPriceBdt() == null ? BigDecimal.ZERO : course.getPriceBdt())
        .isFree(course.getIsFree())
        .instructorName(null)
        .totalEnrolled(0)
        .publishedAt(course.getPublishedAt())
        .createdAt(course.getCreatedAt())
        .build();
  }

  private void validateUniqueContacts(String email, String phone, UUID currentUserId) {
    String normalizedEmail = trimToNull(email);
    if (normalizedEmail != null) {
      userRepository
          .findByEmail(normalizedEmail)
          .ifPresent(
              existing -> {
                if (!existing.getId().equals(currentUserId)) {
                  throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
                }
              });
    }
    String normalizedPhone = trimToNull(phone);
    if (normalizedPhone != null) {
      userRepository
          .findByPhone(normalizedPhone)
          .ifPresent(
              existing -> {
                if (!existing.getId().equals(currentUserId)) {
                  throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already in use");
                }
              });
    }
  }

  private InstructorRole parseInstructorRole(String value) {
    if (value == null || value.isBlank()) {
      return InstructorRole.PRIMARY;
    }
    try {
      return InstructorRole.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid instructor role");
    }
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
