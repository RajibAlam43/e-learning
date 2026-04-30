package com.gii.common.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseTokenEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseTokenEntity {

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "session_id", nullable = false)
  private UUID sessionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "replaced_by_token_id")
  private RefreshToken replacedByToken;
}
