package com.gii.api.service.live.oauth;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleMeetTokenCipherService {
  @Value("${integrations.google-meet.token-encryption-key:}")
  private String encryptionKey;

  public void requireConfigured() {
    key();
  }

  public String encrypt(String value, String context) {
    try {
      byte[] nonce = new byte[12];
      new SecureRandom().nextBytes(nonce);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, nonce));
      cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
      byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
      byte[] output = Arrays.copyOf(nonce, nonce.length + encrypted.length);
      System.arraycopy(encrypted, 0, output, nonce.length, encrypted.length);
      return "v1." + Base64.getEncoder().encodeToString(output);
    } catch (GeneralSecurityException error) {
      throw new IllegalStateException("Could not protect Google authorization", error);
    }
  }

  public String decrypt(String value, String context) {
    try {
      if (!value.startsWith("v1.")) {
        throw new IllegalArgumentException("Unknown credential version");
      }
      byte[] bytes = Base64.getDecoder().decode(value.substring(3));
      if (bytes.length < 29) {
        throw new IllegalArgumentException("Invalid credential length");
      }
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Arrays.copyOf(bytes, 12)));
      cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
      return new String(cipher.doFinal(bytes, 12, bytes.length - 12), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException error) {
      throw new IllegalStateException(
          "Could not read Google authorization; reconnect or check encryption key");
    }
  }

  public static String randomToken() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static String hash(String value) {
    try {
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException error) {
      throw new IllegalStateException(error);
    }
  }

  private SecretKeySpec key() {
    try {
      byte[] bytes = Base64.getDecoder().decode(encryptionKey);
      if (bytes.length != 32) {
        throw new IllegalArgumentException("Invalid key length");
      }
      return new SecretKeySpec(bytes, "AES");
    } catch (IllegalArgumentException error) {
      throw new IllegalStateException(
          "Configure GOOGLE_MEET_TOKEN_ENCRYPTION_KEY with a base64 32-byte key");
    }
  }
}
