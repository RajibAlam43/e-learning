package com.gii.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.LocaleResolver;

class LocaleConfigTest {

  private final LocaleResolver resolver = new LocaleConfig().localeResolver();

  @Test
  void defaultsToBanglaWhenRequestHasNoLanguagePreference() {
    assertThat(resolver.resolveLocale(new MockHttpServletRequest()))
        .isEqualTo(LocaleConfig.DEFAULT_LOCALE);
  }

  @Test
  void usesEnglishAcceptLanguageHeader() {
    var request = new MockHttpServletRequest();
    request.addHeader("Accept-Language", "en-US,en;q=0.9");

    assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.ENGLISH);
  }

  @Test
  void queryParameterOverridesAcceptLanguageHeader() {
    var englishRequest = new MockHttpServletRequest();
    englishRequest.setParameter("lang", "en");
    englishRequest.addHeader("Accept-Language", "bn-BD");
    assertThat(resolver.resolveLocale(englishRequest)).isEqualTo(Locale.ENGLISH);

    var banglaRequest = new MockHttpServletRequest();
    banglaRequest.setParameter("lang", "bn");
    banglaRequest.addHeader("Accept-Language", "en");
    assertThat(resolver.resolveLocale(banglaRequest)).isEqualTo(LocaleConfig.DEFAULT_LOCALE);
  }

  @Test
  void unsupportedLanguageFallsBackToBangla() {
    var request = new MockHttpServletRequest();
    request.setParameter("lang", "fr");

    assertThat(resolver.resolveLocale(request)).isEqualTo(LocaleConfig.DEFAULT_LOCALE);
  }
}
