package com.gii.api.service.security;

import com.gii.common.entity.user.User;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.user.UserRepository;
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
  private final UserRepository userRepository;

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
      String subject = jwtService.extractSubject(jwt);

      if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        User user = resolveUserByJwtSubject(subject);
        if (user.getStatus() != UserStatus.ACTIVE) {
          throw new RuntimeException("Inactive user");
        }

        if (jwtService.isTokenValid(jwt, user)) {
          List<GrantedAuthority> authorities =
              user.getRoleNames().stream()
                  .map(
                      roleName -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + roleName))
                  .toList();

          UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(user, null, authorities);

          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      }
    } catch (Exception ignored) {
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);
  }

  private User resolveUserByJwtSubject(String subject) {
    try {
      UUID userId = UUID.fromString(subject);
      return userRepository
          .findById(userId)
          .orElseThrow(() -> new RuntimeException("User not found"));
    } catch (IllegalArgumentException ex) {
      throw new RuntimeException("Invalid token subject");
    }
  }
}
