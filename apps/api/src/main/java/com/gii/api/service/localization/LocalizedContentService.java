package com.gii.api.service.localization;

import java.util.List;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class LocalizedContentService {

  public String text(String defaultValue, String englishValue) {
    return isEnglish() && hasText(englishValue) ? englishValue : defaultValue;
  }

  public <T> List<T> list(List<T> defaultValue, List<T> englishValue) {
    return isEnglish() && englishValue != null && !englishValue.isEmpty()
        ? englishValue
        : defaultValue;
  }

  public String english(String defaultValue, String englishValue) {
    return hasText(englishValue) ? englishValue : defaultValue;
  }

  private boolean isEnglish() {
    return "en".equalsIgnoreCase(LocaleContextHolder.getLocale().getLanguage());
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
