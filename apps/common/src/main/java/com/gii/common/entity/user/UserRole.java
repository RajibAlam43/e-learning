package com.gii.common.entity.user;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
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
@Table(name = "user_roles")
public class UserRole {

  @EmbeddedId @Builder.Default private UserRoleId id = UserRoleId.builder().build();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("userId")
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("roleId")
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;
}
