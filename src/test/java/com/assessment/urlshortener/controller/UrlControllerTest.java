package com.assessment.urlshortener.controller;

import com.assessment.urlshortener.service.UrlShortenerService;
import com.assessment.urlshortener.model.Url;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.assessment.urlshortener.dto.CreateUrlResponse;
import com.assessment.urlshortener.dto.CreateUrlRequest;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class UrlControllerTest {

    @Mock
    private UrlShortenerService urlShortenerService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        UrlController urlController = new UrlController(urlShortenerService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(urlController)
                .build();
    }

    @Test
    void shouldRejectBlankUrl() throws Exception {

        String requestBody = """
                {
                    "originalUrl": "",
                    "expirationDays": 7
                }
                """;

        mockMvc.perform(
                post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidUrlFormat() throws Exception {

        String requestBody = """
                {
                    "originalUrl": "not-a-url",
                    "expirationDays": 7
                }
                """;

        mockMvc.perform(
                post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateShortUrlSuccessfully() throws Exception {

        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime expiresAt = createdAt.plusDays(7);

        CreateUrlResponse response = new CreateUrlResponse(
                "https://www.google.com",
                "abc1234",
                "http://localhost:8080/abc1234",
                createdAt,
                expiresAt);

        when(urlShortenerService.createShortUrl(any(CreateUrlRequest.class)))
                .thenReturn(response);

        String requestBody = """
                {
                    "originalUrl": "https://www.google.com",
                    "expirationDays": 7
                }
                """;

        mockMvc.perform(
                post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalUrl")
                        .value("https://www.google.com"))
                .andExpect(jsonPath("$.shortCode")
                        .value("abc1234"))
                .andExpect(jsonPath("$.shortUrl")
                        .value("http://localhost:8080/abc1234"));
    }
    @Test
void shouldGetUrlStatsSuccessfully() throws Exception {

    Url url = new Url(
            "https://www.google.com",
            "abc1234",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(7));

    url.setClickCount(5);

    when(urlShortenerService.getUrlStats("abc1234"))
            .thenReturn(url);

    mockMvc.perform(get("/api/urls/abc1234/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.originalUrl")
                    .value("https://www.google.com"))
            .andExpect(jsonPath("$.shortCode")
                    .value("abc1234"))
            .andExpect(jsonPath("$.clickCount")
                    .value(5));
}
}