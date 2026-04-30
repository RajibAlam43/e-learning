package com.gii.common.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "instructor_profiles")
public class InstructorProfile {

  @Id
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @JsonIgnore
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "headline")
  private String headline;

  @Column(name = "institution")
  private String institution;

  @Column(name = "expertise_area")
  private String expertiseArea;

  @Column(name = "about")
  private String about;

  @Column(name = "photo_url")
  private String photoUrl;

  @Column(name = "is_public", nullable = false)
  @Builder.Default
  private Boolean isPublic = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "credentials_text", columnDefinition = "text")
  private String credentialsText;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "specialties_json", columnDefinition = "jsonb")
  private java.util.List<String> specialties;

  @Column(name = "years_experience")
  private Integer yearsExperience;

  @PrePersist
  protected void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
