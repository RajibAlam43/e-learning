package com.gii.api.controller;

import com.gii.api.model.request.me.UpdateProfileRequest;
import com.gii.api.model.response.me.MeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User Profile", description = "Current user profile and settings")
@SecurityRequirement(name = "bearerAuth")
public interface MeApi {

  @GetMapping("/me")
  @Operation(
      summary = "Get current user",
      description = "Retrieve authenticated user's profile information including roles and stats.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User profile retrieved",
            content = @Content(schema = @Schema(implementation = MeResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  ResponseEntity<MeResponse> getMe(Authentication authentication);

  @PatchMapping("/me/profile")
  @Operation(
      summary = "Update profile",
      description =
          "Update user profile information: name, email, phone, avatar, bio, preferences,"
              + " and instructor details.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile updated",
            content = @Content(schema = @Schema(implementation = MeResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "409", description = "Email or phone already in use")
      })
  ResponseEntity<MeResponse> updateProfile(
      @RequestBody UpdateProfileRequest request, Authentication authentication);
}
