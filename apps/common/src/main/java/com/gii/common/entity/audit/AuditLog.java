package com.gii.common.entity.audit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.CreatedOnlyUuidEntity;
import com.gii.common.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Map;
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
@Table(name = "audit_logs")
public class AuditLog extends CreatedOnlyUuidEntity {

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_user_id")
  private User actorUser;

  @Column(name = "action", nullable = false, length = 100)
  private String action;

  @Column(name = "entity_type", nullable = false, length = 100)
  private String entityType;

  @Column(name = "entity_id")
  private String entityId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata_json", columnDefinition = "jsonb")
  private Map<String, Object> metadataJson;
}
