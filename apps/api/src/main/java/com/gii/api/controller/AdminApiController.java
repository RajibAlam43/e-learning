package com.gii.api.controller;

import com.gii.api.model.request.CreateMediaAssetRequest;
import com.gii.api.model.request.UpdateMediaAssetRequest;
import com.gii.api.model.response.MediaAssetResponse;
import com.gii.api.processor.AdminApiProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final AdminApiProcessingService adminApiProcessingService;

    @PostMapping("/courses/{courseId}/lessons/{lessonId}/media-assets")
    public ResponseEntity<MediaAssetResponse> create(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId,
            @RequestBody CreateMediaAssetRequest request
    ) {
        MediaAssetResponse response = adminApiProcessingService.createMediaAsset(courseId, lessonId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/media-assets/{assetId}")
    public ResponseEntity<MediaAssetResponse> get(@PathVariable UUID assetId) {
        return ResponseEntity.ok(adminApiProcessingService.getMediaAsset(assetId));
    }

    @PatchMapping("/media-assets/{assetId}")
    public ResponseEntity<MediaAssetResponse> update(
            @PathVariable UUID assetId,
            @RequestBody UpdateMediaAssetRequest request
    ) {
        return ResponseEntity.ok(adminApiProcessingService.updateMediaAsset(assetId, request));
    }

    @DeleteMapping("/media-assets/{assetId}")
    public ResponseEntity<Void> delete(@PathVariable UUID assetId) {
        adminApiProcessingService.deleteMediaAsset(assetId);
        return ResponseEntity.noContent().build();
    }
}
