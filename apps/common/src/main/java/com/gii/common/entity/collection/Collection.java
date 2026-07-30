package com.gii.common.entity.collection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseUuidEntity;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CollectionType;
import com.gii.common.enums.PublishStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "collections")
public class Collection extends BaseUuidEntity {

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "title_en")
  private String titleEn;

  @Column(name = "slug", nullable = false, unique = true)
  private String slug;

  @Enumerated(EnumType.STRING)
  @Column(name = "collection_type", nullable = false, length = 30)
  @Builder.Default
  private CollectionType type = CollectionType.PACK;

  @Column(name = "thumbnail_object_key")
  private String thumbnailObjectKey;

  @Column(name = "short_description")
  private String shortDescription;

  @Column(name = "short_description_en")
  private String shortDescriptionEn;

  @Column(name = "description")
  private String description;

  @Column(name = "description_en")
  private String descriptionEn;

  @Column(name = "price_bdt", nullable = false)
  @Builder.Default
  private BigDecimal priceBdt = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private PublishStatus status = PublishStatus.DRAFT;

  @Column(name = "published_at")
  private Instant publishedAt;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;
}
