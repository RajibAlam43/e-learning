package com.gii.common.model.support;

import com.gii.common.model.common.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "faqs")
public class Faq extends BaseUuidEntity {

    @Column(name = "question", nullable = false)
    private String question;

    @Column(name = "answer", nullable = false)
    private String answer;

    @Column(name = "position", nullable = false)
    private Integer position = 0;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = true;
}