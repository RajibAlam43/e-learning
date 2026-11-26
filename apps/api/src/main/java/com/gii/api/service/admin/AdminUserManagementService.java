package com.gii.api.service.admin;

import com.gii.api.model.response.PageResponse;
import com.gii.api.model.response.admin.AdminUserSummaryResponse;
import com.gii.common.entity.user.User;
import com.gii.common.repository.user.UserRepository;
import com.gii.common.repository.user.UserRoleRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserManagementService {

  private final UserRepository userRepository;
  private final UserRoleRepository userRoleRepository;

  public PageResponse<AdminUserSummaryResponse> list(String search, int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "page must be at least 0 and size must be between 1 and 100");
    }
    String term = search == null ? "" : search.strip().toLowerCase(Locale.ROOT);
    String pattern = "%" + term.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    Page<User> users =
        userRepository.searchStudents(
            pattern,
            PageRequest.of(
                page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    List<UUID> userIds = users.stream().map(User::getId).toList();
    Map<UUID, List<String>> rolesByUserId =
        userIds.isEmpty()
            ? Map.of()
            : userRoleRepository.findAllByUserIdIn(userIds).stream()
                .collect(
                    Collectors.groupingBy(
                        userRole -> userRole.getId().getUserId(),
                        Collectors.mapping(
                            userRole -> userRole.getRole().getName(), Collectors.toList())));
    List<AdminUserSummaryResponse> content =
        users.stream()
            .map(user ->
                AdminUserSummaryResponse.builder()
                    .userId(user.getId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .phoneCountryCode(user.getPhoneCountryCode())
                    .roles(
                        rolesByUserId.getOrDefault(user.getId(), List.of()).stream()
                            .sorted()
                            .toList())
                    .status(user.getStatus())
                    .createdAt(user.getCreatedAt())
                    .build())
            .toList();
    return PageResponse.<AdminUserSummaryResponse>builder()
        .content(content)
        .page(users.getNumber())
        .size(users.getSize())
        .totalElements(users.getTotalElements())
        .totalPages(users.getTotalPages())
        .build();
  }
}
