package com.gii.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;

@Configuration
public class LocaleConfig {

  public static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("bn-BD");
  private static final Locale ENGLISH = Locale.ENGLISH;

  @Bean(name = "localeResolver")
  LocaleResolver localeResolver() {
    return new LocaleResolver() {
      @Override
      public Locale resolveLocale(HttpServletRequest request) {
        String requestedLanguage = request.getParameter("lang");
        if (requestedLanguage != null) {
          Locale requestedLocale = Locale.forLanguageTag(requestedLanguage.trim());
          return "en".equalsIgnoreCase(requestedLocale.getLanguage()) ? ENGLISH : DEFAULT_LOCALE;
        }
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
          return DEFAULT_LOCALE;
        }
        Locale accepted = request.getLocale();
        return "en".equalsIgnoreCase(accepted.getLanguage()) ? ENGLISH : DEFAULT_LOCALE;
      }

      @Override
      public void setLocale(
          HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException("Request locale cannot be changed");
      }
    };
  }
}
