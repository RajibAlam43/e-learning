package com.gii.common.entity.faculty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "faculty_members")
public class FacultyMember extends BaseUuidEntity {
  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id", nullable = false)
  private FacultyGroup group;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "instructor_id")
  private com.gii.common.entity.user.InstructorProfile instructor;

  private String name;
  private String title;
  private String titleEn;

  @Column(columnDefinition = "text")
  private String bio;

  @Column(columnDefinition = "text")
  private String bioEn;

  @Column(columnDefinition = "text")
  private String credentialsText;

  @Column(columnDefinition = "text")
  private String credentialsTextEn;

  private String institution;
  private String institutionEn;
  private String expertiseArea;
  private String expertiseAreaEn;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<String> specialties;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<String> specialtiesEn;

  private Integer yearsExperience;

  private String photoObjectKey;

  @Column(nullable = false)
  private Integer position;

  @Column(nullable = false)
  private Integer homepagePosition;

  @Column(nullable = false)
  private Boolean showOnFaculty;

  @Column(nullable = false)
  private Boolean showOnHomepage;
}
