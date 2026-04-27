package com.gii.api.processor;

import com.gii.api.model.request.CreateMediaAssetRequest;
import com.gii.api.model.request.UpdateMediaAssetRequest;
import com.gii.api.model.response.MediaAssetResponse;
import com.gii.api.service.media.MediaAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminApiProcessingService {

    private final MediaAssetService mediaAssetService;

    public MediaAssetResponse createMediaAsset(UUID courseId, UUID lessonId, CreateMediaAssetRequest request) {
        return mediaAssetService.createMediaAsset(courseId, lessonId, request);
    }

    public MediaAssetResponse getMediaAsset(UUID assetId) {
        return mediaAssetService.getMediaAsset(assetId);
    }

    public MediaAssetResponse updateMediaAsset(UUID assetId, UpdateMediaAssetRequest request) {
        return mediaAssetService.updateMediaAsset(assetId, request);
    }

    public void deleteMediaAsset(UUID assetId) {
        mediaAssetService.deleteMediaAsset(assetId);
    }
}
