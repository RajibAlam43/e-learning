package com.gii.api.service.media;

import com.gii.api.model.response.MediaPlaybackResponse;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.enums.MediaProvider;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class MuxService implements MediaProviderService {

    @Value("${mux.signing-key-id}:")
    private final String signingKeyId;
    @Value("${mux.playback-token-ttl-s}:")
    private final long playbackTokenTtlSeconds;
    @Value("${mux.private-key-prem}:")
    private final String privateKeyPem;

    @Override
    public MediaProvider provider() {
        return MediaProvider.MUX;
    }

    @Override
    public MediaPlaybackResponse getPlayback(MediaAsset mediaAsset) {
        Instant expiresAt = Instant.now()
                .plusSeconds(playbackTokenTtlSeconds);

        String token = Jwts.builder()
                .header()
                .keyId(signingKeyId)
                .and()
                .subject(mediaAsset.getPlaybackId())
                .audience()
                .add("v")
                .and()
                .expiration(Date.from(expiresAt))
                .signWith(loadPrivateKey())
                .compact();

        String playbackUrl = "https://stream.mux.com/"
                + mediaAsset.getPlaybackId()
                + ".m3u8?token="
                + token;

        return new MediaPlaybackResponse(
                MediaProvider.MUX,
                com.gii.common.enums.PlaybackMode.HLS,
                null,
                playbackUrl,
                playbackUrl,
                mediaAsset.getPlaybackId(),
                token,
                expiresAt
        );
    }

    private PrivateKey loadPrivateKey() {
        try {
            String privateKey = privateKeyPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(privateKey);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Mux private key", e);
        }
    }
}
