package com.assessment.urlshortener.service;

import com.assessment.urlshortener.dto.CreateUrlRequest;
import com.assessment.urlshortener.dto.CreateUrlResponse;
import com.assessment.urlshortener.model.Url;
import com.assessment.urlshortener.repository.UrlRepository;
import com.assessment.urlshortener.exception.ShortUrlExpiredException;
import com.assessment.urlshortener.exception.ShortUrlNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UrlShortenerService {

    private final UrlRepository urlRepository;

    public UrlShortenerService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    public CreateUrlResponse createShortUrl(CreateUrlRequest request) {

        String shortCode = generateUniqueShortCode();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime expiresAt = null;

        if (request.getExpirationDays() != null
                && request.getExpirationDays() > 0) {
            expiresAt = createdAt.plusDays(request.getExpirationDays());
        }

        Url url = new Url(
                request.getOriginalUrl(),
                shortCode,
                createdAt,
                expiresAt
        );

        Url savedUrl = urlRepository.save(url);

        String shortUrl =
                "http://localhost:8080/" + savedUrl.getShortCode();

        return new CreateUrlResponse(
                savedUrl.getOriginalUrl(),
                savedUrl.getShortCode(),
                shortUrl,
                savedUrl.getCreatedAt(),
                savedUrl.getExpiresAt()
        );
    }

    public Url getUrlByShortCode(String shortCode) {

        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new ShortUrlNotFoundException(
                                "Short URL not found: " + shortCode
                        ));

        if (url.getExpiresAt() != null
                && LocalDateTime.now().isAfter(url.getExpiresAt())) {

            throw new ShortUrlExpiredException(
                    "Short URL has expired: " + shortCode
            );
        }

        url.incrementClickCount();
        urlRepository.save(url);

        return url;
    }

    public Url getUrlStats(String shortCode) {

        return urlRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new ShortUrlNotFoundException(
                                "Short URL not found: " + shortCode
                        ));
    }

    private String generateUniqueShortCode() {

        String shortCode;

        do {
            shortCode = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 7);

        } while (urlRepository.existsByShortCode(shortCode));

        return shortCode;
    }
}