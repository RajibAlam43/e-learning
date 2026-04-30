package com.gii.common.entity.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.CreatedOnlyUuidEntity;
import com.gii.common.entity.user.User;
import com.gii.common.enums.SupportTicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "support_tickets")
public class SupportTicket extends CreatedOnlyUuidEntity {

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "name")
  private String name;

  @Column(name = "email", columnDefinition = "citext")
  private String email;

  @Column(name = "phone", length = 30)
  private String phone;

  @Column(name = "subject", nullable = false)
  private String subject;

  @Column(name = "message", nullable = false)
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private SupportTicketStatus status = SupportTicketStatus.OPEN;

  @Column(name = "closed_at")
  private Instant closedAt;
}
