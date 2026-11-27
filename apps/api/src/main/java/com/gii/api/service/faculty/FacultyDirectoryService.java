package com.gii.api.service.faculty;

import com.gii.api.model.request.admin.SaveFacultyGroupRequest;
import com.gii.api.model.request.admin.SaveFacultyMemberRequest;
import com.gii.api.model.response.faculty.FacultyGroupResponse;
import com.gii.api.model.response.faculty.FacultyMemberResponse;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.api.service.storage.AssetUrlService;
import com.gii.common.entity.faculty.FacultyGroup;
import com.gii.common.entity.faculty.FacultyMember;
import com.gii.common.entity.user.InstructorProfile;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.faculty.FacultyGroupRepository;
import com.gii.common.repository.faculty.FacultyMemberRepository;
import com.gii.common.repository.user.InstructorProfileRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class FacultyDirectoryService {
  private final FacultyGroupRepository groups;
  private final FacultyMemberRepository members;
  private final InstructorProfileRepository instructors;
  private final AssetUrlService assets;
  private final LocalizedContentService localized;

  @Transactional(readOnly = true)
  public List<FacultyGroupResponse> list(boolean admin) {
    final var people = members.findAllByOrderByPositionAscIdAsc();
    return groups.findAllByOrderByPositionAscIdAsc().stream()
        .filter(g -> admin || g.getVisible())
        .map(g ->
            new FacultyGroupResponse(
                g.getId(),
                admin ? g.getName() : localized.text(g.getName(), g.getNameEn()),
                g.getNameEn(),
                g.getPosition(),
                g.getVisible(),
                people.stream()
                    .filter(m -> m.getGroup().getId().equals(g.getId()))
                    .filter(m -> admin || (m.getShowOnFaculty() && profilePublic(m)))
                    .map(m -> response(m, admin))
                    .toList()))
        .filter(g -> admin || !g.members().isEmpty())
        .toList();
  }

  @Transactional(readOnly = true)
  public List<FacultyMemberResponse> featured() {
    return members.findAllByOrderByPositionAscIdAsc().stream()
        .filter(m -> m.getGroup().getVisible() && m.getShowOnHomepage() && profilePublic(m))
        .sorted(
            Comparator.comparing(FacultyMember::getHomepagePosition)
                .thenComparing(FacultyMember::getId))
        .map(m -> response(m, false))
        .toList();
  }

  public FacultyGroupResponse saveGroup(UUID id, SaveFacultyGroupRequest request) {
    final var group = id == null ? FacultyGroup.builder().build() : group(id);
    group.setName(request.name().trim());
    group.setNameEn(clean(request.nameEn()));
    group.setPosition(request.position());
    group.setVisible(request.visible());
    groups.saveAndFlush(group);
    return list(true).stream().filter(g -> g.id().equals(group.getId())).findFirst().orElseThrow();
  }

  public void deleteGroup(UUID id) {
    final var group = group(id);
    if (members.existsByGroupId(id)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Move or remove this group's people before deleting it");
    }
    groups.delete(group);
  }

  public FacultyMemberResponse saveMember(UUID id, SaveFacultyMemberRequest request) {
    final var person = id == null ? FacultyMember.builder().build() : member(id);
    final var group = group(request.groupId());
    final InstructorProfile teacher =
        request.instructorId() == null
            ? null
            : instructors
                .findById(request.instructorId())
                .orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
    if (teacher != null
        && members.findAllByOrderByPositionAscIdAsc().stream()
            .anyMatch(m ->
                !m.getId().equals(id)
                    && m.getGroup().getId().equals(group.getId())
                    && m.getInstructor() != null
                    && m.getInstructor().getUserId().equals(teacher.getUserId()))) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "This teacher is already in this group");
    }
    final String photoKey =
        request.photoObjectKey() == null
            ? person.getPhotoObjectKey()
            : assets.normalizeThumbnailKey(request.photoObjectKey(), "faculty");
    if (request.photoObjectKey() != null
        && photoKey != null
        && assets.publicUrl(photoKey) == null) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "The public assets domain is not configured");
    }
    person.setGroup(group);
    person.setInstructor(teacher);
    person.setName(teacher == null ? request.name().trim() : null);
    person.setTitle(teacher == null ? clean(request.title()) : null);
    person.setTitleEn(teacher == null ? clean(request.titleEn()) : null);
    person.setBio(teacher == null ? clean(request.bio()) : null);
    person.setBioEn(teacher == null ? clean(request.bioEn()) : null);
    person.setPhotoObjectKey(photoKey);
    person.setPosition(request.position());
    person.setHomepagePosition(request.homepagePosition());
    person.setShowOnFaculty(request.showOnFaculty());
    person.setShowOnHomepage(request.showOnHomepage());
    applyProfileDetails(person, teacher, request);
    if (teacher != null) {
      // The instructor profile remains the source for all linked directory entries.
      teacher.setDisplayName(request.name().trim());
      teacher.getUser().setFullName(request.name().trim());
      teacher.setHeadline(clean(request.title()));
      teacher.setHeadlineEn(clean(request.titleEn()));
      teacher.setAbout(clean(request.bio()));
      teacher.setAboutEn(clean(request.bioEn()));
      if (request.photoObjectKey() != null) {
        teacher.setPhotoUrl(assets.publicUrl(photoKey));
      }
      instructors.save(teacher);
    }
    members.saveAndFlush(person);
    return response(person, true);
  }

  public void deleteMember(UUID id) {
    members.delete(member(id));
  }

  private FacultyGroup group(UUID id) {
    return groups
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty group not found"));
  }

  private FacultyMember member(UUID id) {
    return members
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty member not found"));
  }

  private boolean profilePublic(FacultyMember member) {
    final var teacher = member.getInstructor();
    return teacher == null
        || (Boolean.TRUE.equals(teacher.getIsPublic())
            && teacher.getUser().getStatus() == UserStatus.ACTIVE);
  }

  private FacultyMemberResponse response(FacultyMember m, boolean admin) {
    final var t = m.getInstructor();
    final String title = t == null ? m.getTitle() : t.getHeadline();
    final String titleEn = t == null ? m.getTitleEn() : t.getHeadlineEn();
    final String bio = t == null ? m.getBio() : t.getAbout();
    final String bioEn = t == null ? m.getBioEn() : t.getAboutEn();
    return new FacultyMemberResponse(
        m.getId(),
        m.getGroup().getId(),
        t == null ? null : t.getUserId(),
        t == null ? m.getName() : t.getDisplayName(),
        admin ? title : localized.text(title, titleEn),
        titleEn,
        admin ? bio : localized.text(bio, bioEn),
        bioEn,
        t == null ? assets.publicUrl(m.getPhotoObjectKey()) : t.getPhotoUrl(),
        admin ? m.getPhotoObjectKey() : null,
        m.getPosition(),
        m.getHomepagePosition(),
        m.getShowOnFaculty(),
        m.getShowOnHomepage(),
        profilePublic(m),
        profileText(
            t == null ? m.getCredentialsText() : t.getCredentialsText(),
            t == null ? m.getCredentialsTextEn() : t.getCredentialsTextEn(),
            admin),
        profileText(
            t == null ? m.getInstitution() : t.getInstitution(),
            t == null ? m.getInstitutionEn() : t.getInstitutionEn(),
            admin),
        profileText(
            t == null ? m.getExpertiseArea() : t.getExpertiseArea(),
            t == null ? m.getExpertiseAreaEn() : t.getExpertiseAreaEn(),
            admin),
        admin
            ? (t == null ? m.getSpecialties() : t.getSpecialties())
            : localized.list(
                t == null ? m.getSpecialties() : t.getSpecialties(),
                t == null ? m.getSpecialtiesEn() : t.getSpecialtiesEn()),
        t == null ? m.getYearsExperience() : t.getYearsExperience(),
        t == null ? m.getCredentialsTextEn() : t.getCredentialsTextEn(),
        t == null ? m.getInstitutionEn() : t.getInstitutionEn(),
        t == null ? m.getExpertiseAreaEn() : t.getExpertiseAreaEn(),
        t == null ? m.getSpecialtiesEn() : t.getSpecialtiesEn());
  }

  private String profileText(String value, String englishValue, boolean admin) {
    return admin ? value : localized.text(value, englishValue);
  }

  private void applyProfileDetails(
      FacultyMember person, InstructorProfile teacher, SaveFacultyMemberRequest request) {
    final var details = request.profileDetails();
    // An omitted details object preserves existing data. A supplied object replaces it,
    // allowing optional fields (including years of experience) to be cleared.
    if (details == null) {
      return;
    }
    if (teacher == null) {
      person.setCredentialsText(clean(details.credentialsText()));
      person.setCredentialsTextEn(clean(details.credentialsTextEn()));
      person.setInstitution(clean(details.institution()));
      person.setInstitutionEn(clean(details.institutionEn()));
      person.setExpertiseArea(clean(details.expertiseArea()));
      person.setExpertiseAreaEn(clean(details.expertiseAreaEn()));
      person.setSpecialties(details.specialties());
      person.setSpecialtiesEn(details.specialtiesEn());
      person.setYearsExperience(details.yearsExperience());
    } else {
      teacher.setCredentialsText(clean(details.credentialsText()));
      teacher.setCredentialsTextEn(clean(details.credentialsTextEn()));
      teacher.setInstitution(clean(details.institution()));
      teacher.setInstitutionEn(clean(details.institutionEn()));
      teacher.setExpertiseArea(clean(details.expertiseArea()));
      teacher.setExpertiseAreaEn(clean(details.expertiseAreaEn()));
      teacher.setSpecialties(details.specialties());
      teacher.setSpecialtiesEn(details.specialtiesEn());
      teacher.setYearsExperience(details.yearsExperience());
    }
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
