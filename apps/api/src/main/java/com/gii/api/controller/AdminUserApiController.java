package com.gii.api.controller;

import com.gii.api.model.response.PageResponse;
import com.gii.api.model.response.admin.AdminUserSummaryResponse;
import com.gii.api.service.admin.AdminUserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminUserApiController implements AdminUserApi {

  private final AdminUserManagementService userManagementService;

  @Override
  public ResponseEntity<PageResponse<AdminUserSummaryResponse>> listUsers(
      String search, int page, int size) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(userManagementService.list(search, page, size));
  }
}
