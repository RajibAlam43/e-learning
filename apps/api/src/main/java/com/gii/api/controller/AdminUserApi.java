package com.gii.api.controller;

import com.gii.api.model.response.PageResponse;
import com.gii.api.model.response.admin.AdminUserSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public interface AdminUserApi {

  @GetMapping
  @Operation(
      summary = "List students with pagination",
      description =
          "Includes accounts with the STUDENT role, even if they also have other roles, and all"
              + " account statuses, with or without orders. Searches name, email, or phone by"
              + " literal substring, ignoring case for name/email. Blank search lists all students."
              + " Ordered by registration date descending, then user ID descending.")
  ResponseEntity<PageResponse<AdminUserSummaryResponse>> listUsers(
      @RequestParam(required = false) String search,
      @Parameter(description = "Zero-based page number, at least 0")
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Students per page, from 1 to 100")
          @RequestParam(defaultValue = "20")
          int size);
}
