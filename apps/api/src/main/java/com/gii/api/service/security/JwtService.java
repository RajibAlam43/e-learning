package com.gii.api.service.security;

import com.gii.common.entity.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  @Value("${app.jwt.secret}")
  private String secret;

  @Value("${app.jwt.access-token-expiration-ms}")
  private long accessTokenExpiration;

  @Value("${app.jwt.issuer:e-learning-api}")
  private String issuer;

  @Value("${app.jwt.audience:e-learning-clients}")
  private String audience;

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
  }

  public String generateAccessToken(User user) {
    var builder =
        Jwts.builder()
            // Use immutable user id as token subject so phone-only accounts are supported.
            .subject(user.getId().toString())
            .issuer(issuer)
            .audience().add(audience).and()
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration));

    if (user.getEmail() != null && !user.getEmail().isBlank()) {
      builder.claim("email", user.getEmail());
    }
    if (user.getPhone() != null && !user.getPhone().isBlank()) {
      builder.claim("phone", user.getPhone());
    }
    builder.claim("roles", user.getRoleNames());

    return builder.signWith(getSigningKey()).compact();
  }

  public String extractSubject(String token) {
    return parseClaims(token).getSubject();
  }

  public UUID extractUserId(String token) {
    return UUID.fromString(extractSubject(token));
  }

  public List<String> extractRoles(String token) {
    Object roles = parseClaims(token).get("roles");
    if (roles instanceof List<?> rawRoles) {
      return rawRoles.stream()
          .filter(Objects::nonNull)
          .map(String::valueOf)
          .filter(role -> !role.isBlank())
          .toList();
    }
    return List.of();
  }

  public boolean isTokenValid(String token) {
    Claims claims = parseClaims(token);
    return !claims.getExpiration().before(new Date());
  }

  public boolean isTokenExpired(String token) {
    return parseClaims(token).getExpiration().before(new Date());
  }

  private Claims parseClaims(String token) {
    Claims claims =
        Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    if (!Objects.equals(issuer, claims.getIssuer())) {
      throw new RuntimeException("Invalid token issuer");
    }
    if (claims.getAudience() == null || !claims.getAudience().contains(audience)) {
      throw new RuntimeException("Invalid token audience");
    }
    return claims;
  }
}
