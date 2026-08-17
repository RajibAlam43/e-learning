package com.gii.common.entity.setting;

import com.gii.common.entity.common.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
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
@Table(name = "app_settings")
public class AppSetting extends BaseUuidEntity {

  @Column(name = "setting_key", nullable = false, unique = true, length = 150)
  private String settingKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "value_json", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> valueJson;

  @Column(name = "description", length = 1000)
  private String description;

  @Column(name = "is_public", nullable = false)
  @Builder.Default
  private Boolean isPublic = false;
}
