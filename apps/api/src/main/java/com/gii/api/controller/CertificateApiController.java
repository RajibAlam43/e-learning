package com.gii.api.controller;

import com.gii.api.model.response.certificate.CertificateDownloadUrlResponse;
import com.gii.api.model.response.certificate.CertificateIssueResponse;
import com.gii.api.model.response.certificate.PublicCertificateVerificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CertificateApiController implements CertificateApi {

    @Override
    public ResponseEntity<CertificateIssueResponse> issueOrGetCertificate(UUID courseId, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<CertificateDownloadUrlResponse> getCertificateDownloadUrl(UUID certificateId, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<PublicCertificateVerificationResponse> verifyCertificate(String code) {
        return null;
    }
}
