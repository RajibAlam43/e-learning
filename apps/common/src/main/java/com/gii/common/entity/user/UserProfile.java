package com.gii.common.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Map;
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
@Table(name = "user_profiles")
public class UserProfile {

  @Id
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @JsonIgnore
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Column(name = "locale", nullable = false, length = 20)
  @Builder.Default
  private String locale = "bn-BD";

  @Column(name = "timezone", length = 100)
  private String timezone;

  @Column(name = "bio")
  private String bio;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "extra_json", columnDefinition = "jsonb")
  private Map<String, Object> extraJson;
}
