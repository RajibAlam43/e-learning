package com.gii.api.service.security;

import com.gii.common.entity.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  @Value("${app.jwt.secret}")
  private String secret;

  @Value("${app.jwt.access-token-expiration-ms}")
  private long accessTokenExpiration;

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
  }

  public String generateAccessToken(User user) {
    return Jwts.builder()
        .subject(user.getEmail())
        // .claim("role", user.getRole().name())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
        .signWith(getSigningKey())
        .compact();
  }

  public String extractSubject(String token) {
    return parseClaims(token).getSubject();
  }

  public boolean isTokenValid(String token, User user) {
    String email = extractSubject(token);
    return email.equals(user.getEmail()) && !isTokenExpired(token);
  }

  public boolean isTokenExpired(String token) {
    return parseClaims(token).getExpiration().before(new Date());
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }
}
