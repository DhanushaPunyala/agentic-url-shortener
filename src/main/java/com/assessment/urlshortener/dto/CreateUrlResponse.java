package com.assessment.urlshortener.dto;

import java.time.LocalDateTime;

public class CreateUrlResponse {

    private String originalUrl;
    private String shortCode;
    private String shortUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public CreateUrlResponse(
            String originalUrl,
            String shortCode,
            String shortUrl,
            LocalDateTime createdAt,
            LocalDateTime expiresAt) {

        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.shortUrl = shortUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}