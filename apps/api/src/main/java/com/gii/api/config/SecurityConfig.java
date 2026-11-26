package com.gii.api.config;

import com.gii.api.service.security.JwtAuthenticationFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Value("${app.cors.allowed-origins:}")
  private List<String> allowedOrigins;

  @Value("${app.cors.allowed-origin-patterns:}")
  private List<String> allowedOriginPatterns;

  @Profile("local")
  @Bean
  SecurityFilterChain securityFilterChainLocal(HttpSecurity http) {
    http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

    return http.build();
  }

  @Profile("!local")
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) {

    http.csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth ->
            auth.requestMatchers(
                    "/actuator/health",
                    "/public/**",
                    "/recording-uploads/**",
                    "/webhooks/**",
                    "/payments/sslcommerz/*/success",
                    "/payments/sslcommerz/*/failed",
                    "/payments/sslcommerz/*/cancelled",
                    "/payments/bkash/*/success",
                    "/payments/bkash/*/failed",
                    "/payments/bkash/*/cancelled",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html")
                .permitAll()
                .anyRequest()
                .authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  private boolean hasValues(List<String> values) {
    return values != null && values.stream().anyMatch(value -> value != null && !value.isBlank());
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    if (hasValues(allowedOriginPatterns)) {
      config.setAllowedOriginPatterns(
          allowedOriginPatterns.stream()
              .filter(value -> value != null && !value.isBlank())
              .toList());
    } else if (hasValues(allowedOrigins)) {
      List<String> configuredOrigins =
          allowedOrigins.stream().filter(value -> value != null && !value.isBlank()).toList();
      config.setAllowedOrigins(configuredOrigins);
    } else {
      throw new IllegalStateException(
          "CORS is not configured. Set app.cors.allowed-origins"
              + " or app.cors.allowed-origin-patterns.");
    }

    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    // Only these capability-token endpoints are callable by the pilot extension.
    // They accept no cookies or LMS bearer tokens; every operation verifies its recording token.
    CorsConfiguration uploads = new CorsConfiguration();
    uploads.setAllowedOrigins(List.of("*"));
    uploads.setAllowedMethods(List.of("POST", "OPTIONS"));
    uploads.setAllowedHeaders(List.of("Content-Type", "X-Recording-Token"));
    uploads.setAllowCredentials(false);
    source.registerCorsConfiguration("/recording-uploads/**", uploads);
    source.registerCorsConfiguration("/**", config);

    return source;
  }
}
