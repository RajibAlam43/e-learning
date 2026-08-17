package com.gii.api.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gii.api.model.request.admin.CreateInstructorRequest;
import com.gii.common.entity.user.InstructorProfile;
import com.gii.common.entity.user.Role;
import com.gii.common.entity.user.User;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.user.InstructorProfileRepository;
import com.gii.common.repository.user.RoleRepository;
import com.gii.common.repository.user.UserRepository;
import com.gii.common.repository.user.UserRoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminInstructorManagementServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private UserRoleRepository userRoleRepository;
  @Mock private InstructorProfileRepository instructorProfileRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private CourseInstructorRepository courseInstructorRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @InjectMocks private AdminInstructorManagementService service;

  @Test
  void creationReturnsEightCharacterPasswordAndStoresOnlyItsHash() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findByEmail("new-instructor@example.com")).thenReturn(Optional.empty());
    when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-password");
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User user = invocation.getArgument(0);
              user.setId(userId);
              return user;
            });
    when(roleRepository.findByName("INSTRUCTOR"))
        .thenReturn(Optional.of(Role.builder().id(1L).name("INSTRUCTOR").build()));
    when(instructorProfileRepository.save(any(InstructorProfile.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(courseInstructorRepository.findByInstructorId(userId)).thenReturn(List.of());

    var response =
        service.create(
            CreateInstructorRequest.builder()
                .fullName("New Instructor")
                .email("new-instructor@example.com")
                .displayName("New Inst")
                .build());

    assertThat(response.temporaryPassword()).matches("[A-Za-z0-9]{8}");
    ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
    verify(passwordEncoder).encode(passwordCaptor.capture());
    assertThat(passwordCaptor.getValue()).isEqualTo(response.temporaryPassword());
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-password");
    assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo(response.temporaryPassword());
  }
}
