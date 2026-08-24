package com.gii.api.service.auth;

import com.gii.api.exception.UnauthorizedApiException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleIdentityTokenVerifier {

  private final GoogleIdTokenVerifier verifier;

  public GoogleIdentityTokenVerifier(@Value("${auth.google.client-ids}") List<String> clientIds) {
    List<String> audience =
        clientIds.stream().map(String::trim).filter(id -> !id.isBlank()).toList();
    if (audience.isEmpty()) {
      throw new IllegalStateException("At least one Google OAuth client ID must be configured");
    }
    this.verifier =
        new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport.Builder().build(), GsonFactory.getDefaultInstance())
            .setAudience(audience)
            .build();
  }

  public GoogleProfile verify(String credential) {
    try {
      GoogleIdToken idToken = verifier.verify(credential);
      if (idToken == null) {
        throw invalidToken();
      }
      GoogleIdToken.Payload payload = idToken.getPayload();
      String subject = payload.getSubject();
      String email = payload.getEmail();
      if (subject == null
          || subject.isBlank()
          || email == null
          || email.isBlank()
          || email.indexOf('@') <= 0
          || !Boolean.TRUE.equals(payload.getEmailVerified())) {
        throw invalidToken();
      }
      Object nameClaim = payload.get("name");
      String name = nameClaim instanceof String value ? value : null;
      return new GoogleProfile(subject, email, name);
    } catch (UnauthorizedApiException exception) {
      throw exception;
    } catch (Exception exception) {
      throw invalidToken();
    }
  }

  private UnauthorizedApiException invalidToken() {
    return new UnauthorizedApiException("Invalid Google credential");
  }

  public record GoogleProfile(String subject, String email, String name) {}
}
