package com.assessment.urlshortener.controller;

import com.assessment.urlshortener.dto.CreateUrlRequest;
import com.assessment.urlshortener.dto.CreateUrlResponse;
import com.assessment.urlshortener.model.Url;
import com.assessment.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/urls")
public class UrlController {

    private final UrlShortenerService urlShortenerService;

    public UrlController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @PostMapping
    public ResponseEntity<CreateUrlResponse> createShortUrl(
            @Valid @RequestBody CreateUrlRequest request) {

        CreateUrlResponse response =
                urlShortenerService.createShortUrl(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    @GetMapping("/{shortCode}/stats")
public ResponseEntity<Url> getUrlStats(
        @PathVariable String shortCode) {

    Url url = urlShortenerService.getUrlStats(shortCode);

    return ResponseEntity.ok(url);
}
}