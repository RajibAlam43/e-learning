package com.gii.api.service.localization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

class LocalizedContentServiceTest {

  private final LocalizedContentService service = new LocalizedContentService();

  @AfterEach
  void resetLocale() {
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  void returnsEnglishTextAndListsForEnglishRequests() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    assertThat(service.text("বাংলা", "English")).isEqualTo("English");
    assertThat(service.list(List.of("বাংলা"), List.of("English")))
        .containsExactly("English");
  }

  @Test
  void returnsBanglaForBanglaRequests() {
    LocaleContextHolder.setLocale(Locale.forLanguageTag("bn-BD"));

    assertThat(service.text("বাংলা", "English")).isEqualTo("বাংলা");
    assertThat(service.list(List.of("বাংলা"), List.of("English")))
        .containsExactly("বাংলা");
  }

  @Test
  void missingEnglishContentFallsBackToBangla() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    assertThat(service.text("বাংলা", null)).isEqualTo("বাংলা");
    assertThat(service.text("বাংলা", "  ")).isEqualTo("বাংলা");
    assertThat(service.list(List.of("বাংলা"), null)).containsExactly("বাংলা");
    assertThat(service.list(List.of("বাংলা"), List.of())).containsExactly("বাংলা");
  }

  @Test
  void certificateSelectionAlwaysPrefersEnglishIndependentOfRequestLocale() {
    LocaleContextHolder.setLocale(Locale.forLanguageTag("bn-BD"));

    assertThat(service.english("বাংলা", "English")).isEqualTo("English");
    assertThat(service.english("বাংলা", null)).isEqualTo("বাংলা");
  }
}
