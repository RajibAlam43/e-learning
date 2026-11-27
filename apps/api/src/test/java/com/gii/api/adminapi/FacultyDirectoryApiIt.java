package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.common.entity.user.User;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.faculty.FacultyGroupRepository;
import com.gii.common.repository.faculty.FacultyMemberRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestPropertySource(properties = "assets.base-url=https://assets.example.test")
class FacultyDirectoryApiIt extends AbstractAdminApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired FacultyGroupRepository groups;
  @Autowired FacultyMemberRepository members;
  private final ObjectMapper json = new ObjectMapper();

  private UUID group(User admin, String name, int position) throws Exception {
    final var body =
        mvc.perform(
                post("/admin/faculty/groups")
                    .with(authentication(adminAuth(admin.getId())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of("name", name, "position", position, "visible", true))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(json.readTree(body).path("id").asText());
  }

  private String person(
      UUID group,
      String name,
      boolean faculty,
      boolean home,
      int position,
      UUID teacher,
      String photo)
      throws Exception {
    final var data = new java.util.HashMap<String, Object>();
    data.put("groupId", group);
    data.put("name", name);
    data.put("title", "Role");
    data.put("bio", "Biography");
    data.put("position", position);
    data.put("homepagePosition", 10 - position);
    data.put("showOnFaculty", faculty);
    data.put("showOnHomepage", home);
    data.put("instructorId", teacher);
    data.put("photoObjectKey", photo);
    return json.writeValueAsString(data);
  }

  private UUID add(User admin, String data) throws Exception {
    final var result =
        mvc.perform(
                post("/admin/faculty/members")
                    .with(authentication(adminAuth(admin.getId())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(data))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(json.readTree(result).path("id").asText());
  }

  @Test
  void manualProfilesCustomGroupsAndIndependentVisibilityCanBeManaged() throws Exception {
    final var admin = user("Admin", "faculty-admin@example.com");
    final var group = group(admin, "Guest scholars", 7);
    final var id =
        add(
            admin,
            person(
                group, "Guest", false, true, 1, null, "thumbnails/faculty/portrait-1234abcd.png"));
    mvc.perform(get("/public/faculty"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/public/faculty/featured"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Guest"))
        .andExpect(
            jsonPath("$[0].photoUrl")
                .value("https://assets.example.test/thumbnails/faculty/portrait-1234abcd.png"));
    mvc.perform(
            delete("/admin/faculty/groups/{id}", group)
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isConflict());
    mvc.perform(
            put("/admin/faculty/members/{id}", id)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(person(group, "Guest updated", true, false, 2, null, null)))
        .andExpect(status().isOk());
    mvc.perform(get("/public/faculty"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].members[0].name").value("Guest updated"));
    mvc.perform(get("/public/faculty/featured")).andExpect(jsonPath("$.length()").value(0));
    mvc.perform(
            put("/admin/faculty/groups/{id}", group)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Guest scholars renamed\",\"position\":1,\"visible\":false}"))
        .andExpect(status().isOk());
    mvc.perform(get("/public/faculty")).andExpect(jsonPath("$.length()").value(0));
    mvc.perform(
            delete("/admin/faculty/members/{id}", id)
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isNoContent());
    mvc.perform(
            delete("/admin/faculty/groups/{id}", group)
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isNoContent());
    assertThat(members.findById(id)).isEmpty();
    assertThat(groups.findById(group)).isEmpty();
  }

  @Test
  void manualProfileDetailsCanBeSavedLocalizedPreservedAndCleared() throws Exception {
    final var admin = user("Admin", "faculty-manual-details@example.com");
    final var group = group(admin, "Advisors", 1);
    final var body =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(person(group, "Advisor", true, true, 1, null, null));
    body.set(
        "profileDetails",
        json.valueToTree(
            Map.of(
                "credentialsText",
                "Default degree",
                "credentialsTextEn",
                "English degree",
                "institution",
                "Institute",
                "institutionEn",
                "English institute",
                "expertiseArea",
                "Research",
                "specialties",
                List.of("Arabic", "History"),
                "yearsExperience",
                12)));
    final var id = add(admin, body.toString());
    mvc.perform(get("/public/faculty").header("Accept-Language", "en"))
        .andExpect(jsonPath("$[0].members[0].credentialsText").value("English degree"))
        .andExpect(jsonPath("$[0].members[0].institution").value("English institute"))
        .andExpect(jsonPath("$[0].members[0].specialties.length()").value(2))
        .andExpect(jsonPath("$[0].members[0].yearsExperience").value(12));
    mvc.perform(
            put("/admin/faculty/members/{id}", id)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(person(group, "Advisor updated", true, true, 1, null, null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.credentialsText").value("Default degree"));
    assertThat(members.findById(id).orElseThrow().getYearsExperience()).isEqualTo(12);
    body.set("profileDetails", json.createObjectNode());
    mvc.perform(
            put("/admin/faculty/members/{id}", id)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk());
    assertThat(members.findById(id).orElseThrow().getYearsExperience()).isNull();
    assertThat(members.findById(id).orElseThrow().getCredentialsText()).isNull();
    body.set("profileDetails", json.valueToTree(Map.of("yearsExperience", -1)));
    mvc.perform(
            put("/admin/faculty/members/{id}", id)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void linkedTeachersKeepOneProfileAndRespectPrivacyAndAccountStatus() throws Exception {
    final var admin = user("Admin", "faculty-teacher-admin@example.com");
    final var teacher = user("Teacher", "faculty-teacher@example.com");
    final var profile = instructorProfile(teacher);
    profile.setPhotoUrl("https://assets.example.test/old-photo.png");
    profile.setCredentialsText("Default qualification");
    profile.setCredentialsTextEn("English qualification");
    profile.setInstitution("Default institution");
    profile.setInstitutionEn("English institution");
    profile.setExpertiseArea("Default expertise");
    profile.setExpertiseAreaEn("English expertise");
    profile.setSpecialties(List.of("Default specialty"));
    profile.setSpecialtiesEn(List.of("English specialty"));
    profile.setYearsExperience(5);
    instructorProfileRepository.saveAndFlush(profile);
    final var group = group(admin, "Teachers", 1);
    final var id =
        add(admin, person(group, "Teacher updated", true, true, 1, teacher.getId(), null));
    assertThat(instructorProfileRepository.findById(teacher.getId()).orElseThrow().getDisplayName())
        .isEqualTo("Teacher updated");
    mvc.perform(get("/public/faculty").header("Accept-Language", "en"))
        .andExpect(
            jsonPath("$[0].members[0].photoUrl").value("https://assets.example.test/old-photo.png"))
        .andExpect(jsonPath("$[0].members[0].credentialsText").value("English qualification"))
        .andExpect(jsonPath("$[0].members[0].institution").value("English institution"))
        .andExpect(jsonPath("$[0].members[0].expertiseArea").value("English expertise"))
        .andExpect(jsonPath("$[0].members[0].specialties[0]").value("English specialty"))
        .andExpect(jsonPath("$[0].members[0].yearsExperience").value(5));
    mvc.perform(
            post("/admin/faculty/members")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(person(group, "Duplicate", true, true, 2, teacher.getId(), null)))
        .andExpect(status().isConflict());
    mvc.perform(
            put("/admin/faculty/members/{id}", id)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    person(
                        group,
                        "Teacher updated",
                        true,
                        true,
                        1,
                        teacher.getId(),
                        "thumbnails/faculty/new-photo-aabb1234.jpg")))
        .andExpect(status().isOk());
    assertThat(profile.getPhotoUrl()).endsWith("new-photo-aabb1234.jpg");
    mvc.perform(get("/public/instructors/{id}", teacher.getId()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.avatarUrl")
                .value("https://assets.example.test/thumbnails/faculty/new-photo-aabb1234.jpg"));
    profile.setIsPublic(false);
    instructorProfileRepository.saveAndFlush(profile);
    mvc.perform(get("/public/faculty")).andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/public/faculty/featured")).andExpect(jsonPath("$.length()").value(0));
    profile.setIsPublic(true);
    teacher.setStatus(UserStatus.SUSPENDED);
    userRepository.saveAndFlush(teacher);
    mvc.perform(get("/public/faculty/featured")).andExpect(jsonPath("$.length()").value(0));
    mvc.perform(
            delete("/admin/faculty/members/{id}", id)
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isNoContent());
    assertThat(instructorProfileRepository.findById(teacher.getId())).isPresent();
    assertThat(userRepository.findById(teacher.getId())).isPresent();
  }

  @Test
  void groupMemberAndHomepagePositionsControlOrdering() throws Exception {
    final var admin = user("Admin", "faculty-order-admin@example.com");
    final var later = group(admin, "Later group", 8);
    final var first = group(admin, "First group", 1);
    add(admin, person(later, "Later", true, true, 1, null, null));
    add(admin, person(first, "Second", true, true, 2, null, null));
    add(admin, person(first, "First", true, false, 1, null, null));
    mvc.perform(get("/public/faculty"))
        .andExpect(jsonPath("$[0].name").value("First group"))
        .andExpect(jsonPath("$[0].members[0].name").value("First"))
        .andExpect(jsonPath("$[0].members[1].name").value("Second"));
    mvc.perform(get("/public/faculty/featured"))
        .andExpect(jsonPath("$[0].name").value("Second"))
        .andExpect(jsonPath("$[1].name").value("Later"));
  }

  @Test
  void onlyAdminsCanWriteAndInvalidPhotoKeysAndPositionsAreRejected() throws Exception {
    final var admin = user("Admin", "faculty-auth-admin@example.com");
    final var group = group(admin, "Staff", 1);
    for (String role : List.of("STUDENT", "INSTRUCTOR")) {
      final var auth =
          new UsernamePasswordAuthenticationToken(
              admin.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
      mvc.perform(get("/admin/faculty").with(authentication(auth)))
          .andExpect(status().isForbidden());
      mvc.perform(
              post("/admin/faculty/members")
                  .with(authentication(auth))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(person(group, "Hidden", true, true, 1, null, null)))
          .andExpect(status().isForbidden());
    }
    mvc.perform(
            post("/admin/faculty/members")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(person(group, "Bad key", true, true, 1, null, "private/secret.png")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/admin/faculty/members")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(person(group, "Bad order", true, true, 0, null, null)))
        .andExpect(status().isBadRequest());
  }
}
