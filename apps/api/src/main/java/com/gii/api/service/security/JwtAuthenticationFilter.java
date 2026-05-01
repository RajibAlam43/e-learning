package com.gii.api.service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(
      @NotNull HttpServletRequest request,
      @NotNull HttpServletResponse response,
      @NotNull FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String jwt = authHeader.substring(7);

    try {
      UUID userId = jwtService.extractUserId(jwt);

      if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        if (jwtService.isTokenValid(jwt)) {
          List<String> roleNames = jwtService.extractRoles(jwt);
          List<GrantedAuthority> authorities =
              roleNames.stream()
                  .map(
                      roleName -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + roleName))
                  .toList();

          UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(userId, null, authorities);

          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      }
    } catch (Exception ignored) {
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);
  }
}
