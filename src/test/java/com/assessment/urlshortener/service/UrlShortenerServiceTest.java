package com.assessment.urlshortener.service;

import com.assessment.urlshortener.dto.CreateUrlRequest;
import com.assessment.urlshortener.dto.CreateUrlResponse;
import com.assessment.urlshortener.model.Url;
import com.assessment.urlshortener.repository.UrlRepository;
import com.assessment.urlshortener.exception.ShortUrlNotFoundException;
import com.assessment.urlshortener.exception.ShortUrlExpiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

class UrlShortenerServiceTest {

    @Mock
    private UrlRepository urlRepository;

    private UrlShortenerService urlShortenerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        urlShortenerService = new UrlShortenerService(urlRepository);
    }

    @Test
    void shouldCreateShortUrlSuccessfully() {

        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.google.com");
        request.setExpirationDays(7);

        when(urlRepository.existsByShortCode(anyString()))
                .thenReturn(false);

        when(urlRepository.save(any(Url.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateUrlResponse response = urlShortenerService.createShortUrl(request);

        assertNotNull(response);
        assertEquals(
                "https://www.google.com",
                response.getOriginalUrl());
        assertNotNull(response.getShortCode());
        assertNotNull(response.getShortUrl());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getExpiresAt());

        verify(urlRepository, times(1))
                .save(any(Url.class));
    }

    @Test
    void shouldIncrementClickCountWhenShortUrlIsAccessed() {

        Url url = new Url(
                "https://www.google.com",
                "abc1234",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7));

        when(urlRepository.findByShortCode("abc1234"))
                .thenReturn(Optional.of(url));

        when(urlRepository.save(any(Url.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Url result = urlShortenerService.getUrlByShortCode("abc1234");

        assertNotNull(result);
        assertEquals(1, result.getClickCount());

        verify(urlRepository, times(1))
                .save(url);
    }

    @Test
    void shouldThrowExceptionWhenShortCodeNotFound() {

        when(urlRepository.findByShortCode("invalid"))
                .thenReturn(Optional.empty());

        ShortUrlNotFoundException exception = assertThrows(
                ShortUrlNotFoundException.class,
                () -> urlShortenerService.getUrlByShortCode("invalid"));

        assertEquals(
                "Short URL not found: invalid",
                exception.getMessage());

        verify(urlRepository, never())
                .save(any(Url.class));
    }

    @Test
    void shouldThrowExceptionWhenShortUrlIsExpired() {

        Url expiredUrl = new Url(
                "https://www.google.com",
                "expired1",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1));

        when(urlRepository.findByShortCode("expired1"))
                .thenReturn(Optional.of(expiredUrl));

        ShortUrlExpiredException exception = assertThrows(
                ShortUrlExpiredException.class,
                () -> urlShortenerService.getUrlByShortCode("expired1"));

        assertEquals(
                "Short URL has expired: expired1",
                exception.getMessage());

        assertEquals(0, expiredUrl.getClickCount());

        verify(urlRepository, never())
                .save(any(Url.class));
    }

    @Test
    void shouldGetUrlStatsWithoutIncrementingClickCount() {

        Url url = new Url(
                "https://www.google.com",
                "stats123",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7));

        url.setClickCount(5);

        when(urlRepository.findByShortCode("stats123"))
                .thenReturn(Optional.of(url));

        Url result = urlShortenerService.getUrlStats("stats123");

        assertNotNull(result);
        assertEquals(5, result.getClickCount());
        assertEquals("stats123", result.getShortCode());

        verify(urlRepository, never())
                .save(any(Url.class));
    }
}