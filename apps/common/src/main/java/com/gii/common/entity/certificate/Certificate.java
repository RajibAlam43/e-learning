package com.gii.common.entity.certificate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CertificateTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "certificates")
public class Certificate {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "certificate_code", nullable = false, unique = true, length = 100)
  private String certificateCode;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false, length = 20)
  private CertificateTargetType targetType;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id")
  private Course course;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "collection_id")
  private Collection collection;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id")
  private CertificateTemplate template;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "pdf_url")
  private String pdfUrl;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "issued_by")
  private User issuedBy;

  @Column(name = "recipient_name", nullable = false)
  private String recipientName;

  @Column(name = "target_title", nullable = false)
  private String targetTitle;

  @Column(name = "target_slug", nullable = false)
  private String targetSlug;

  @PrePersist
  protected void onCreate() {
    if (this.issuedAt == null) {
      this.issuedAt = Instant.now();
    }
  }
}
