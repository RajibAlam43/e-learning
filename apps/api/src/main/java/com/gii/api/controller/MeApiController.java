package com.gii.api.controller;

import com.gii.api.model.request.me.UpdateProfileRequest;
import com.gii.api.model.response.me.MeResponse;
import com.gii.api.service.me.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeApiController implements MeApi {

  private final ProfileService profileService;

  @Override
  public ResponseEntity<MeResponse> getMe(Authentication authentication) {
    return ResponseEntity.ok(profileService.get(authentication));
  }

  @Override
  public ResponseEntity<MeResponse> updateProfile(
      UpdateProfileRequest request, Authentication authentication) {
    return ResponseEntity.ok(profileService.update(request, authentication));
  }
}
