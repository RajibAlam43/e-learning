package com.gii.common.entity.support;

import com.gii.common.entity.common.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
@Table(name = "faqs")
public class Faq extends BaseUuidEntity {

  @Column(name = "question", nullable = false)
  private String question;

  @Column(name = "answer", nullable = false)
  private String answer;

  @Column(name = "position", nullable = false)
  @Builder.Default
  private Integer position = 0;

  @Column(name = "is_published", nullable = false)
  @Builder.Default
  private Boolean isPublished = true;
}
