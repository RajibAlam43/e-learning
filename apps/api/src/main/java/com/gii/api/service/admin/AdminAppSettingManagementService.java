package com.gii.api.service.admin;

import com.gii.api.model.request.admin.UpsertAppSettingRequest;
import com.gii.api.model.response.admin.AdminAppSettingResponse;
import com.gii.common.entity.setting.AppSetting;
import com.gii.common.repository.setting.AppSettingRepository;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminAppSettingManagementService {

  private static final Pattern VALID_KEY = Pattern.compile("[a-z0-9][a-z0-9._-]{0,149}");
  private static final Pattern SENSITIVE_KEY =
      Pattern.compile(
          "(^|[._-])"
              + "(secrets?|passwords?|tokens?|credentials?|private|api[_-]?keys?"
              + "|access[_-]?keys?)"
              + "($|[._-])");

  private final AppSettingRepository appSettingRepository;

  @Transactional(readOnly = true)
  public List<AdminAppSettingResponse> list() {
    return appSettingRepository.findAllByOrderBySettingKeyAsc().stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public AdminAppSettingResponse get(String key) {
    return toResponse(find(key));
  }

  public AdminAppSettingResponse upsert(String key, UpsertAppSettingRequest request) {
    String normalizedKey = normalizeKey(key);
    if (Boolean.TRUE.equals(request.isPublic())
        && (isSensitiveKey(normalizedKey) || containsSensitiveField(request.value()))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Sensitive settings cannot be public");
    }
    AppSetting setting =
        appSettingRepository
            .findBySettingKey(normalizedKey)
            .orElseGet(() -> AppSetting.builder().settingKey(normalizedKey).build());
    setting.setValueJson(request.value());
    setting.setDescription(trimToNull(request.description()));
    setting.setIsPublic(request.isPublic());
    return toResponse(appSettingRepository.save(setting));
  }

  public void delete(String key) {
    appSettingRepository.delete(find(key));
  }

  private AppSetting find(String key) {
    return appSettingRepository
        .findBySettingKey(normalizeKey(key))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Setting not found"));
  }

  private String normalizeKey(String key) {
    String normalized = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    if (!VALID_KEY.matcher(normalized).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid setting key");
    }
    return normalized;
  }

  private boolean containsSensitiveField(Object value) {
    if (value instanceof java.util.Map<?, ?> map) {
      return map.entrySet().stream()
          .anyMatch(
              entry ->
                  isSensitiveKey(String.valueOf(entry.getKey()))
                      || containsSensitiveField(entry.getValue()));
    }
    if (value instanceof Iterable<?> values) {
      for (Object item : values) {
        if (containsSensitiveField(item)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isSensitiveKey(String key) {
    String normalized = key.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    return SENSITIVE_KEY.matcher(normalized).find();
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private AdminAppSettingResponse toResponse(AppSetting setting) {
    return AdminAppSettingResponse.builder()
        .settingId(setting.getId())
        .key(setting.getSettingKey())
        .value(setting.getValueJson())
        .description(setting.getDescription())
        .isPublic(setting.getIsPublic())
        .createdAt(setting.getCreatedAt())
        .updatedAt(setting.getUpdatedAt())
        .build();
  }
}
