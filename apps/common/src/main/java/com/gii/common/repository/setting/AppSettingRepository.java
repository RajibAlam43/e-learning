package com.gii.common.repository.setting;

import com.gii.common.entity.setting.AppSetting;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, UUID> {

  Optional<AppSetting> findBySettingKey(String settingKey);

  List<AppSetting> findAllByOrderBySettingKeyAsc();

  List<AppSetting> findByIsPublicTrueOrderBySettingKeyAsc();
}
