package com.gii.api.service.pub;

import com.gii.api.model.response.PublicAppSettingResponse;
import com.gii.common.repository.setting.AppSettingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicAppSettingsService {

  private final AppSettingRepository appSettingRepository;

  public List<PublicAppSettingResponse> execute() {
    return appSettingRepository.findByIsPublicTrueOrderBySettingKeyAsc().stream()
        .map(
            setting ->
                PublicAppSettingResponse.builder()
                    .key(setting.getSettingKey())
                    .value(setting.getValueJson())
                    .updatedAt(setting.getUpdatedAt())
                    .build())
        .toList();
  }
}
