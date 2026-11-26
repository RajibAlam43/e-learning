package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.entity.user.User;
import com.gii.common.entity.user.UserRole;
import com.gii.common.entity.user.UserRoleId;
import com.gii.common.enums.UserStatus;
import jakarta.persistence.EntityManagerFactory;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@TestPropertySource(properties = "spring.jpa.open-in-view=false")
class AdminUsersApiIt extends AbstractAdminApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManagerFactory entityManagerFactory;

  @AfterEach
  void cleanup() {
    cleanupAdminData();
  }

  @Test
  void listsAccountsWithoutOrdersIncludingPhoneOnlyAndAllStatusesWithoutSecrets() throws Exception {
    var student = student("Student", "student@example.com");
    final var phoneOnly = phoneUser("Phone Student", "01712345678");
    var admin = student("Admin", "admin@example.com");
    grantRole(admin, "ADMIN");
    var suspended = student("Suspended", "suspended@example.com");
    suspended.setStatus(UserStatus.SUSPENDED);
    userRepository.save(suspended);
    var deleted = student("Deleted", "deleted@example.com");
    deleted.setStatus(UserStatus.DELETED);
    userRepository.save(deleted);

    mockMvc
        .perform(adminRequest())
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalElements").value(5))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(
            jsonPath("$.content[*].userId")
                .value(
                    containsInAnyOrder(
                        student.getId().toString(),
                        phoneOnly.getId().toString(),
                        admin.getId().toString(),
                        suspended.getId().toString(),
                        deleted.getId().toString())));

    mockMvc
        .perform(adminRequest().param("search", "01712345678"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].email").isEmpty())
        .andExpect(jsonPath("$.content[0].phone").value("01712345678"))
        .andExpect(jsonPath("$.content[0].phoneCountryCode").value("+880"))
        .andExpect(jsonPath("$.content[0].roles").value(contains("STUDENT")))
        .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
        .andExpect(jsonPath("$.content[0].createdAt").isNotEmpty())
        .andExpect(result ->
            assertThat(result.getResponse().getContentAsString())
                .doesNotContain("password", "googleSubject", "refreshToken", "secret"));

    mockMvc
        .perform(adminRequest().param("search", "admin@example.com"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].roles").value(contains("ADMIN", "STUDENT")));
  }

  @Test
  void includesAnyoneWithStudentRoleAndExcludesAccountsWithoutIt() throws Exception {
    var teacher = student("Directory Teacher", "teacher@example.com");
    grantRole(teacher, "INSTRUCTOR");
    student("Directory Student", "student@example.com");
    var teacherOnly = user("Teacher Only", "teacher-only@example.com");
    grantRole(teacherOnly, "INSTRUCTOR");
    var adminOnly = user("Admin Only", "admin-only@example.com");
    grantRole(adminOnly, "ADMIN");
    user("No Roles", "no-roles@example.com");

    mockMvc
        .perform(adminRequest().param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.content.length()").value(1));

    mockMvc
        .perform(adminRequest().param("search", "teacher"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].userId").value(teacher.getId().toString()))
        .andExpect(jsonPath("$.content[0].roles").value(contains("INSTRUCTOR", "STUDENT")));

    for (String search : List.of("teacher-only", "admin-only", "no-roles")) {
      mockMvc
          .perform(adminRequest().param("search", search))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements").value(0))
          .andExpect(jsonPath("$.content").isEmpty());
    }
  }

  @Test
  void paginatesUsersWithoutDuplicatesWhenAccountsHaveMultipleRolesOrEqualRegistrationTimes()
      throws Exception {
    var first = student("First", "first@example.com");
    final var second = student("Second", "second@example.com");
    final var third = student("Third", "third@example.com");
    grantRole(first, "ADMIN");
    jdbcTemplate.update("UPDATE users SET created_at = '2026-01-01T00:00:00Z'");
    var newer = student("Newer", "newer@example.com");
    var tiedIds =
        List.of(first, second, third).stream()
            .map(user -> user.getId().toString())
            .sorted(Comparator.reverseOrder())
            .toList();

    mockMvc
        .perform(adminRequest().param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.totalElements").value(4))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(
            jsonPath("$.content[*].userId")
                .value(contains(newer.getId().toString(), tiedIds.getFirst())));

    mockMvc
        .perform(adminRequest().param("page", "1").param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.totalElements").value(4))
        .andExpect(jsonPath("$.content[*].userId").value(contains(tiedIds.get(1), tiedIds.get(2))))
        .andExpect(jsonPath("$.content[0].roles").isArray());

    mockMvc
        .perform(adminRequest().param("page", "20").param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(4))
        .andExpect(jsonPath("$.content").isEmpty());
  }

  @ParameterizedTest
  @CsvSource({
    "'  aLiCe  ',Alice Student",
    "ALICE@EXAMPLE,Alice Student",
    "712345,Phone Student",
    "বাংলা,বাংলা শিক্ষার্থী"
  })
  void searchesNameEmailAndPhoneIncludingPhoneOnlyAccounts(String search, String expectedName)
      throws Exception {
    student("Alice Student", "alice@example.com");
    student("বাংলা শিক্ষার্থী", "bangla@example.com");
    phoneUser("Phone Student", "01712345678");
    student("Unrelated", "other@example.com");

    mockMvc
        .perform(adminRequest().param("search", search))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].fullName").value(expectedName));
  }

  @ParameterizedTest
  @ValueSource(strings = {"%", "_", "!", "' OR 1=1 --"})
  void treatsSearchCharactersLiterally(String search) throws Exception {
    student("Literal " + search, "literal@example.com");
    student("Other", "other@example.com");

    mockMvc
        .perform(adminRequest().param("search", search))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].fullName").value("Literal " + search));
  }

  @Test
  void acceptsBlankSearchAndMaximumPageSizeAndReturnsEmptyResultsForNoMatch() throws Exception {
    student("Existing", "existing@example.com");

    mockMvc
        .perform(adminRequest().param("search", "   ").param("size", "100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(100))
        .andExpect(jsonPath("$.totalElements").value(1));

    mockMvc
        .perform(adminRequest().param("search", "no match"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.totalPages").value(0))
        .andExpect(jsonPath("$.content").isEmpty());
  }

  @ParameterizedTest
  @CsvSource({"page,-1", "size,0", "size,-1", "size,101", "page,abc", "size,abc"})
  void rejectsInvalidPagination(String parameter, String value) throws Exception {
    mockMvc.perform(adminRequest().param(parameter, value)).andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @ValueSource(strings = {"STUDENT", "INSTRUCTOR"})
  void rejectsNonAdminAccounts(String role) throws Exception {
    var auth =
        new UsernamePasswordAuthenticationToken(
            UUID.randomUUID(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    mockMvc
        .perform(get("/admin/users").with(authentication(auth)))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsAnonymousRequests() throws Exception {
    mockMvc.perform(get("/admin/users")).andExpect(status().isForbidden());
  }

  @Test
  void loadsRolesWithOneBatchWithoutFetchingEveryUserOrPaginatingCollectionFetch()
      throws Exception {
    for (int index = 0; index < 5; index++) {
      var account = student("Account " + index, "account" + index + "@example.com");
      grantRole(account, "ADMIN");
    }
    var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();
    try {
      mockMvc
          .perform(adminRequest().param("size", "3"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content.length()").value(3))
          .andExpect(jsonPath("$.totalElements").value(5))
          .andExpect(jsonPath("$.content[0].roles").value(contains("ADMIN", "STUDENT")));
      assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    } finally {
      statistics.setStatisticsEnabled(false);
    }
  }

  private MockHttpServletRequestBuilder adminRequest() {
    return get("/admin/users").with(authentication(adminAuth(UUID.randomUUID())));
  }

  private User phoneUser(String name, String phone) {
    User user =
        userRepository.saveAndFlush(
            User.builder()
                .fullName(name)
                .phone(phone)
                .phoneCountryCode("+880")
                .passwordHash("x")
                .status(UserStatus.ACTIVE)
                .build());
    grantRole(user, "STUDENT");
    return user;
  }

  private User student(String name, String email) {
    User user = user(name, email);
    grantRole(user, "STUDENT");
    return user;
  }

  private void grantRole(User user, String name) {
    var role = roleRepository.findByName(name).orElseThrow();
    userRoleRepository.saveAndFlush(
        UserRole.builder()
            .id(UserRoleId.builder().userId(user.getId()).roleId(role.getId()).build())
            .user(user)
            .role(role)
            .build());
  }
}
