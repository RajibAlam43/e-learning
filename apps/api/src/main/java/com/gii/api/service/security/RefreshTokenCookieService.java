package com.gii.api.service.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookieService {

    private static final String COOKIE_NAME = "refresh_token";
    private static final String CACHE_CONTROL_VALUE = "no-store, no-cache, must-revalidate, max-age=0";

    @Value("${auth.refresh-cookie.path:/public/auth/refresh}")
    private String cookiePath;

    @Value("${auth.refresh-cookie.same-site:None}")
    private String sameSite;

    @Value("${auth.refresh-cookie.secure:true}")
    private boolean secureCookie;

    @Value("${auth.refresh-cookie.max-age-days:30}")
    private long maxAgeDays;

    @Value("${auth.refresh-cookie.domain:}")
    private String cookieDomain;

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path(cookiePath)
                .maxAge(Duration.ofDays(maxAgeDays));

        if (!cookieDomain.isBlank()) {
            cookieBuilder.domain(cookieDomain);
        }

        ResponseCookie cookie = cookieBuilder.build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        setNoStoreHeaders(response);
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path(cookiePath)
                .maxAge(0);

        if (!cookieDomain.isBlank()) {
            cookieBuilder.domain(cookieDomain);
        }

        ResponseCookie cookie = cookieBuilder.build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        setNoStoreHeaders(response);
    }

    private void setNoStoreHeaders(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE);
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setDateHeader(HttpHeaders.EXPIRES, 0);
    }
}
